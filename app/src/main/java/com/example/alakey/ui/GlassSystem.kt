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
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import coil.compose.AsyncImage
import com.example.alakey.data.PodcastEntity
import com.example.alakey.ui.theme.LIQUID_PLASMA_SRC

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset


@Composable
fun FluxBackground(modifier: Modifier = Modifier, amplitude: Float = 1f, color: Color = Color.Cyan) {
    if (Build.VERSION.SDK_INT >= 33) {
        val infiniteTransition = rememberInfiniteTransition(label = "time")
        val time by infiniteTransition.animateFloat(
            initialValue = 0f, 
            targetValue = 30f, 
            animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing)), 
            label = "t"
        )
        val shader = remember { RuntimeShader(LIQUID_PLASMA_SRC) }
        val compositeTime = time * (0.5f + amplitude * 1.5f)
        
        Canvas(modifier.fillMaxSize()) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", compositeTime)
            drawRect(brush = ShaderBrush(shader))
            drawRect(brush = Brush.radialGradient(listOf(color.copy(0.1f), Color.Transparent), radius = size.maxDimension), blendMode = BlendMode.Screen)
        }
    } else { Box(modifier.fillMaxSize().background(Color(0xFF020024))) }
}

fun Modifier.pressScale(targetScale: Float = 0.95f) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "press_scale"
    )
    this.scale(scale).clickable(interactionSource = interactionSource, indication = null) { }
}

fun Modifier.glassShimmer() = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val xOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "shimmer_x"
    )
    this.drawWithCache {
        val shimmerBrush = Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.White.copy(0.08f), Color.Transparent),
            start = Offset(size.width * xOffset, 0f),
            end = Offset(size.width * xOffset + 100f, size.height)
        )
        onDrawWithContent { drawContent(); drawRect(shimmerBrush) }
    }
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
fun NebulaText(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier, glowColor: Color = Color.Cyan, speed: Float = 1.0f) {
    val dynamicWeight = remember(speed) {
        when {
            speed < 0.9f -> FontWeight.Light
            speed > 1.4f -> FontWeight.Black
            speed > 1.1f -> FontWeight.Bold
            else -> style.fontWeight ?: FontWeight.Normal
        }
    }
    
    val dynamicSpacing = remember(speed) {
        when {
            speed < 0.9f -> 2.sp // Airy
            speed > 1.4f -> (-0.5).sp // Urgent
            else -> style.letterSpacing
        }
    }
    
    Box(modifier) {
        if (Build.VERSION.SDK_INT >= 31) { 
            androidx.compose.material3.Text(
                text = text, 
                style = style.copy(fontWeight = dynamicWeight, letterSpacing = dynamicSpacing), 
                color = glowColor.copy(0.8f), 
                modifier = Modifier.graphicsLayer { renderEffect = RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.DECAL).asComposeRenderEffect() }
            ) 
        }
        androidx.compose.material3.Text(text = text, style = style.copy(fontWeight = dynamicWeight, letterSpacing = dynamicSpacing), color = Color.White.copy(0.98f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassPodcastRow(
    spec: PodcastRowSpec, 
    onClick: () -> Unit, 
    onDownload: () -> Unit, 
    onAddToQueue: () -> Unit,
    onMarkPlayed: () -> Unit = {},
    onArchiveOlder: () -> Unit = {},
    onDeleteDownload: () -> Unit = {},
    onPlayNext: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    PrismaticGlass(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(84.dp).glassShimmer()) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { showMenu = true }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(spec.imageUrl, null, Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    NebulaText(spec.title, MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    Row { NebulaText(spec.subtitle, MaterialTheme.typography.bodySmall, glowColor = Color.Transparent) }
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.Black.copy(0.9f))
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Next", color = Color.White) },
                        onClick = { onPlayNext(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Mark Played", color = Color.White) },
                        onClick = { onMarkPlayed(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive All Older", color = Color.White) },
                        onClick = { onArchiveOlder(); showMenu = false }
                    )
                    if (spec.isDownloaded) {
                        DropdownMenuItem(
                            text = { Text("Delete Download", color = Color.Red) },
                            onClick = { onDeleteDownload(); showMenu = false }
                        )
                    }
                }
            }
            
            Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!spec.isInQueue) {
                IconButton(onClick = onAddToQueue) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = Color.White.copy(0.7f))
                }
            } else {
                IconButton(onClick = onAddToQueue) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAddCheck, null, tint = Color.Cyan.copy(0.7f))
                }
            }
                if (spec.isDownloaded) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color.Green.copy(0.6f), modifier = Modifier.size(16.dp).padding(8.dp))
                } else {
                    IconButton(onClick = onDownload) {
                         Icon(Icons.Rounded.CloudDownload, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassMiniPlayer(spec: PlayerSpec, onPlay: () -> Unit, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp).padding(bottom = 8.dp)) {
        PrismaticGlass(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize().clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(spec.imageUrl, null, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(spec.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(spec.artist, color = Color.LightGray, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                MorphingPlayPauseButton(spec.isPlaying, { onPlay() }, Modifier.size(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassPlayerScreen(
    spec: PlayerSpec,
    onClose: () -> Unit, 
    onPlayPause: () -> Unit, 
    onSeek: (Long) -> Unit, 
    onSkip: (Int) -> Unit,
    onSetSpeed: (Float) -> Unit
) {
    // Policy: Gestures & Animation State
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize().offset { IntOffset(0, offsetY.value.roundToInt()) }.pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = { if (offsetY.value > 300f) onClose() else scope.launch { offsetY.animateTo(0f) } },
            onDrag = { change, dragAmount -> 
                if (offsetY.value > 0 || dragAmount.y > 0) {
                    change.consume()
                    scope.launch { offsetY.snapTo((offsetY.value + dragAmount.y).coerceAtLeast(0f)) }
                }
            }
        )
    }) { 
        GlassPlayerMechanism(spec, offsetY.value, onClose, onPlayPause, onSeek, onSkip, onSetSpeed)
    }
}

@Composable
fun GlassPlayerMechanism(
    spec: PlayerSpec,
    dragOffset: Float,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkip: (Int) -> Unit,
    onSetSpeed: (Float) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Flux Background
        Box(Modifier.fillMaxSize()) {
            FluxBackground(Modifier.fillMaxSize(), spec.amplitude, Color(spec.dominantColor))
        }

        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Drag Handle
            Box(Modifier.width(40.dp).height(4.dp).background(Color.White.copy(0.3f), RoundedCornerShape(2.dp)).padding(top = 8.dp).clickable { onClose() })

            Spacer(Modifier.height(32.dp))

            // Artwork & Shadow
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                 // Volumetric Shadow
                 Box(Modifier.size(280.dp).graphicsLayer { 
                    translationX = -dragOffset * 0.1f
                    renderEffect = if (Build.VERSION.SDK_INT >= 31) RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.DECAL).asComposeRenderEffect() else null
                }.background(Color(spec.dominantColor).copy(0.4f), CircleShape))
                
                // Art
                val animatedAmplitude by animateFloatAsState(targetValue = spec.amplitude, label = "breath")
                val breathScale = 0.95f + (animatedAmplitude * 0.05f)
                
                Box(Modifier.size(280.dp).inertialTilt(15f).scale(breathScale).shadow(24.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp))) {
                    AsyncImage(spec.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    
                    if (spec.sleepTimerSeconds > 0) {
                         Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.TopCenter) {
                             Text(
                                 "${spec.sleepTimerSeconds / 60}:${(spec.sleepTimerSeconds % 60).toString().padStart(2, '0')}", 
                                 color = Color.White, 
                                 fontWeight = FontWeight.Bold,
                                 modifier = Modifier.padding(top = 16.dp)
                             )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Titles
            NebulaText(spec.title, MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), speed = spec.speed)
            Spacer(Modifier.height(8.dp))
            Text(spec.artist, color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodyLarge)
            
            Spacer(Modifier.height(32.dp))
            
            // Slider
            Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
                Slider(
                    value = spec.currentMs.toFloat(), 
                    valueRange = 0f..spec.durationMs.toFloat().coerceAtLeast(1f), 
                    onValueChange = { 
                        onSeek(it.toLong())
                        if (it.toLong() % 1000 < 100) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) 
                    }, 
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF00F0FF), inactiveTrackColor = Color.White.copy(0.15f))
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                Text(formatMs(spec.currentMs), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                Text(formatMs(spec.durationMs), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Controls
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSetSpeed(if(spec.speed >= 2f) 0.5f else spec.speed + 0.5f); haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }) {
                   Text("${spec.speed}x", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSkip(-15) }) { Icon(Icons.Rounded.Replay, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                
                Box(Modifier.size(80.dp).shadow(30.dp, CircleShape, spotColor = if (spec.isPlaying) Color(0xFF00F0FF) else Color.Transparent).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                     MorphingPlayPauseButton(isPlaying = spec.isPlaying, onToggle = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlayPause() }, modifier = Modifier.size(32.dp))
                }
                
                IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSkip(30) }) { Icon(Icons.AutoMirrored.Rounded.Forward, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                
                IconButton(onClick = { /* Sleep Timer or Queue */ }) { Icon(Icons.Rounded.Timer, null, tint = Color.White.copy(0.5f)) }
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

private val path = Path()
fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

fun formatMs(ms: Long): String { val s = ms / 1000; return String.format(java.util.Locale.US, "%d:%02d", s / 60, s % 60) }

@Composable
fun MorphingPlayPauseButton(isPlaying: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val transition = updateTransition(targetState = isPlaying, label = "PlayPause")
    val t by transition.animateFloat(
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
        label = "progress"
    ) { if (it) 1f else 0f }

    Canvas(modifier.clickable { onToggle() }) {
        val w = size.width
        val h = size.height
        val barWidth = w / 3f
        val gap = w / 3f
        
        // Morph logic:
        // Pause (1f): Two bars at x=0 and x=2*barWidth
        // Play (0f): Left bar becomes top half of triangle, Right bar becomes bottom half? 
        // Simpler: Just generic shape lerp if using Path, but manual drawing is cleaner for geometric morphs.
        // Let's do the "Split Triangle" morph.
        // Triangle vertices: (0,0), (0, h), (w, h/2)
        // Split horizontally? No.
        
        // Approach: Draw paths.
        // Left bar: (0,0)-(w/3,0)-(w/3,h)-(0,h) -> Morph to Left part of triangle
        // Right bar: (2w/3,0)-(w,0)-(w,h)-(2w/3,h) -> Morph to Right part of triangle
        
        // Let's stick to a high-quality "Pause" to "Play" utilizing a single path for smooth corners if possible,
        // but for now, drawing two independent shapes that move is easier to read.
        
        val p1 = Path().apply {
            // Left shape
            moveTo(0f, 0f)
            lineTo(barWidth, 0f)
            lineTo(barWidth, h)
            lineTo(0f, h)
            close()
        }
        
        // Simple scale/shape transform... actually, let's use the purely geometric approach:
        // Pause:  || 
        // Play:   |>
        
        // Lerp between:
        // Left Bar Top-Left: (0,0) -> (0,0)
        // Left Bar Top-Right: (w/3, 0) -> (w, h/2)
        // Left Bar Bottom-Right: (w/3, h) -> (w, h/2)
        // Left Bar Bottom-Left: (0, h) -> (0, h)
        // Wait, that forms a triangle. So the "Right Bar" disappears?
        // Better: The two bars merge.
        
        val pauseGap = gap * t // Gap exists at t=1 (Playing? No isPlaying=true means Pause icon shown? No, isPlaying=true means "Pause" action acts -> show Pause icon)
        // isPlaying=true -> Show Pause Icon (Bars)
        // isPlaying=false -> Show Play Icon (Triangle)
        
        val actualT = if (isPlaying) 1f else 0f 
        // Actually, let's use the animated 't'
        
        // Lerping vertices is hard to get perfect without visual artifacts.
        // Let's do a classic trick: The "Play" triangle is actually two halves.
        // Top half: (0,0) -> (w, h/2) -> (0, h/2)
        // Bottom half: (0,h) -> (w, h/2) -> (0, h/2)
        // Pause bars: 
        // Left: (0,0) -> (w/3, 0) -> (w/3, h) -> (0, h) ?? No.
        
        // Fallback to simpler standard: Pure Triangle <-> Pure Bars with crossfade? No, prompt asked for "Morphing".
        // Let's use the Android 'AnimatedVectorDrawable' style logic manually.
        // Draw Left Pause Bar morphing to Top Half of Triangle.
        // Draw Right Pause Bar morphing to Bottom Half of Triangle.
        
        val center = h / 2f
        
        // Left Bar (Morphs to Upper Triangle)
        val l1 = Offset(0f, 0f) 
        val l2 = Offset(lerp(barWidth, w, 1f-t), lerp(0f, center, 1f-t))
        val l3 = Offset(lerp(barWidth, 0f, 1f-t), lerp(h, center, 1f-t))
        val l4 = Offset(0f, h)
        // This is getting complex to math out perfectly for a "premium" feel in one shot.
        // Let's try a proven approach: The "Pause" bars move together and the gap closes, while the right side scales down to a point.
        
        drawPath(
            path = Path().apply {
                // Top-Left
                moveTo(0f, 0f)
                // Top-Right: At Pause(t=1): w/3, 0. At Play(t=0): w, center
                lineTo(lerp(w, w/3f, t), lerp(center, 0f, t))
                // Bottom-Right: At Pause(t=1): w/3, h. At Play(t=0): w, center
                lineTo(lerp(w, w/3f, t), lerp(center, h, t))
                // Bottom-Left
                lineTo(0f, h)
                close()
                
                // Add the second bar for Pause?
                // If we want two bars...
            },
            color = Color.Black
        )
        // For t=1 (Pause), we need the second bar.
        if (t > 0.1f) {
             drawRect(Color.Black, topLeft = Offset(2f * w / 3f, 0f), size = Size(w/3f, h), alpha = t)
        }
        // This math is a bit messy. Let's revert to a cleaner, simpler implementation that is visually robust:
        // Just draw the standard icons but apply a scale/rotate transition.
        // "Morphing Mechanics": path-based SVG animations.
        // Okay, I will use the standard geometric morph:
        // Pause [ |  | ]  <->  Play [ |> ] 
        // Left bar stays, Right bar moves and shrinks?
        
        // Let's use a simple Canvas drawing that looks like a Play button (Tri) when t=0, and Pause (II) when t=1.
        // Vertices:
        // 1. (0,0) -> (0,0)
        // 2. (w, h/2) -> (w/3, 0)  // Triangle Tip -> Top Real Pause
        // 3. (w, h/2) -> (w/3, h)  // Triangle Tip -> Bottom Real Pause
        // 4. (0, h) -> (0, h)
        
        // This forms the Left Bar + the Point.
        // Where does the Right Bar come from?
        // It fades in.
        
        // Revised Premium Approach:
        // Draw two paths.
        // Part A: Left side of Play / Left Bar of Pause
        // Part B: Right side of Play / Right Bar of Pause (This is the hard part)
        
        // Let's stick to a high-quality cross-fade with scale for now to ensure it looks good and works, 
        // as pure geometric morphing strictly via code without SVG paths often looks broken.
        // BUT the prompt explicitly asked for "Morphing Mechanics".
        // I will implement a "Split" morph.
        
        val width = size.width
        val height = size.height
        
        // Shape 1: Left Bar / Top Half
        // Pause: Rect(0, 0, w/3, h)
        // Play: Triangle(0,0, w, h/2, 0, h/2) ... no.
        
        // Let's try the "Pause bars split and merge to point"
        val barW = width * 0.3f
        
        // Left Part
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(lerp(width, barW, t), lerp(height/2f, 0f, t)) // Point/TopRight
        path.lineTo(lerp(width, barW, t), lerp(height/2f, height, t)) // Point/BotRight
        path.lineTo(0f, height)
        path.close()
        
        drawPath(path, Color.Black)
        
        // Right Bar (Only visible in Pause, fades out/slides in)
        if (t > 0f) {
            drawRect(Color.Black, topLeft = Offset(width - barW, 0f), size = Size(barW, height), alpha = t)
        }
    }
}
