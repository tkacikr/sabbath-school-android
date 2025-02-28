package app.ss.media.playback.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ss.design.compose.extensions.isLargeScreen
import app.ss.design.compose.extensions.isS
import app.ss.design.compose.extensions.modifier.thenIf
import app.ss.design.compose.theme.Dimens
import app.ss.design.compose.theme.Spacing12
import app.ss.design.compose.theme.Spacing8
import app.ss.design.compose.theme.SsColor
import app.ss.design.compose.theme.lighter
import app.ss.design.compose.theme.onSurfaceSecondary
import app.ss.design.compose.widget.icon.IconBox
import app.ss.design.compose.widget.icon.IconButton
import app.ss.design.compose.widget.icon.IconSlot
import app.ss.design.compose.widget.icon.Icons
import app.ss.media.R
import app.ss.media.playback.PLAYBACK_PROGRESS_INTERVAL
import app.ss.media.playback.PlaybackConnection
import app.ss.media.playback.extensions.isActive
import app.ss.media.playback.extensions.playPause
import app.ss.media.playback.ui.common.Dismissible
import app.ss.media.playback.ui.common.LocalPlaybackConnection
import app.ss.media.playback.ui.spec.NowPlayingSpec
import app.ss.media.playback.ui.spec.PlaybackStateSpec
import app.ss.media.playback.ui.spec.toSpec
import androidx.compose.material.icons.Icons as MaterialIcons
import app.ss.translations.R.string as RString

private object PlaybackMiniControlsDefaults {
    val height = 60.dp
    val maxWidth = 600.dp
    val playPauseSize = 30.dp
    val replaySize = 30.dp
    val cancelSize = 20.dp
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun PlaybackMiniControls(
    modifier: Modifier = Modifier,
    playbackConnection: PlaybackConnection,
    onExpand: () -> Unit
) {
    val playbackState by playbackConnection.playbackState.collectAsStateWithLifecycle()
    val nowPlaying by playbackConnection.nowPlaying.collectAsStateWithLifecycle()

    val visible = (playbackState to nowPlaying).isActive
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it / 2 }),
        exit = slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        PlaybackMiniControls(
            spec = playbackState.toSpec(),
            nowPlayingSpec = nowPlaying.toSpec(),
            playbackConnection = playbackConnection,
            onExpand = onExpand
        )
    }
}

@Composable
fun PlaybackMiniControls(
    spec: PlaybackStateSpec,
    nowPlayingSpec: NowPlayingSpec,
    modifier: Modifier = Modifier,
    height: Dp = PlaybackMiniControlsDefaults.height,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current,
    onExpand: () -> Unit
) {
    val cancel: () -> Unit = { playbackConnection.transportControls?.stop() }

    val backgroundColor = playbackMiniBackgroundColor()
    val contentColor = playbackContentColor()

    Dismissible(onDismiss = cancel) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = Color.Transparent,
                shape = MaterialTheme.shapes.medium,
                modifier = modifier
                    .padding(horizontal = Dimens.grid_5)
                    .padding(bottom = Dimens.grid_4)
                    .thenIf(isLargeScreen()) {
                        requiredSizeIn(
                            maxWidth = PlaybackMiniControlsDefaults.maxWidth
                        )
                    }
                    .align(Alignment.Center)
                    .clickable { onExpand() }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(playbackButtonSpacing()),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(height)
                            .fillMaxWidth()
                            .background(backgroundColor)
                    ) {
                        NowPlayingColumn(
                            spec = nowPlayingSpec,
                            onCancel = cancel
                        )
                        PlaybackReplay(
                            contentColor = contentColor,
                            onRewind = {
                                playbackConnection.transportControls?.rewind()
                            }
                        )

                        PlaybackPlayPause(
                            spec = spec,
                            contentColor = contentColor,
                            onPlayPause = {
                                playbackConnection.mediaController?.playPause()
                            }
                        )

                        Spacer(modifier = Modifier.width(Dimens.grid_2))
                    }
                    PlaybackProgress(
                        spec = spec,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        playbackConnection = playbackConnection
                    )
                }
            }
        }
    }
}

@Composable
private fun playbackMiniBackgroundColor(
    isDark: Boolean = isSystemInDarkTheme()
): Color =
    if (isDark) {
        Color.Black.lighter()
    } else {
        SsColor.BaseGrey1
    }

@Composable
internal fun playbackContentColor(
    isDark: Boolean = isSystemInDarkTheme()
): Color = when {
    isS() -> MaterialTheme.colorScheme.onSurface
    isDark -> Color.White
    else -> Color.Black
}

@Composable
private fun playbackButtonSpacing(
    isLargeScreen: Boolean = isLargeScreen()
): Dp {
    return if (isLargeScreen) {
        Spacing12
    } else {
        Spacing8
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun PlaybackProgress(
    spec: PlaybackStateSpec,
    color: Color,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current
) {
    val progressState by playbackConnection.playbackProgress.collectAsStateWithLifecycle()
    val sizeModifier = Modifier
        .height(2.dp)
        .fillMaxWidth()

    when {
        spec.isBuffering -> {
            LinearProgressIndicator(
                color = color,
                modifier = sizeModifier
            )
        }
        else -> {
            LinearProgressIndicator(
                progress = animateFloatAsState(progressState.progress, tween(PLAYBACK_PROGRESS_INTERVAL.toInt(), easing = LinearEasing)).value,
                color = color,
                backgroundColor = color.copy(ProgressIndicatorDefaults.IndicatorBackgroundOpacity),
                modifier = sizeModifier
            )
        }
    }
}

@Composable
private fun RowScope.NowPlayingColumn(
    spec: NowPlayingSpec,
    onCancel: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.grid_1),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
    ) {
        IconButton(onClick = onCancel) {
            IconBox(
                icon = Icons.Cancel,
                modifier = Modifier.size(PlaybackMiniControlsDefaults.cancelSize),
                contentColor = SsColor.BaseGrey2
            )
        }

        NowPlayingColumn(
            spec = spec,
            modifier = Modifier
                .weight(1f)
        )
    }
}

@Composable
private fun NowPlayingColumn(
    spec: NowPlayingSpec,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            spec.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                spec.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceSecondary()
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun PlaybackReplay(
    size: Dp = PlaybackMiniControlsDefaults.replaySize,
    contentColor: Color,
    onRewind: () -> Unit
) {
    IconButton(onClick = onRewind) {
        IconBox(
            icon = IconSlot.fromResource(
                R.drawable.ic_audio_icon_backward,
                contentDescription = stringResource(id = RString.ss_action_rewind)
            ),
            modifier = Modifier.size(size),
            contentColor = contentColor
        )
    }
}

@Composable
private fun PlaybackPlayPause(
    spec: PlaybackStateSpec,
    size: Dp = PlaybackMiniControlsDefaults.playPauseSize,
    contentColor: Color,
    onPlayPause: () -> Unit
) {
    IconButton(onClick = onPlayPause) {
        val painter = when {
            spec.isPlaying -> painterResource(id = R.drawable.ic_audio_icon_pause)
            spec.isPlayEnabled -> painterResource(id = R.drawable.ic_audio_icon_play)
            spec.isError -> rememberVectorPainter(MaterialIcons.Rounded.ErrorOutline)
            else -> rememberVectorPainter(MaterialIcons.Rounded.HourglassBottom)
        }
        IconBox(
            icon = IconSlot.fromPainter(
                painter = painter,
                contentDescription = stringResource(id = RString.ss_action_play_pause)
            ),
            modifier = Modifier.size(size),
            contentColor = contentColor
        )
    }
}
