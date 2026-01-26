package com.example.alakey.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.alakey.data.ItunesSearchResult
import com.example.alakey.data.PodcastEntity
import android.bluetooth.BluetoothHeadset

@Composable
fun MainContent() {
    val vm: AppViewModel = hiltViewModel()
    val state by vm.uiState.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val sleepTimerSeconds by vm.sleepTimerSeconds.collectAsState()
    val context = LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    
    // Smart Sleep Timer
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val motionDetector = remember { 
        MotionDetector { 
           vm.resetSleepTimer() 
           Toast.makeText(context, "Sleep Timer Extended", Toast.LENGTH_SHORT).show()
        } 
    }

    DisposableEffect(sleepTimerSeconds > 0) {
        if (sleepTimerSeconds > 0) {
            sensorManager.registerListener(motionDetector, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager.unregisterListener(motionDetector)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.connect(context)
        vm.checkForAutoDownloads()
        vm.userEvents.collect { event ->
            when (event) {
                is AppViewModel.UserEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is AppViewModel.UserEvent.ShowError -> Toast.makeText(context, "Error: ${event.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_HEADSET_PLUG || intent.action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED) {
                     vm.resumePlayback()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        val flags = if (Build.VERSION.SDK_INT >= 33) ContextCompat.RECEIVER_NOT_EXPORTED else 0
        ContextCompat.registerReceiver(context, receiver, filter, flags)
        onDispose { context.unregisterReceiver(receiver) }
    }

    var showPlayer by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedView by remember { mutableStateOf(0) } // 0 = Library, 1 = Marketplace

    BackHandler(showPlayer) { showPlayer = false }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        FluxBackground()
        
        Column(Modifier.fillMaxSize()) {
           Row(
               Modifier
                   .fillMaxWidth()
                   .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp)
                   .padding(horizontal = 24.dp), 
               horizontalArrangement = Arrangement.SpaceBetween, 
               verticalAlignment = Alignment.CenterVertically
           ) { 
               Row(
                   Modifier
                       .clip(RoundedCornerShape(12.dp))
                       .background(Color.White.copy(0.1f))
                       .padding(4.dp)
               ) {
                   Box(
                       Modifier
                           .clip(RoundedCornerShape(8.dp))
                           .background(if (selectedView == 0) Color.Cyan.copy(0.3f) else Color.Transparent)
                           .clickable { selectedView = 0 }
                           .padding(horizontal = 16.dp, vertical = 8.dp)
                   ) {
                       Text("Library", color = Color.White, fontWeight = FontWeight.Bold)
                   }
                   Box(
                       Modifier
                           .clip(RoundedCornerShape(8.dp))
                           .background(if (selectedView == 1) Color.Cyan.copy(0.3f) else Color.Transparent)
                           .clickable { selectedView = 1 }
                           .padding(horizontal = 16.dp, vertical = 8.dp)
                   ) {
                       Text("Marketplace", color = Color.White, fontWeight = FontWeight.Bold)
                   }
               }
               
               Row {
                   IconButton(onClick = { showAddDialog = true }, modifier = Modifier.background(Color.White.copy(0.1f), androidx.compose.foundation.shape.CircleShape)) { 
                       Icon(Icons.Rounded.Add, null, tint = Color.White) 
                   }
               }
           }
           
           if (selectedView == 0) {
                  val filters = listOf("All", "Continue", "New", "Short")
                  var activeFilter by remember { mutableStateOf("All") }
                  
                  Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      filters.forEach { f ->
                          val isActive = activeFilter == f
                          Box(Modifier.clip(RoundedCornerShape(50)).background(if(isActive) Color(0xFF00F0FF).copy(0.3f) else Color.White.copy(0.1f)).clickable { activeFilter = f }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                              Text(f, color = if(isActive) Color(0xFF00F0FF) else Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                          }
                      }
                  }

                  val filteredPodcasts = remember(state.podcasts, activeFilter) {
                      when(activeFilter) {
                          "Continue" -> state.podcasts.filter { p -> 
                              val duration = if (p.duration > 0) p.duration.toDouble() else Double.MAX_VALUE
                              p.progress > 0 && p.progress.toDouble() < (duration * 0.95)
                          }.sortedByDescending { it.lastPlayed }
                          "New" -> state.podcasts.filter { p ->
                                 val duration = if (p.duration > 0) p.duration.toDouble() else Double.MAX_VALUE
                                 p.progress.toDouble() < (duration * 0.95)
                          }.sortedByDescending { it.pubDate } 
                          "Short" -> state.podcasts.filter { 
                              val duration = if (it.duration > 0) it.duration.toDouble() else Double.MAX_VALUE
                              it.progress.toDouble() < (duration * 0.95) && it.duration in 1..1200 
                          }
                          else -> state.podcasts.filter { p ->
                              val duration = if (p.duration > 0) p.duration.toDouble() else Double.MAX_VALUE
                              p.progress.toDouble() < (duration * 0.95)
                          }
                      }
                  }

                  LazyColumn(contentPadding = PaddingValues(top = 0.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)) {
                      if (filteredPodcasts.isEmpty()) {
                          item { 
                              Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                                  Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                                      Icon(Icons.Rounded.FilterListOff, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp))
                                      Spacer(Modifier.height(16.dp))
                                      NebulaText("No episodes found", MaterialTheme.typography.bodyLarge, glowColor = Color.Transparent) 
                                  } 
                              } 
                          }
                      } else {
                          val grouped = if(activeFilter == "All") filteredPodcasts.groupBy { it.title } else mapOf("Results" to filteredPodcasts)
                          items(items = grouped.keys.toList(), key = { it }) { title ->
                              val eps = grouped[title] ?: emptyList()
                              if (eps.isNotEmpty()) {
                                   GlassFolderRow(
                                      title = title,
                                      imageUrl = eps.first().imageUrl,
                                      episodes = eps,
                                      onPlay = { vm.play(it); showPlayer = true },
                                      onDownload = { vm.downloadEpisode(it.id) },
                                      onUnsubscribe = { vm.unsubscribe(title) },
                                      onToggleQueue = { 
                                          if (it.isInQueue) vm.removeFromQueue(it) else vm.addToQueue(it)
                                      }
                                  )
                                  Spacer(Modifier.height(8.dp))
                              }
                          }
                      }
                  }
           } else {
               GlassMarketplace(onSubscribe = { query -> 
                   vm.marketplaceSubscribe(query)
               })
           }
        }
        
        if (state.current != null) {
            Box(Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) { 
                GlassMiniPlayer(state.current!!, { vm.togglePlay() }, { showPlayer = true }) 
            }
        }
    }

    if (showPlayer && state.current != null) {
        Surface(Modifier.fillMaxSize(), color = Color.Black.copy(0.95f)) {
            GlassPlayerScreen(
                podcast = state.current!!,
                isPlaying = state.isPlaying,
                currentTime = state.currentTime,
                duration = state.duration,
                sleepTimerSeconds = sleepTimerSeconds,
                speed = state.speed,
                onClose = { showPlayer = false },
                onPlayPause = { vm.togglePlay() },
                onSeek = { vm.seek(it) },
                onSkip = { vm.skip(it) },
                onSetSleepTimer = { vm.startSleepTimer() },
                onSetSpeed = { vm.setPlaybackSpeed(it) },
            )
        }
    }

    if (showAddDialog) {
        AddPodcastDialog(
            onDismiss = { showAddDialog = false },
            onImport = { url ->
                vm.importFeed(url)
                showAddDialog = false
            },
            onSearch = { vm.searchPodcasts(it) },
            searchResults = searchResults
        )
    }
}

@Composable
fun AddPodcastDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<ItunesSearchResult>
) {
    var text by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        PrismaticGlass(Modifier.fillMaxWidth().height(400.dp), RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp)) {
                TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color.White) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Search") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("URL") })
                }
                Spacer(Modifier.height(16.dp))
                if (selectedTab == 0) {
                    Column {
                        OutlinedTextField(value = text, onValueChange = { text = it; onSearch(it) }, label = { Text("Search for a podcast") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(16.dp))
                        LazyColumn {
                            items(searchResults) { result ->
                                Row(Modifier.fillMaxWidth().clickable { onImport(result.feedUrl) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        result.artworkUrl100,
                                        null,
                                        Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(result.collectionName, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.Center) {
                        NebulaText("Add Feed", MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(0.3f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { 
                            BasicTextField(value = text, onValueChange = { text = it }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White), singleLine = true, decorationBox = { if (text.isEmpty()) Text("https://...", color = Color.Gray); it() }, modifier = Modifier.fillMaxWidth()) 
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { 
                            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(0.6f)) }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { onImport(text) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)) { Text("Import") } 
                        }
                    }
                }
            }
        }
    }
}
