package com.example.alakey.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.alakey.data.PodcastEntity

@Composable
fun GlassFolderRow(
    title: String,
    imageUrl: String,
    episodes: List<PodcastEntity>,
    onPlay: (PodcastEntity) -> Unit,
    onDownload: (PodcastEntity) -> Unit,
    onUnsubscribe: () -> Unit,
    onToggleQueue: (PodcastEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        PrismaticGlass(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(68.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    NebulaText(title, MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Folder, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        NebulaText("${episodes.size} episodes", style = MaterialTheme.typography.bodySmall, glowColor = Color.Transparent)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onUnsubscribe, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Delete, null, tint = Color.Red.copy(0.7f))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 8.dp)) {
                episodes.forEach { episode ->
                    GlassPodcastRow(
                        podcast = episode, 
                        onClick = { onPlay(episode) }, 
                        onDownload = { onDownload(episode) },
                        onAddToQueue = { onToggleQueue(episode) }
                    )
                }
            }
        }
    }
}
