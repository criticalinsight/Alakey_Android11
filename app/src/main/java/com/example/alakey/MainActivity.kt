package com.example.alakey

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.hilt.work.HiltWorker
import androidx.hilt.work.HiltWorkerFactory
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.*
import coil.compose.AsyncImage
import com.example.alakey.data.*
import com.example.alakey.service.AudioService
import com.example.alakey.ui.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import java.io.File
import kotlinx.coroutines.isActive
import android.hardware.SensorManager
import kotlin.math.sqrt
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.bluetooth.BluetoothHeadset


import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltAndroidApp
class AlakeyApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}

@HiltWorker
class FeedSyncWorker @dagger.assisted.AssistedInject constructor(
    @dagger.assisted.Assisted c: Context, @dagger.assisted.Assisted p: WorkerParameters, private val repo: UniversalRepository
) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result = try {
        repo.syncAll(); Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        fun schedule(c: Context) {
            WorkManager.getInstance(c).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<FeedSyncWorker>(3, TimeUnit.HOURS).build())
        }
    }
}

@HiltWorker
class AudioDownloadWorker @dagger.assisted.AssistedInject constructor(
    @dagger.assisted.Assisted appContext: Context,
    @dagger.assisted.Assisted workerParams: WorkerParameters,
    private val repository: UniversalRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val podcastId = inputData.getString("podcastId") ?: return Result.failure()
        return try {
            repository.downloadAudio(podcastId)
            Log.d("AudioDownloadWorker", "Downloaded audio for $podcastId")
            Result.success()
        } catch (e: Exception) {
            Log.e("AudioDownloadWorker", "Error downloading audio", e)
            Result.failure()
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        FeedSyncWorker.schedule(this)
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<FeedSyncWorker>().build())


        setContent {
            MainContent()
        }
    }
}

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
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        vm.connect(context)
        vm.checkForAutoDownloads() // Initialize auto-downloads
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
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    var showPlayer by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedView by remember { mutableStateOf(0) } // 0 = Library, 1 = Marketplace

    BackHandler(showPlayer) { showPlayer = false }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        FluxBackground()
        
        Column(Modifier.fillMaxSize()) {
           // Header Row with Tabs
           Row(
               Modifier
                   .fillMaxWidth()
                   .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp)
                   .padding(horizontal = 24.dp), 
               horizontalArrangement = Arrangement.SpaceBetween, 
               verticalAlignment = Alignment.CenterVertically
           ) { 
               // Custom Tab Switcher
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
                   IconButton(onClick = { showAddDialog = true }, modifier = Modifier.background(Color.White.copy(0.1f), androidx.compose.foundation.shape.CircleShape)) { Icon(Icons.Rounded.Add, null, tint = Color.White) }
               }
           }
           
           if (selectedView == 0) {
                 // Smart Playlist Chips
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
                         "Played" -> state.podcasts.filter { p -> 
                             val duration = if (p.duration > 0) p.duration.toDouble() else Double.MAX_VALUE
                             p.progress > 0 && p.progress.toDouble() >= (duration * 0.95)
                         }
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
                     if (filteredPodcasts.isEmpty()) item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.FilterListOff, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp)); Spacer(Modifier.height(16.dp)); NebulaText("No episodes found", MaterialTheme.typography.bodyLarge, glowColor = Color.Transparent) } } }
                     else {
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
                                 Text(
                                     text = "Queue: ${state.queue.size}", 
                                     style = MaterialTheme.typography.bodySmall, 
                                     color = Color.White.copy(0.3f),
                                     modifier = Modifier.align(Alignment.CenterHorizontally)
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
        
        if (state.current != null) Box(Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) { GlassMiniPlayer(state.current!!, { vm.togglePlay() }, { showPlayer = true }) }
    }

    if (showPlayer && state.current != null) Surface(Modifier.fillMaxSize(), color = Color.Black.copy(0.95f)) {
        val podcast = state.current!!
        val isPlaying = state.isPlaying
        val currentTime = state.currentTime
        val duration = state.duration
        val sleepTimerSeconds = sleepTimerSeconds

        GlassPlayerScreen(
            podcast = podcast,
            isPlaying = isPlaying,
            currentTime = currentTime,
            duration = duration,
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

    if (showAddDialog) AddPodcastDialog(
        onDismiss = { showAddDialog = false },
        onImport = { url ->
            vm.importFeed(url)
            showAddDialog = false
        },
        onSearch = { vm.searchPodcasts(it) },
        searchResults = searchResults
    )
}



@Composable
fun AddPodcastDialog(onDismiss: () -> Unit, onImport: (String) -> Unit, onSearch: (String) -> Unit, searchResults: List<ItunesSearchResult>) {
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
                        NebulaText("Add Feed", MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(0.3f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { BasicTextField(value = text, onValueChange = { text = it }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White), singleLine = true, decorationBox = { if (text.isEmpty()) Text("https://...", color = Color.Gray); it() }, modifier = Modifier.fillMaxWidth()) }
                        Spacer(Modifier.height(24.dp)); Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(0.6f)) }; Spacer(Modifier.width(8.dp)); Button(onClick = { onImport(text) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)) { Text("Import") } }
                    }
                }
            }
        }
    }
}


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
     var expanded by remember { mutableStateOf(false) }
     var showConfirmUnsubscribe by remember { mutableStateOf(false) }

     Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                  .clickable { expanded = !expanded }
                  .padding(vertical = 4.dp)
          ) {
               AsyncImage(imageUrl, null, Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
               Spacer(Modifier.width(12.dp))
               NebulaText(title, MaterialTheme.typography.titleLarge)
               Spacer(Modifier.weight(1f))
               Icon(
                   if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, 
                   null, 
                   tint = Color.White.copy(0.7f)
               )
               Spacer(Modifier.width(8.dp))
               IconButton(onClick = { showConfirmUnsubscribe = true }) { Icon(Icons.Rounded.Delete, null, tint = Color.Red.copy(0.5f)) }
          }
          if (showConfirmUnsubscribe) {
              AlertDialog(
                  onDismissRequest = { showConfirmUnsubscribe = false },
                  title = { Text("Unsubscribe?", color = Color.White) },
                  text = { Text("Are you sure you want to remove '${title}' and its content?", color = Color.White.copy(0.8f)) },
                  confirmButton = { TextButton(onClick = { onUnsubscribe(); showConfirmUnsubscribe = false }) { Text("Yes, Remove", color = Color.Red) } },
                  dismissButton = { TextButton(onClick = { showConfirmUnsubscribe = false }) { Text("Cancel", color = Color.White.copy(0.7f)) } },
                  containerColor = Color.Black.copy(0.9f)
              )
          }
          if (expanded) {
              episodes.forEach { p ->
                  GlassPodcastRow(p, { onPlay(p) }, { onDownload(p) }, { onToggleQueue(p) })
              }
          }
     }
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repo: UniversalRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    data class UiState(val podcasts: List<PodcastEntity> = emptyList(), val current: PodcastEntity? = null, val isPlaying: Boolean = false, val currentTime: Long = 0, val duration: Long = 1, val speed: Float = 1.0f, val queue: List<PodcastEntity> = emptyList())

    val uiState = MutableStateFlow(UiState())
    private var controller: androidx.media3.session.MediaController? = null
    private var controllerFuture: ListenableFuture<androidx.media3.session.MediaController>? = null
    private var playerListener: Player.Listener? = null
    private var progressPollingJob: Job? = null
    private val _searchResults = MutableStateFlow<List<ItunesSearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()
    private val _sleepTimerSeconds = MutableStateFlow(0)
    val sleepTimerSeconds = _sleepTimerSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null
    private var initialSleepDuration: Int = 0 


    sealed interface UserEvent {
        data class ShowMessage(val message: String) : UserEvent
        data class ShowError(val message: String) : UserEvent
    }
    private val _userEvents = kotlinx.coroutines.flow.MutableSharedFlow<UserEvent>()
    val userEvents = _userEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repo.library.collect { podcasts ->
                val currentPodcast = uiState.value.current
                val updatedPodcast = currentPodcast?.let { cp -> podcasts.find { it.id == cp.id } }
                uiState.update { it.copy(podcasts = podcasts, current = updatedPodcast) }
            }
        }

        viewModelScope.launch {
            repo.queue.collect { queue ->
                uiState.update { it.copy(queue = queue) }
            }
        }




    }





    private fun emitEvent(event: UserEvent) {
        viewModelScope.launch { _userEvents.emit(event) }
    }

    fun connect(ctx: Context) {
        val token = SessionToken(ctx, ComponentName(ctx, AudioService::class.java))
        controllerFuture = androidx.media3.session.MediaController.Builder(ctx, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()

            playerListener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    uiState.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) startProgressPolling()
                    else stopProgressPolling()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        val current = uiState.value.current
                        if (current != null) {
                            deleteAudioFile(current)
                            playNextNewerEpisode(current)
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val podcast = uiState.value.podcasts.find { it.id == mediaItem?.mediaId }
                    uiState.update {
                        it.copy(
                            current = podcast,
                            duration = controller?.duration?.coerceAtLeast(1) ?: 1
                        )
                    }
                }
            }.also { controller?.addListener(it) }

            controller?.let { c ->
                val podcast = uiState.value.podcasts.find { p -> p.id == c.currentMediaItem?.mediaId }
                uiState.update {
                    it.copy(
                        current = podcast,
                        isPlaying = c.isPlaying,
                        duration = c.duration.coerceAtLeast(1)
                    )
                }
                if (c.isPlaying) startProgressPolling()
                if (c.currentMediaItem == null && uiState.value.podcasts.isNotEmpty()) {
                     resumeLastPlayed()
                }
            }
        }, MoreExecutors.directExecutor())
    }

    fun startSleepTimer(minutes: Int = 45) {
        sleepTimerJob?.cancel()
        initialSleepDuration = minutes * 60
        _sleepTimerSeconds.value = initialSleepDuration
         controller?.play()
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerSeconds.value > 0) {
                delay(1000)
                _sleepTimerSeconds.value--
                // Fading handled by checkFadeOut in polling loop
            }
            controller?.pause()
            controller?.volume = 1.0f
        }
    }

    fun resetSleepTimer() {
        if (_sleepTimerSeconds.value > 0) {
             _sleepTimerSeconds.value = initialSleepDuration
             controller?.volume = 1.0f
        }
    }

   fun downloadEpisode(podcastId: String)  {
        runCatching {  val downloadRequest = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setInputData(
                Data.Builder()
                    .putString("podcastId", podcastId)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(downloadRequest) }.onFailure { e -> Log.e("AppViewModel", "Download failure for $podcastId", e) }

    }

    fun checkForAutoDownloads() {
        viewModelScope.launch {
            val podcasts = repo.library.first()
            if (podcasts.isNotEmpty()) {
                val latestEpisode = podcasts.first()
                val downloadRequest = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
                    .setInputData(
                        Data.Builder()
                            .putString("podcastId", latestEpisode.id)
                            .build()
                    )
                    .build()
                WorkManager.getInstance(context).enqueue(downloadRequest)

                Log.d("AppViewModel", "Auto-downloading episode: ${latestEpisode.episodeTitle}")
            }
        }
    }


    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressPolling()
        sleepTimerJob?.cancel()
        playerListener?.let { controller?.removeListener(it) }
        controllerFuture?.let { androidx.media3.session.MediaController.releaseFuture(it) }
    }

    fun importFeed(url: String) {
        viewModelScope.launch {
            repo.subscribe(url)
                .onSuccess { emitEvent(UserEvent.ShowMessage("Feed added successfully")) }
                .onFailure { emitEvent(UserEvent.ShowError("Failed to add feed: ${it.message}")) }
        }
    }

    fun searchPodcasts(query: String) {
        viewModelScope.launch {
            repo.searchPodcasts(query)
                .onSuccess { _searchResults.value = it }
                .onFailure { emitEvent(UserEvent.ShowError("Search failed: ${it.message}")) }
        }
    }

    fun play(p: PodcastEntity) {
        viewModelScope.launch {
                val podcastToPlay = p
            val mediaItem = MediaItem.Builder()
            .setMediaId(podcastToPlay.id)
            .setUri(podcastToPlay.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(podcastToPlay.episodeTitle)
                    .setArtist(podcastToPlay.title)
                    .setArtworkUri(Uri.parse(podcastToPlay.imageUrl))
                    .build()
            )
            .build()
            controller?.setMediaItem(mediaItem)
            controller?.prepare()
            controller?.play()
        }
    }

    fun togglePlay() {
        if (controller?.isPlaying == true) controller?.pause() else controller?.play()
    }

    fun seek(ms: Long) {
        controller?.seekTo(ms)
    }

    fun skip(sec: Int) {
        controller?.seekTo((controller?.currentPosition ?: 0) + (sec * 1000))
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    fun unsubscribe(title: String) {
        viewModelScope.launch {
            repo.unsubscribe(title)
        }
    }

    fun marketplaceSubscribe(query: String) {
        viewModelScope.launch {
            repo.searchPodcasts(query)
                .onSuccess { results ->
                    if (results.isNotEmpty()) {
                        repo.subscribe(results.first().feedUrl)
                            .onSuccess { emitEvent(UserEvent.ShowMessage("Subscribed to $query!")) }
                            .onFailure { emitEvent(UserEvent.ShowError("Failed to subscribe")) }
                    } else {
                        emitEvent(UserEvent.ShowMessage("No results found for $query"))
                    }
                }
                .onFailure { emitEvent(UserEvent.ShowError("Search failed")) }
        }
    }

    fun addToQueue(podcast: PodcastEntity) {
        viewModelScope.launch {
            repo.addToQueue(podcast.id)
        }
    }

    fun removeFromQueue(podcast: PodcastEntity) {
        viewModelScope.launch {
            repo.removeFromQueue(podcast.id)
        }
    }

    fun sortPodcastsByDateAsc() {
        uiState.update { it.copy(podcasts = it.podcasts.sortedBy { p -> p.pubDate }) }
    }

    fun sortPodcastsByDateDesc() {
        uiState.update { it.copy(podcasts = it.podcasts.sortedByDescending { p -> p.pubDate }) }
    }

    fun resumeLastPlayed() {
        viewModelScope.launch {
            val lastPlayed = repo.getLastPlayedPodcast()
            if (lastPlayed != null) {
                 play(lastPlayed)
                 // Note: play() starts playback. If we just want to prepare, we'd need a separate prepare() function.
                 // User said "resume playing", so play() is correct.
            }
        }
    }
    
    fun resumePlayback() {
        controller?.play()
    }

    private fun deleteAudioFile(podcast: PodcastEntity) {
        viewModelScope.launch {
             if (podcast.audioUrl.startsWith(context.getExternalFilesDir(null)?.absolutePath ?: "file://")) {
                 try {
                     val file = File(podcast.audioUrl)
                     if (file.exists()) {
                         file.delete() 
                         repo.savePodcast(podcast.copy(isDownloaded = false, audioUrl = podcast.feedUrl))
                     }
                 } catch (e: Exception) {
                     emitEvent(UserEvent.ShowError("Failed to delete file"))
                 }
             }
        }
    }

    private fun playNextNewerEpisode(current: PodcastEntity) {
        viewModelScope.launch {
            val allEpisodes = repo.getPodcastsByTitle(current.title).sortedBy { it.pubDate }
            val currentIndex = allEpisodes.indexOfFirst { it.id == current.id }
            if (currentIndex != -1 && currentIndex + 1 < allEpisodes.size) {
                 val nextEpisode = allEpisodes[currentIndex + 1]
                 play(nextEpisode)
                 emitEvent(UserEvent.ShowMessage("Playing next episode: ${nextEpisode.episodeTitle}"))
            } else {
                 emitEvent(UserEvent.ShowMessage("All caught up!"))
            }
        }
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = viewModelScope.launch {
            while (isActive) {
                val current = controller?.currentPosition ?: 0L
                val duration = controller?.duration ?: 1L
                uiState.update { it.copy(currentTime = current, duration = if (duration > 0) duration else 1L) }
                
                uiState.value.current?.let { p ->
                    if (current > 0) {
                         repo.updateProgress(p.id, current)
                         repo.updateLastPlayed(p.id, System.currentTimeMillis())
                    }
                }

                checkFadeOut(current, duration)
                delay(1000)
            }
        }
    }
    
    // Override existing stopProgressPolling to be accessible or just redefine helper?
    // It's defined as private fun stopProgressPolling() { progressPollingJob?.cancel() } usually.
    // I need to find where startProgressPolling is defined to replace it properly.
    // For now I will assume I can just add checkFadeOut and let startProgressPolling call it if I can replace startProgressPolling.
    // I will replace the existing startProgressPolling at the bottom of the file if I can find it.
    
    private fun checkFadeOut(current: Long, duration: Long) {
         val timeLeft = duration - current
         val sleepTimeLeft = _sleepTimerSeconds.value
         
         if (timeLeft < 5000 && duration > 10000) {
             val volume = (timeLeft / 5000f).coerceIn(0f, 1f)
             controller?.volume = volume
         } 
         else if (sleepTimerJob != null && sleepTimeLeft < 5 && sleepTimeLeft > 0) {
              val volume = (sleepTimeLeft / 5f).coerceIn(0f, 1f)
              controller?.volume = volume
         } else {
             if (controller?.volume != 1f) controller?.volume = 1f
         }
    }
}


class MotionDetector(private val onMotionDetected: () -> Unit) : SensorEventListener {
    private var acceleration = 0f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var lastAcceleration = SensorManager.GRAVITY_EARTH

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = currentAcceleration - lastAcceleration
        acceleration = acceleration * 0.9f + delta
        
        // Threshold for "shake" or significant movement
        if (acceleration > 2.0f) { 
            onMotionDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

