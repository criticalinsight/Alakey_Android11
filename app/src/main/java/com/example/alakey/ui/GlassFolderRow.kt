package com.example.alakey.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun GlassFolderHeader(
    title: String,
    imageUrl: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onUnsubscribe: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        PrismaticGlass(Modifier.fillMaxWidth().height(90.dp).glassShimmer()) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                // Main toggle area
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressScale()
                        .clickable { onToggle() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        imageUrl,
                        null,
                        Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        NebulaText(title, MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isExpanded) "Tap to collapse" else "$count episodes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.6f)
                        )
                    }
                    Icon(
                        if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        null,
                        tint = Color.Cyan
                    )
                }
                
                // Secondary action area (Delete)
                IconButton(
                    onClick = onUnsubscribe,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Rounded.Delete, null, tint = Color.Red.copy(0.6f))
                }
            }
        }
    }
}
