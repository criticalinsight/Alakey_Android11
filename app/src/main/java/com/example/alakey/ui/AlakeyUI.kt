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
import androidx.compose.ui.zIndex
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.Brush
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
    val logs by vm.logs.collectAsState()
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
        vm.connect()
        vm.checkForAutoDownloads()
        vm.userEvents.collect { event ->
            when (event) {
                is AppViewModel.UserEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is AppViewModel.UserEvent.ShowError -> Toast.makeText(context, "Error: ${event.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Phase 4: Simplicity - Declarative Back Logic
    BackHandler(enabled = state.navigationStack.size > 1 || state.isPlayerOpen) {
        if (state.isPlayerOpen) {
            vm.dispatch(AppViewModel.Action.SetPlayerOpen(false))
        } else {
            vm.dispatch(AppViewModel.Action.Pop)
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

    var showAddDialog by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) } // Debug Mode

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        FluxBackground(amplitude = state.amplitude, color = Color(state.dominantColor))
        // Debug Trigger (Invisible top left)
        Box(Modifier.size(64.dp).align(Alignment.TopStart).zIndex(10f).clickable { showDebug = !showDebug })
        
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
                           .background(if (state.navigationStack.last() == AppViewModel.Screen.Library) Color.Cyan.copy(0.3f) else Color.Transparent)
                           .pressScale()
                           .clickable { vm.navigate(AppViewModel.Screen.Library) }
                           .padding(horizontal = 16.dp, vertical = 8.dp)
                   ) {
                        Text("Library", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.navigationStack.last() == AppViewModel.Screen.Inbox) Color.Cyan.copy(0.3f) else Color.Transparent)
                            .pressScale()
                            .clickable { vm.navigate(AppViewModel.Screen.Inbox) } 
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Inbox", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Box(
                       Modifier
                           .clip(RoundedCornerShape(8.dp))
                           .background(if (state.navigationStack.last() == AppViewModel.Screen.Marketplace) Color.Cyan.copy(0.3f) else Color.Transparent)
                           .pressScale()
                           .clickable { vm.navigate(AppViewModel.Screen.Marketplace) }
                           .padding(horizontal = 16.dp, vertical = 8.dp)
                   ) {
                       Text("Marketplace", color = Color.White, fontWeight = FontWeight.Bold)
                   }
                }
                
                // Radio FAB
                Box(
                    Modifier
                        .padding(start = 16.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFF0055FF))))
                        .pressScale()
                        .clickable { vm.playRadio() }
                        .padding(12.dp)
                ) {
                    Icon(Icons.Rounded.Radio, null, tint = Color.White)
                }
                
                Row {
                    IconButton(onClick = { vm.setCarMode(true) }, modifier = Modifier.background(Color.White.copy(0.1f), androidx.compose.foundation.shape.CircleShape).pressScale()) {
                        Icon(Icons.Rounded.DirectionsCar, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.background(Color.White.copy(0.1f), androidx.compose.foundation.shape.CircleShape).pressScale()) { 
                        Icon(Icons.Rounded.Add, null, tint = Color.White) 
                    }
                }
           }
           
            val activeScreen = state.navigationStack.last()
            
            AnimatedContent(
                targetState = activeScreen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)).togetherWith(fadeOut(animationSpec = tween(200)))
                },
                label = "screen_transition",
                modifier = Modifier.weight(1f)
            ) { currentScreen ->
                if (currentScreen == AppViewModel.Screen.Library) {
                   val filters = listOf("All", "Continue", "New", "Short")
                   
                   Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                       filters.forEach { f ->
                           val isActive = state.activeFilter == f
                           Box(Modifier.clip(RoundedCornerShape(50)).background(if(isActive) Color(0xFF00F0FF).copy(0.3f) else Color.White.copy(0.1f)).pressScale().clickable { vm.setFilter(f) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                               Text(f, color = if(isActive) Color(0xFF00F0FF) else Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                           }
                       }
                   }

                    val filteredPodcasts = remember(state.podcasts, state.optimisticPodcasts, state.activeFilter) {
                        val allPodcasts = (state.podcasts + state.optimisticPodcasts).distinctBy { it.feedUrl }
                        when(state.activeFilter) {
                            "Continue" -> allPodcasts.filter { p -> 
                                val duration = if (p.duration > 0) p.duration.toDouble() else Double.MAX_VALUE
                                p.progress > 0 && p.progress.toDouble() < (duration * 0.95)
                            }.sortedByDescending { it.lastPlayed }
                            "New" -> allPodcasts.filter { p ->
                                   val duration = if (p.duration > 0) p.duration.toDouble() else Double.MAX_VALUE
                                   p.progress.toDouble() < (duration * 0.95)
                            }.sortedByDescending { it.pubDate } 
                            "Short" -> allPodcasts.filter { 
                                val duration = if (it.duration > 0) it.duration.toDouble() else Double.MAX_VALUE
                                it.progress.toDouble() < (duration * 0.95) && it.duration in 1..1200 
                            }
                            else -> allPodcasts.filter { p ->
                                val duration = if (p.duration > 0) p.duration.toDouble() else Double.MAX_VALUE
                                p.progress.toDouble() < (duration * 0.95)
                            }
                        }
                    }
                   val expandedGroups = remember { mutableStateListOf<String>() }
                   
                   AnimatedContent(
                       targetState = state.activeFilter,
                       transitionSpec = {
                           (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)).togetherWith(fadeOut(animationSpec = tween(200)))
                       },
                       label = "filter_transition",
                       modifier = Modifier.weight(1f)
                   ) { filter ->
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
                               val grouped = if(state.activeFilter == "All") filteredPodcasts.groupBy { it.title } else mapOf("Results" to filteredPodcasts)
                               
                               grouped.forEach { (title, eps) ->
                                   if (eps.isNotEmpty()) {
                                       // Header
                                       item(key = "header_$title") {
                                           GlassFolderHeader(
                                               title = title,
                                               imageUrl = eps.first().imageUrl,
                                               count = eps.size,
                                               isExpanded = expandedGroups.contains(title),
                                               onToggle = { 
                                                   if (expandedGroups.contains(title)) expandedGroups.remove(title) else expandedGroups.add(title) 
                                               },
                                               onUnsubscribe = { vm.unsubscribe(title) }
                                           )
                                       }
                                       
                                       // Episodes (if expanded)
                                       if (expandedGroups.contains(title) || state.activeFilter != "All") {
                                           items(items = eps, key = { it.id }) { ep ->
                                               Box(Modifier.padding(start = 16.dp)) {
                                                   GlassPodcastRow(
                                                       spec = PodcastRowSpec(
                                                           id = ep.id,
                                                           title = ep.episodeTitle,
                                                           subtitle = ep.title,
                                                           imageUrl = ep.imageUrl,
                                                           isDownloaded = ep.isDownloaded,
                                                           isInQueue = ep.isInQueue,
                                                           progress = if(ep.duration>0) ep.progress.toFloat()/ep.duration else 0f
                                                       ),
                                                       onClick = { vm.play(ep); vm.setPlayerOpen(true) },
                                                       onDownload = { vm.downloadEpisode(ep.id) },
                                                       onAddToQueue = { 
                                                           if (ep.isInQueue) vm.removeFromQueue(ep) else vm.addToQueue(ep)
                                                       },
                                                       onMarkPlayed = { vm.markPlayed(ep) },
                                                       onArchiveOlder = { vm.markOlderPlayed(ep) },
                                                       onDeleteDownload = { vm.deleteDownload(ep) },
                                                       onPlayNext = { vm.playNext(ep) }
                                                   )
                                               }
                                           }
                                       }
                                   }
                               }
                           }
                       }
                   }
                } else if (currentScreen == AppViewModel.Screen.Inbox) {
                  // INBOX VIEW
                  val inbox = state.inbox
                  LazyColumn(contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)) {
                      if (inbox.isEmpty()) {
                           item { 
                               Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                                   Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                                       Icon(Icons.Rounded.Inbox, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp))
                                       Spacer(Modifier.height(16.dp))
                                       NebulaText("All caught up!", MaterialTheme.typography.bodyLarge, glowColor = Color.Transparent) 
                                   } 
                               } 
                           }
                      } else {
                          items(items = inbox, key = { it.id }) { ep ->
                              Box(Modifier.padding(bottom=8.dp)) {
                                  GlassPodcastRow(
                                      spec = PodcastRowSpec(
                                          id = ep.id,
                                          title = ep.episodeTitle,
                                          subtitle = ep.title,
                                          imageUrl = ep.imageUrl,
                                          isDownloaded = ep.isDownloaded,
                                          isInQueue = ep.isInQueue,
                                          progress = if(ep.duration>0) ep.progress.toFloat()/ep.duration else 0f
                                      ),
                                      onClick = { vm.play(ep); vm.setPlayerOpen(true) },
                                      onDownload = { vm.downloadEpisode(ep.id) },
                                      onAddToQueue = { 
                                          vm.addToQueue(ep) 
                                      },
                                      onMarkPlayed = { vm.markPlayed(ep) },
                                      onArchiveOlder = { vm.markOlderPlayed(ep) },
                                      onDeleteDownload = { vm.deleteDownload(ep) },
                                      onPlayNext = { vm.playNext(ep) }
                                  )
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
    }
    
    // --- Flux Player Continuum ---
    val expansion by animateFloatAsState(
        targetValue = if (state.isPlayerOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "expansion"
    )
       if (state.current != null) {
            val playerSpec = PlayerSpec(
                title = state.current!!.episodeTitle,
                artist = state.current!!.title,
                imageUrl = state.current!!.imageUrl,
                isPlaying = state.isPlaying,
                currentMs = state.currentTime,
                durationMs = state.duration,
                speed = state.speed,
                amplitude = state.amplitude,
                sleepTimerSeconds = sleepTimerSeconds,
                dominantColor = state.dominantColor,
                vibrantColor = state.vibrantColor,
                mutedColor = state.mutedColor
            )
            
            FluxPlayerContinuum(
                expansion = expansion,
                spec = playerSpec,
                onTogglePlay = { vm.togglePlay() },
                onClick = { vm.setPlayerOpen(true) },
                onClose = { vm.setPlayerOpen(false) },
                onSeek = { vm.seek(it) },
                onSkip = { vm.skip(it) },
                onSetSpeed = { vm.setPlaybackSpeed(it) }
            )
        }
    }
    
    if (state.isCarMode) {
        CarModeScreen(
            spec = if (state.current != null) PlayerSpec(
                title = state.current!!.episodeTitle,
                artist = state.current!!.title,
                imageUrl = state.current!!.imageUrl,
                isPlaying = state.isPlaying,
                currentMs = state.currentTime,
                durationMs = state.duration,
                speed = state.speed,
                amplitude = state.amplitude,
                sleepTimerSeconds = sleepTimerSeconds,
                dominantColor = state.dominantColor
            ) else null,
            onTogglePlay = { vm.togglePlay() },
            onSkipForward = { vm.skip(30) },
            onSkipBack = { vm.skip(-15) },
            onExit = { vm.setCarMode(false) }
        )
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

    if (showDebug) {
        DebugOverlay(
            historySize = vm.history.size,
            onTimeTravel = { vm.travelTo(it) },
            logs = logs
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
    var selectedTab by remember { mutableIntStateOf(0) }

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
                                Box(Modifier.padding(bottom=8.dp)) {
                                    GlassPodcastRow(
                                        spec = PodcastRowSpec(
                                            id = result.feedUrl, // No dedicated ID for search result item, use feed
                                            title = result.collectionName,
                                            subtitle = "", // artistName not available in ItunesSearchResult mapping
                                            imageUrl = result.artworkUrl100
                                        ),
                                        onClick = { onImport(result.feedUrl) }, // Tap adds feed
                                        onDownload = { /* No-op, or preview? */ },
                                        onAddToQueue = { onImport(result.feedUrl) } // Add = Subscribe
                                    )
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


class MotionDetector(private val onShake: () -> Unit) : android.hardware.SensorEventListener {
    private var lastUpdate: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private val SHAKE_THRESHOLD = 800

    override fun onSensorChanged(event: android.hardware.SensorEvent?) {
        if (event == null) return
        val curTime = System.currentTimeMillis()
        if ((curTime - lastUpdate) > 100) {
            val diffTime = (curTime - lastUpdate)
            lastUpdate = curTime
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
            if (speed > SHAKE_THRESHOLD) {
                onShake()
            }
            lastX = x
            lastY = y
            lastZ = z
        }
    }
    
    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
}
