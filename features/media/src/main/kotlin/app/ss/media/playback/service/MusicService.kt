package app.ss.media.playback.service

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import app.ss.lessons.data.repository.media.MediaRepository
import app.ss.media.playback.BACKWARD
import app.ss.media.playback.FORWARD
import app.ss.media.playback.MediaNotifications
import app.ss.media.playback.NOTIFICATION_ID
import app.ss.media.playback.PLAY_PAUSE
import app.ss.media.playback.STOP_PLAYBACK
import app.ss.media.playback.extensions.isIdle
import app.ss.media.playback.extensions.playPause
import app.ss.media.playback.model.MediaId
import app.ss.media.playback.model.MediaId.Companion.CALLER_OTHER
import app.ss.media.playback.model.MediaId.Companion.CALLER_SELF
import app.ss.media.playback.players.SSAudioPlayer
import app.ss.media.playback.receivers.BecomingNoisyReceiver
import com.cryart.sabbathschool.core.extensions.coroutines.DispatcherProvider
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var musicPlayer: SSAudioPlayer

    @Inject
    lateinit var mediaNotifications: MediaNotifications

    @Inject
    lateinit var repository: MediaRepository

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private var becomingNoisyReceiver: BecomingNoisyReceiver? = null

    override fun onCreate() {
        super.onCreate()

        sessionToken = musicPlayer.getSession().sessionToken
        sessionToken?.let { token ->
            becomingNoisyReceiver = BecomingNoisyReceiver(this, token)
        }

        musicPlayer.onPlayingState { isPlaying, byUi ->
            val isIdle = musicPlayer.getSession().controller.playbackState.isIdle
            if (!isPlaying && isIdle) {
                pauseForeground(byUi)
                mediaNotifications.clearNotifications()
            } else {
                startForeground()
            }

            mediaNotifications.updateNotification(getSession())
        }

        musicPlayer.onMetaDataChanged {
            mediaNotifications.updateNotification(getSession())
        }
    }

    private fun startForeground() {
        if (IS_FOREGROUND) {
            Timber.i("Tried to start foreground, but was already in foreground")
            return
        }
        Timber.i("Starting foreground service")
        startForeground(NOTIFICATION_ID, mediaNotifications.buildNotification(musicPlayer.getSession()))
        becomingNoisyReceiver?.register()
        IS_FOREGROUND = true
    }

    private fun pauseForeground(removeNotification: Boolean) {
        if (!IS_FOREGROUND) {
            Timber.w("Tried to stop foreground, but was already NOT in foreground")
            return
        }
        Timber.d("Stopping foreground service")
        becomingNoisyReceiver?.unregister()
        stopForeground(removeNotification)
        IS_FOREGROUND = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_STICKY
        }

        val mediaSession = musicPlayer.getSession()
        val controller = mediaSession.controller

        when (intent.action) {
            PLAY_PAUSE -> controller.playPause()
            FORWARD -> controller.transportControls.fastForward()
            BACKWARD -> controller.transportControls.rewind()
            STOP_PLAYBACK -> controller.transportControls.stop()
        }

        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_STICKY
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        val caller = if (clientPackageName == applicationContext.packageName) CALLER_SELF else CALLER_OTHER
        return BrowserRoot(MediaId(caller = caller).toString(), null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.detach()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        musicPlayer.pause()
        musicPlayer.stop(false)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        musicPlayer.release()
    }

    companion object {
        var IS_FOREGROUND = false
    }
}
