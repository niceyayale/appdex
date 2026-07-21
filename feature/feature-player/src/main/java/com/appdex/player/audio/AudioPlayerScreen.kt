package com.appdex.player.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.AppXCard
import com.appdex.ui.theme.*

@Composable
fun AudioPlayerScreen(
    audioPaths: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            audioPaths.forEach { path ->
                addMediaItem(MediaItem.fromUri(path))
            }
            prepare()
            seekTo(initialIndex, 0)
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    var isPlaying by rememberSaveable { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentIndex by rememberSaveable { mutableStateOf(initialIndex) }
    var repeatMode by rememberSaveable { mutableStateOf(Player.REPEAT_MODE_OFF) }
    var shuffle by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            isPlaying = exoPlayer.isPlaying
            currentIndex = exoPlayer.currentMediaItemIndex
            kotlinx.coroutines.delay(200)
        }
    }

    val currentTitle = audioPaths.getOrNull(currentIndex)?.substringAfterLast('/') ?: "Unknown"

    Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlue)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppXBar(
                title = "音频播放",
                back = true,
                onBack = {
                    exoPlayer.release()
                    onDismiss()
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Album art visual
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(SurfaceAlt)
                        .border(1.dp, BorderLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = AmberGold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Track info
                Text(
                    text = currentTitle,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Track ${currentIndex + 1} of ${audioPaths.size}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Seek bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = { fraction ->
                            if (duration > 0) {
                                exoPlayer.seekTo((fraction * duration).toLong())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        text = formatTime(duration),
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Main controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        shuffle = !shuffle
                        exoPlayer.shuffleModeEnabled = shuffle
                    }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffle) AmberGold else TextTertiary
                        )
                    }

                    IconButton(onClick = { exoPlayer.seekToPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = TextPrimary)
                    }

                    IconButton(onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(48.dp),
                            tint = AmberGold
                        )
                    }

                    IconButton(onClick = { exoPlayer.seekToNext() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary)
                    }

                    IconButton(onClick = {
                        repeatMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        exoPlayer.repeatMode = repeatMode
                    }) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) AmberGold else TextTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playlist
                AppXCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderLight,
                    backgroundColor = SurfaceAlt
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp), tint = AmberGold)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Playlist",
                            fontSize = 12.sp,
                            color = AmberGold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    audioPaths.forEachIndexed { index, path ->
                        val name = path.substringAfterLast('/')
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                color = if (index == currentIndex) AmberGold else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (index == currentIndex && isPlaying) {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = AmberGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
