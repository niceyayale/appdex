package com.appdex.player.image

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.appdex.common.FormatUtil
import com.appdex.ui.components.AppXBar
import com.appdex.ui.components.InfoRow
import com.appdex.ui.theme.*

@Composable
fun ImageViewerScreen(
    imagePaths: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { imagePaths.size }
    var showUi by rememberSaveable { mutableStateOf(true) }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    // Rotation state lifted up so buttons can control it
    var rotation by remember { mutableFloatStateOf(0f) }

    // Reset rotation when page changes
    LaunchedEffect(pagerState.currentPage) {
        rotation = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBlue)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomableImage(
                imagePath = imagePaths[page],
                onTap = { showUi = !showUi },
                rotationState = rotation,
                onRotationChange = { rotation = it }
            )
        }

        // Top bar
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSpaceOuter.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(21.dp),
                            tint = StarlightWhite
                        )
                    }
                    Column {
                        Text(
                            text = imagePaths.getOrNull(pagerState.currentPage)
                                ?.substringAfterLast('/')
                                ?: "Image",
                            color = StarlightWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${imagePaths.size}",
                            color = StarlightWhite.copy(alpha = 0.7f)
                        )
                    }
                }
                IconButton(onClick = { showInfo = !showInfo }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = StarlightWhite)
                }
            }
        }

        // Bottom controls
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSpaceOuter.copy(alpha = 0.6f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { rotation -= 90f }) {
                    Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left", tint = StarlightWhite)
                }
                IconButton(onClick = { rotation += 90f }) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right", tint = StarlightWhite)
                }
            }
        }
    }

    // Info dialog
    if (showInfo) {
        val currentPath = imagePaths.getOrNull(pagerState.currentPage) ?: ""
        ImageInfoDialog(
            path = currentPath,
            onDismiss = { showInfo = false }
        )
    }
}

@Composable
private fun ZoomableImage(
    imagePath: String,
    onTap: () -> Unit,
    rotationState: Float,
    onRotationChange: (Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, zoom, gestureRotation ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                        onRotationChange(rotationState + gestureRotation)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size == 1) {
                            val change = event.changes.first()
                            if (change.pressed != change.previousPressed) {
                                onTap()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imagePath)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    rotationZ = rotationState
                }
        )
    }
}

@Composable
private fun ImageInfoDialog(
    path: String,
    onDismiss: () -> Unit
) {
    val file = java.io.File(path)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = SurfaceDeep,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Image Info",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AmberGold
                )
                Spacer(modifier = Modifier.size(16.dp))
                InfoRow("Name", file.name)
                InfoRow("Path", file.parent ?: "")
                InfoRow("Size", FormatUtil.formatFileSize(file.length()))
                InfoRow("Modified", FormatUtil.formatTimestamp(file.lastModified()))
                Spacer(modifier = Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Close", fontSize = 12.sp, color = AmberGold)
                    }
                }
            }
        }
    }
}
