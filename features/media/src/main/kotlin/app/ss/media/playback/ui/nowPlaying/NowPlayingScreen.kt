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

package app.ss.media.playback.ui.nowPlaying

import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ss.design.compose.theme.onSurfaceSecondary
import app.ss.design.compose.widget.DragHandle
import app.ss.design.compose.widget.icon.IconBox
import app.ss.design.compose.widget.icon.IconButton
import app.ss.design.compose.widget.icon.IconSlot
import app.ss.media.R
import app.ss.media.playback.PlaybackConnection
import app.ss.media.playback.extensions.isBuffering
import app.ss.media.playback.model.PlaybackProgressState
import app.ss.media.playback.model.PlaybackQueue
import app.ss.media.playback.model.PlaybackSpeed
import app.ss.media.playback.ui.common.PlaybackSpeedLabel
import app.ss.media.playback.ui.nowPlaying.components.BoxState
import app.ss.media.playback.ui.nowPlaying.components.PlayBackControls
import app.ss.media.playback.ui.nowPlaying.components.PlaybackProgressDuration
import app.ss.media.playback.ui.playbackContentColor
import app.ss.media.playback.ui.spec.toSpec
import app.ss.models.media.AudioFile
import app.ss.translations.R.string as RString

@Immutable
data class NowPlayingScreenSpec(
    val nowPlayingAudio: AudioFile,
    val playbackQueue: PlaybackQueue,
    val playbackState: PlaybackStateCompat,
    val playbackProgressState: PlaybackProgressState,
    val playbackConnection: PlaybackConnection,
    val playbackSpeed: PlaybackSpeed,
    val isDraggable: (Boolean) -> Unit
)

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
internal fun NowPlayingScreen(
    viewModel: NowPlayingViewModel = viewModel(),
    isDraggable: (Boolean) -> Unit = {}
) {
    val playbackConnection = viewModel.playbackConnection
    val playbackState by playbackConnection.playbackState
        .collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlayingAudio
        .collectAsStateWithLifecycle()
    val playbackQueue by playbackConnection.playbackQueue
        .collectAsStateWithLifecycle()
    val playbackSpeed by playbackConnection.playbackSpeed
        .collectAsStateWithLifecycle()
    val playbackProgressState by playbackConnection.playbackProgress
        .collectAsStateWithLifecycle()
    val nowPlayingAudio = if (nowPlaying.id.isEmpty()) {
        playbackQueue.currentAudio ?: nowPlaying
    } else {
        nowPlaying
    }

    NowPlayingScreen(
        spec = NowPlayingScreenSpec(
            nowPlayingAudio,
            playbackQueue,
            playbackState,
            playbackProgressState,
            playbackConnection,
            playbackSpeed,
            isDraggable
        )
    )
}

@Composable
internal fun NowPlayingScreen(
    spec: NowPlayingScreenSpec,
    listState: LazyListState = rememberLazyListState(),
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val (_, _, playbackState, playbackProgressState, playbackConnection, playbackSpeed, isDraggable) = spec

    var boxState by remember { mutableStateOf(BoxState.Expanded) }
    val expanded = boxState == BoxState.Expanded

    val spacing by animateDpAsState(
        if (expanded) 32.dp else 0.dp,
        animationSpec = spring(
            Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
        )
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                isDraggable(false)
                return super.onPreScroll(available, source)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isDraggable(true)
                return super.onPostFling(consumed, available)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
            .nestedScroll(connection = nestedScrollConnection),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DragHandle()

        NowPlayingDetail(
            spec = spec,
            boxState = boxState,
            listState = listState,
            modifier = Modifier.weight(1f)
        )

        PlaybackProgressDuration(
            isBuffering = playbackState.isBuffering,
            progressState = playbackProgressState,
            onSeekTo = { progress ->
                playbackConnection.transportControls?.seekTo(progress)
            }
        )

        Spacer(modifier = Modifier.height(spacing))

        PlayBackControls(
            spec = playbackState.toSpec(),
            contentColor = playbackContentColor(),
            playbackConnection = playbackConnection
        )

        Spacer(modifier = Modifier.height(spacing))

        BottomControls(
            playbackSpeed = playbackSpeed,
            isDarkTheme = isDarkTheme,
            toggleSpeed = { playbackConnection.toggleSpeed() },
            toggleExpand = {
                boxState = when (boxState) {
                    BoxState.Collapsed -> BoxState.Expanded
                    BoxState.Expanded -> BoxState.Collapsed
                }
            }
        )
    }
}

@Composable
private fun BottomControls(
    modifier: Modifier = Modifier,
    playbackSpeed: PlaybackSpeed,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    toggleSpeed: () -> Unit = {},
    toggleExpand: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        PlaybackSpeedLabel(
            playbackSpeed = playbackSpeed,
            toggleSpeed = { toggleSpeed() },
            contentColor = tintColor(isDark = isDarkTheme)
        )

        IconButton(onClick = toggleExpand) {
            IconBox(
                icon = IconSlot.fromResource(
                    R.drawable.ic_audio_icon_playlist,
                    contentDescription = stringResource(id = RString.ss_action_playlist)
                ),
                contentColor = tintColor(
                    isDark = isDarkTheme
                )
            )
        }
    }
}

@Composable
private fun tintColor(
    isDark: Boolean
): Color = if (isDark) onSurfaceSecondary() else MaterialTheme.colorScheme.primary
