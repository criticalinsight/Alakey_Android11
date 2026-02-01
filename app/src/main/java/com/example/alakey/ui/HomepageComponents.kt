package com.example.alakey.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.alakey.data.PodcastEntity

// --- Homepage Overhaul Components ---

@Composable
fun GlassDock(
    currentScreen: AppViewModel.Screen,
    onNavigate: (AppViewModel.Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .padding(bottom = 24.dp)
            .height(72.dp)
            .width(280.dp) // Compact floating width
    ) {
        // Glass Capsule
        Box(
            Modifier
                .fillMaxSize()
                .shadow(24.dp, CircleShape, spotColor = Color(0xFF00F0FF).copy(0.3f))
                .clip(CircleShape)
                .background(Color.Black.copy(0.6f))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.4f), Color.White.copy(0.1f))), CircleShape)
        )
        
        // Content
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                icon = Icons.Rounded.LibraryBooks, 
                label = "Library", 
                isSelected = currentScreen == AppViewModel.Screen.Library,
                onClick = { onNavigate(AppViewModel.Screen.Library) }
            )
            DockItem(
                icon = Icons.Rounded.Inbox, 
                label = "Inbox", 
                isSelected = currentScreen == AppViewModel.Screen.Inbox,
                onClick = { onNavigate(AppViewModel.Screen.Inbox) }
            )
            DockItem(
                icon = Icons.Rounded.Search, 
                label = "Search", 
                isSelected = currentScreen == AppViewModel.Screen.Marketplace,
                onClick = { onNavigate(AppViewModel.Screen.Marketplace) }
            )
        }
    }
}

@Composable
private fun DockItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val color by animateColorAsState(targetValue = if (isSelected) Color(0xFF00F0FF) else Color.White.copy(0.4f), label = "color")
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1f, label = "scale")
    
    Column(
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick() 
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp).scale(scale))
        Spacer(Modifier.height(4.dp))
        if (isSelected) {
            Box(Modifier.size(4.dp).background(color, CircleShape))
        }
    }
}

@Composable
fun SpotlightHero(
    podcast: PodcastEntity?, // Can be null if nothing playing/featured
    onClick: () -> Unit
) {
    if (podcast == null) return

    Box(
        Modifier
            .fillMaxWidth()
            .height(420.dp) // Taller for more immersive feel
            .padding(16.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable { onClick() }
    ) {
        // Background Image (Darkened)
        AsyncImage(
            model = podcast.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Gradient Scrim
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f), Color.Black))))

        // Content
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF00F0FF)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(16.dp))
            Text(podcast.episodeTitle, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 3)
            Spacer(Modifier.height(8.dp))
            Text(podcast.title, color = Color.White.copy(0.7f), style = MaterialTheme.typography.titleMedium)
        }
    }
}
