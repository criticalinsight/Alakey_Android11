package com.example.alakey.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
// Note: ContentCut is usually in generic Icons.Default or Icons.Rounded.ContentCut if extended icons are available.
// Checking availability: ContentCut is part of standard set.
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.alakey.data.PodcastEntity

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset

@org.intellij.lang.annotations.Language("AGSL")
const val LIQUID_PLASMA_SRC = """
    uniform float2 resolution;
    uniform float time;
    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution.xy;
        float t = time * 0.2;
        float2 p = uv * 3.0 - float2(20.0);
        float2 i = p;
        float c = 1.0;
        float inten = 0.035;
        for (int n = 0; n < 3; n++) {
            float t2 = t * (1.0 - (3.0 / float(n+1)));
            i = p + float2(cos(t2 - i.x) + sin(t2 + i.y), sin(t2 - i.y) + cos(t2 + i.x));
            c += 1.0 / length(float2(p.x / (sin(i.x+t)/inten), p.y / (cos(i.y+t)/inten)));
        }
        c /= 3.0;
        c = 1.6 - sqrt(c);
        return half4(c*c*c*c*0.12, c*c*c*0.22, c*c*0.5, 1.0);
    }
"""

@Composable
fun FluxBackground(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= 33) {
        val infiniteTransition = rememberInfiniteTransition(label = "time")
        val time by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 30f, animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing)), label = "t")
        val shader = remember { RuntimeShader(LIQUID_PLASMA_SRC) }
        Canvas(modifier.fillMaxSize()) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", time)
            drawRect(brush = ShaderBrush(shader))
            drawRect(brush = Brush.radialGradient(listOf(Color.White.copy(0.04f), Color.Transparent), radius = size.maxDimension), blendMode = BlendMode.Overlay)
        }
    } else { Box(modifier.fillMaxSize().background(Color(0xFF020024))) }
}

fun Modifier.inertialTilt(maxRotation: Float = 12f) = composed {
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    this.graphicsLayer { rotationX = rotX.value; rotationY = rotY.value; cameraDistance = 16f * density }.pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = { scope.launch { rotX.animateTo(0f, spring(0.4f, 200f)); rotY.animateTo(0f, spring(0.4f, 200f)) } },
            onDrag = { change, dragAmount ->
                change.consume()
                scope.launch {
                    val tx = (rotX.value - dragAmount.y * 0.15f).coerceIn(-maxRotation, maxRotation)
                    val ty = (rotY.value + dragAmount.x * 0.15f).coerceIn(-maxRotation, maxRotation)
                    rotX.snapTo(tx); rotY.snapTo(ty)
                }
            }
        )
    }
}

@Composable
fun PrismaticGlass(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(24.dp), content: @Composable BoxScope.() -> Unit) {
    val density = LocalDensity.current
    Box(modifier.clip(shape).background(Color.White.copy(alpha = 0.05f)).drawWithCache {
        val path = Path().apply { addRoundRect(RoundRect(size.toRect(), CornerRadius(shape.topStart.toPx(size, density)))) }
        val spectrum = Brush.sweepGradient(listOf(Color.Cyan.copy(0.6f), Color(0xFFBD00FF).copy(0.6f), Color.Yellow.copy(0.4f), Color.Cyan.copy(0.6f)))
        onDrawWithContent { drawContent(); drawPath(path, spectrum, style = Stroke(width = 1.2.dp.toPx())) }
    }, content = content)
}

@Composable
fun NebulaText(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier, glowColor: Color = Color.Cyan) {
    Box(modifier) {
        if (Build.VERSION.SDK_INT >= 31) { 
            androidx.compose.material3.Text(text = text, style = style, color = glowColor.copy(0.8f), modifier = Modifier.graphicsLayer { renderEffect = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.DECAL).asComposeRenderEffect() }) 
        }
        androidx.compose.material3.Text(text = text, style = style, color = Color.White.copy(0.98f))
    }
}

@Composable
fun GlassPodcastRow(podcast: PodcastEntity, onClick: () -> Unit, onDownload: () -> Unit, onAddToQueue: () -> Unit) {
    PrismaticGlass(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(84.dp).clickable { onClick() }) {
        Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(podcast.imageUrl, null, Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                NebulaText(podcast.episodeTitle, MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Row { NebulaText(podcast.title, MaterialTheme.typography.bodySmall, glowColor = Color.Transparent) }
            }
            if (!podcast.isInQueue) {
                IconButton(onClick = onAddToQueue) {
                    Icon(Icons.Rounded.PlaylistAdd, null, tint = Color.White.copy(0.7f))
                }
            } else {
                 IconButton(onClick = onAddToQueue) {
                    Icon(Icons.Rounded.PlaylistAddCheck, null, tint = Color.Cyan.copy(0.7f))
                 }
            }
            if (podcast.isDownloaded) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Color.Green.copy(0.6f), modifier = Modifier.size(16.dp))
            } else {
                IconButton(onClick = onDownload) {
                     Icon(Icons.Rounded.CloudDownload, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun GlassMiniPlayer(podcast: PodcastEntity, onPlay: () -> Unit, onClick: () -> Unit) {
    PrismaticGlass(Modifier.fillMaxWidth().height(72.dp).clickable { onClick() }, CircleShape) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(podcast.imageUrl, null, Modifier.size(56.dp).clip(CircleShape)); Spacer(Modifier.width(12.dp))
            NebulaText(podcast.episodeTitle, MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)); Spacer(Modifier.weight(1f))
            IconButton(onClick = onPlay) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassPlayerScreen(
    podcast: PodcastEntity, 
    isPlaying: Boolean, 
    currentTime: Long, 
    duration: Long, 
    sleepTimerSeconds: Int,
    speed: Float, /* New */
    onClose: () -> Unit, 
    onPlayPause: () -> Unit, 
    onSeek: (Long) -> Unit, 
    onSkip: (Int) -> Unit,
    onSetSleepTimer: () -> Unit,
    onSetSpeed: (Float) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    // Karaoke mode now default if transcript exists

    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize().offset { IntOffset(0, offsetY.value.roundToInt()) }.pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = { if (offsetY.value > 300f) onClose() else scope.launch { offsetY.animateTo(0f) } },
            onDrag = { change, dragAmount -> change.consume(); scope.launch { offsetY.snapTo((offsetY.value + dragAmount.y).coerceAtLeast(0f)) } }
        )
    }) {

        FluxBackground()
        Column(Modifier.fillMaxSize().padding(top=40.dp, start=24.dp, end=24.dp, bottom=24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(0.2f)).clickable { onClose() }); Spacer(Modifier.height(32.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                // Album Art Layer
                Box(Modifier.inertialTilt(12f)) {
                    Box(Modifier.fillMaxWidth(0.95f).aspectRatio(1f).shadow(50.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF00F0FF))) { PrismaticGlass(Modifier.fillMaxSize()) { AsyncImage(podcast.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
                }

                if (isPlaying) {
                     Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).height(40.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        repeat(10) { i ->
                            val h by rememberInfiniteTransition().animateFloat(initialValue = 10f, targetValue = 40f, animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 100, easing = FastOutSlowInEasing), RepeatMode.Reverse))
                            Box(Modifier.width(4.dp).height(h.dp).background(Color(0xFF00F0FF).copy(0.8f), CircleShape))
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { NebulaText(podcast.episodeTitle, MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), glowColor = Color(0xFFBD00FF)); Spacer(Modifier.height(8.dp)); NebulaText(podcast.title, MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(0.7f)), glowColor = Color.Transparent) }
            Spacer(Modifier.height(32.dp))
            Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
                Slider(value = currentTime.toFloat(), valueRange = 0f..duration.toFloat().coerceAtLeast(1f), onValueChange = { onSeek(it.toLong()) }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF00F0FF), inactiveTrackColor = Color.White.copy(0.15f)))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                Text(formatMs(currentTime), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                if (sleepTimerSeconds > 0) {
                    Text(formatMs(sleepTimerSeconds * 1000L), style = MaterialTheme.typography.labelSmall, color = Color(0xFF00F0FF))
                }
                Text("-" + formatMs(duration - currentTime), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f)) 
            }
            Spacer(Modifier.height(24.dp))
            PrismaticGlass(Modifier.fillMaxWidth().height(100.dp), RoundedCornerShape(100.dp)) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                     // Speed Button
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable { 
                        val nextSpeed = when (speed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            2.0f -> 0.8f
                            else -> 1.0f
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSetSpeed(nextSpeed)
                    }.padding(8.dp)) {
                        Text("${speed}x", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSkip(-15) }) { Icon(Icons.Rounded.Replay, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    Box(Modifier.size(72.dp).shadow(30.dp, CircleShape, spotColor = if (isPlaying) Color(0xFF00F0FF) else Color.Transparent).background(Color.White, CircleShape).clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlayPause() }, contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(36.dp)) }
                    IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSkip(30) }) { Icon(Icons.Rounded.Forward, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                }
            }
        }
    }
}


fun formatMs(ms: Long): String { val s = ms / 1000; return String.format(java.util.Locale.US, "%d:%02d", s / 60, s % 60) }
