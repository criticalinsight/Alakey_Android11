package com.example.alakey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.alakey.data.PodcastEntity

/**
 * Car System: A safety-first projection of the UI State.
 * Optimized for:
 * 1. Readability at a glance (High Contrast, Large Type).
 * 2. Touch accuracy (Huge Targets).
 * 3. Minimal cognitive load (No browsing, only playback).
 */
@Composable
fun CarModeScreen(
    spec: PlayerSpec?,
    onTogglePlay: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBack: () -> Unit,
    onExit: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    
    // Pure Black background for OLED efficiency and night safety
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // 1. Info Display (High Vis)
        if (spec != null) {
            Text(
                spec.title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 32.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                spec.artist,
                color = Color.LightGray,
                fontSize = 20.sp,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        } else {
            Text("No Media", color = Color.Gray, fontSize = 24.sp)
        }

        // 2. Control Cluster (HUGE TARGETS)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rev 30
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .clickable { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onSkipBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Replay30, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            // Play/Pause (The Hero Button)
            Box(
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4500)) // Safety Orange / Red High Vis
                    .clickable { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (spec?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null,
                    tint = Color.Black,
                    modifier = Modifier.size(64.dp)
                )
            }

            // Fwd 30
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .clickable { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onSkipForward() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Forward30, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        
        Spacer(Modifier.height(48.dp))
        
        // 3. Exit Button
        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.DarkGray)
                .clickable { onExit() },
            contentAlignment = Alignment.Center
        ) {
            Text("EXIT CAR MODE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
