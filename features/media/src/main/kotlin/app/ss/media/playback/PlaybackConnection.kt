/*
 * Copyright (c) 2021. Adventech <info@adventech.io>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package app.ss.media.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.ss.media.playback.extensions.NONE_PLAYBACK_STATE
import app.ss.media.playback.extensions.NONE_PLAYING
import app.ss.media.playback.extensions.duration
import app.ss.media.playback.extensions.isBuffering
import app.ss.media.playback.extensions.isPlaying
import app.ss.media.playback.model.MEDIA_TYPE_AUDIO
import app.ss.media.playback.model.MediaId
import app.ss.media.playback.model.PlaybackModeState
import app.ss.media.playback.model.PlaybackProgressState
import app.ss.media.playback.model.PlaybackQueue
import app.ss.media.playback.model.PlaybackSpeed
import app.ss.media.playback.players.AudioPlayer
import app.ss.media.playback.players.QUEUE_LIST_KEY
import app.ss.models.media.AudioFile
import com.cryart.sabbathschool.core.extensions.coroutines.flow.flowInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

const val PLAYBACK_PROGRESS_INTERVAL = 1000L

interface PlaybackConnection {
    val isConnected: StateFlow<Boolean>
    val playbackState: StateFlow<PlaybackStateCompat>
    val nowPlaying: StateFlow<MediaMetadataCompat>

    val playbackQueue: StateFlow<PlaybackQueue>

    val playbackProgress: StateFlow<PlaybackProgressState>
    val playbackMode: StateFlow<PlaybackModeState>
    val playbackSpeed: StateFlow<PlaybackSpeed>

    var mediaController: MediaControllerCompat?
    val transportControls: MediaControllerCompat.TransportControls?

    fun playAudio(audio: AudioFile)
    fun playAudios(audios: List<AudioFile>, index: Int = 0)

    fun toggleSpeed()
    fun setQueue(audios: List<AudioFile>, index: Int = 0)
}

internal class PlaybackConnectionImpl(
    context: Context,
    serviceComponent: ComponentName,
    private val audioPlayer: AudioPlayer,
    coroutineScope: CoroutineScope = ProcessLifecycleOwner.get().lifecycleScope
) : PlaybackConnection, CoroutineScope by coroutineScope {

    override val isConnected = MutableStateFlow(false)
    override val playbackState = MutableStateFlow(NONE_PLAYBACK_STATE)
    override val nowPlaying = MutableStateFlow(NONE_PLAYING)

    private val playbackQueueState = MutableStateFlow(PlaybackQueue())
    override val playbackQueue = playbackQueueState

    private var playbackProgressInterval: Job = Job()
    override val playbackProgress = MutableStateFlow(PlaybackProgressState())

    override val playbackMode = MutableStateFlow(PlaybackModeState())
    override val playbackSpeed = MutableStateFlow(PlaybackSpeed.NORMAL)

    override var mediaController: MediaControllerCompat? = null
    override val transportControls get() = mediaController?.transportControls

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
        context,
        serviceComponent,
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() }

    private var currentProgressInterval: Long = PLAYBACK_PROGRESS_INTERVAL

    init {
        startPlaybackProgress()
    }

    override fun playAudio(audio: AudioFile) = playAudios(audios = listOf(audio), index = 0)

    override fun playAudios(audios: List<AudioFile>, index: Int) {
        val audiosIds = audios.map { it.id }.toTypedArray()
        val audio = audios[index]
        transportControls?.playFromMediaId(
            MediaId(MEDIA_TYPE_AUDIO, audio.id).toString(),
            Bundle().apply {
                putStringArray(QUEUE_LIST_KEY, audiosIds)
            }
        )
        transportControls?.sendCustomAction(UPDATE_QUEUE, bundleOf())
    }

    override fun toggleSpeed() {
        val nextSpeed = when (playbackSpeed.value) {
            PlaybackSpeed.SLOW -> PlaybackSpeed.NORMAL
            PlaybackSpeed.NORMAL -> PlaybackSpeed.FAST
            PlaybackSpeed.FAST -> PlaybackSpeed.FASTEST
            PlaybackSpeed.FASTEST -> PlaybackSpeed.SLOW
        }

        if (playbackSpeed.tryEmit(nextSpeed)) {
            audioPlayer.setPlaybackSpeed(nextSpeed.speed)
            resetPlaybackProgressInterval()
        }
    }

    override fun setQueue(audios: List<AudioFile>, index: Int) {
        val audiosIds = audios.map { it.id }
        val initialId = audios.getOrNull(index)?.id ?: ""
        val playbackQueue = PlaybackQueue(
            list = audiosIds,
            audiosList = audios,
            initialMediaId = initialId,
            currentIndex = index
        )
        this.playbackQueueState.value = playbackQueue
    }

    private fun startPlaybackProgress() = launch {
        combine(playbackState, nowPlaying, ::Pair).collect { (state, current) ->
            playbackProgressInterval.cancel()
            val duration = current.duration
            val position = state.position

            if (state == NONE_PLAYBACK_STATE || current == NONE_PLAYING || duration < 1) {
                return@collect
            }

            val initial = PlaybackProgressState(duration, position, buffered = audioPlayer.bufferedPosition())
            playbackProgress.emit(initial)

            if (state.isPlaying && !state.isBuffering) {
                startPlaybackProgressInterval(initial)
            }
        }
    }

    private fun startPlaybackProgressInterval(initial: PlaybackProgressState) {
        playbackProgressInterval = launch {
            flowInterval(currentProgressInterval).collect {
                val current = playbackProgress.value.elapsed
                val elapsed = current + PLAYBACK_PROGRESS_INTERVAL
                playbackProgress.emit(initial.copy(elapsed = elapsed, buffered = audioPlayer.bufferedPosition()))
            }
        }
    }

    private fun resetPlaybackProgressInterval() {
        val speed = playbackSpeed.value.speed
        currentProgressInterval = (PLAYBACK_PROGRESS_INTERVAL.toDouble() / speed).toLong()

        playbackProgressInterval.cancel()
        startPlaybackProgressInterval(playbackProgress.value)
    }

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }

            isConnected.value = true
        }

        override fun onConnectionSuspended() {
            isConnected.value = false
        }

        override fun onConnectionFailed() {
            isConnected.value = false
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.value = state ?: return
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            nowPlaying.value = metadata ?: return
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            playbackMode.value = playbackMode.value.copy(repeatMode = repeatMode)
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            playbackMode.value = playbackMode.value.copy(shuffleMode = shuffleMode)
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}
