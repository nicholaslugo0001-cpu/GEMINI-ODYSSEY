package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.configureAppCheck
import com.example.audio.TechnoClassicalMusicManager
import com.example.data.CloudSyncRepository
import com.example.data.PlayerProgressRepository
import com.example.game.WebSectors
import com.example.multiplayer.LocalLanMultiplayerManager
import com.example.multiplayer.OnlineMultiplayerRepository
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.authStateFlow
import com.example.ui.screens.onGoogleSignInClicked
import com.example.ui.screens.signOutGoogle
import com.example.ui.theme.GeminiOdysseyTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AppNavigation()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
private fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    var isGuestSession by rememberSaveable { mutableStateOf(false) }

    if (user == null && !isGuestSession) {
        GeminiOdysseyTheme {
            AuthScreen(
                onAuthSuccess = { isGuestSession = false },
                onPlayAsGuest = { isGuestSession = true }
            )
        }
    } else {
        val uid = user?.uid ?: "guest_local"
        val displayName = user?.displayName ?: "Guest Knight"
        val isGuest = user == null
        key(uid) {
            AuthenticatedAppContent(
                currentUserId = uid,
                displayName = displayName,
                isGuestMode = isGuest,
                onExitGuestToAuth = { isGuestSession = false }
            )
        }
    }
}

@Composable
private fun AuthenticatedAppContent(
    currentUserId: String,
    displayName: String,
    isGuestMode: Boolean,
    onExitGuestToAuth: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = PlayerProgressRepository(app.applicationContext),
                    cloudRepository = CloudSyncRepository(db),
                    onlineMultiplayer = OnlineMultiplayerRepository(db),
                    lanMultiplayer = LocalLanMultiplayerManager(app.applicationContext),
                    currentUserId = currentUserId,
                    displayName = displayName,
                    isGuestMode = isGuestMode
                )
            }
        }
    )

    val progress by viewModel.playerProgress.collectAsStateWithLifecycle()
    val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val coopSession by viewModel.coopSession.collectAsStateWithLifecycle()
    val discoveredLanHosts by viewModel.discoveredLanHosts.collectAsStateWithLifecycle()
    val geminiChatState by viewModel.geminiChatState.collectAsStateWithLifecycle()
    val geminiChatMessages by viewModel.geminiChatMessages.collectAsStateWithLifecycle()
    val geminiBriefingState by viewModel.geminiBriefingState.collectAsStateWithLifecycle()
    val arenaCoachState by viewModel.arenaCoachState.collectAsStateWithLifecycle()
    val inGameDialogState by viewModel.inGameDialogState.collectAsStateWithLifecycle()
    val inGameDialogVisible by viewModel.inGameDialogVisible.collectAsStateWithLifecycle()
    val inGameDialogHistory by viewModel.inGameDialogHistory.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val musicManager = remember { TechnoClassicalMusicManager() }
    var isBossFightActive by remember { mutableStateOf(false) }

    LaunchedEffect(progress.musicEnabled, progress.selectedMusicTrackId, screen, isBossFightActive) {
        val inArena = screen is AppScreen.Arena
        val activeSectorId = (screen as? AppScreen.Arena)?.sector?.id
        musicManager.updateConfig(
            enabled = progress.musicEnabled,
            trackId = progress.selectedMusicTrackId,
            sectorId = activeSectorId,
            isBossFight = inArena && isBossFightActive
        )
        if (!inArena) {
            isBossFightActive = false
            musicManager.setDucking(false)
        }
    }

    DisposableEffect(lifecycleOwner, musicManager) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> musicManager.setAppInForeground(true)
                Lifecycle.Event.ON_STOP -> musicManager.setAppInForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            musicManager.release()
        }
    }

    val launchGoogleSignIn: () -> Unit = {
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { viewModel.syncCloudNow() },
            onAuthError = { onExitGuestToAuth() },
            scope = scope,
            onAuthCancelled = {}
        )
    }

    GeminiOdysseyTheme(
        themeMode = progress.themeMode,
        highContrast = progress.highContrast
    ) {
        when (val current = screen) {
            is AppScreen.Hub -> {
                HubScreen(
                    progress = progress,
                    leaderboardState = leaderboardState,
                    coopSession = coopSession,
                    discoveredLanHosts = discoveredLanHosts,
                    currentUserId = currentUserId,
                    isGuestMode = isGuestMode,
                    geminiChatState = geminiChatState,
                    geminiChatMessages = geminiChatMessages,
                    geminiBriefingState = geminiBriefingState,
                    onAskGemini = { prompt -> viewModel.askGemini(prompt) },
                    onRefreshGeminiBriefing = { viewModel.refreshGeminiBriefing() },
                    onClearGeminiChat = { viewModel.clearGeminiChat() },
                    onLaunchSector = { sector ->
                        viewModel.resetArenaCoach()
                        viewModel.launchSector(sector)
                    },
                    onPurchaseUpgrade = { type -> viewModel.purchaseUpgrade(type) },
                    onAllocateSkillPoint = { stat -> viewModel.allocateSkillPoint(stat) },
                    onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
                    onSetHighContrast = { enabled -> viewModel.setHighContrast(enabled) },
                    onSetLargeTouchTargets = { enabled -> viewModel.setLargeTouchTargets(enabled) },
                    onSetReducedMotion = { enabled -> viewModel.setReducedMotion(enabled) },
                    onSetSoundEnabled = { enabled -> viewModel.setSoundEnabled(enabled) },
                    onSetHapticsEnabled = { enabled -> viewModel.setHapticsEnabled(enabled) },
                    onSetMusicEnabled = { enabled -> viewModel.setMusicEnabled(enabled) },
                    onSelectMusicTrack = { trackId -> viewModel.setSelectedMusicTrack(trackId) },
                    onToggleHandedness = { left -> viewModel.setLeftHanded(left) },
                    onSyncCloud = { viewModel.syncCloudNow() },
                    onHostOnlineRoom = { sectorId -> viewModel.hostOnlineRoom(sectorId) },
                    onJoinOnlineRoom = { code -> viewModel.joinOnlineRoom(code) },
                    onHostLanRoom = { sectorId -> viewModel.hostLanRoom(sectorId) },
                    onJoinLanHost = { host -> viewModel.joinLanHost(host) },
                    onJoinLanByAddress = { addr -> viewModel.joinLanByAddress(addr) },
                    onStartLanDiscovery = { viewModel.startLanDiscovery() },
                    onStopLanDiscovery = { viewModel.stopLanDiscovery() },
                    onToggleCoopReady = { viewModel.toggleCoopReady() },
                    onSelectCoopSector = { sectorId -> viewModel.selectCoopSector(sectorId) },
                    onStartCoopStage = { viewModel.startCoopStage() },
                    onLeaveCoopRoom = { viewModel.leaveCoopRoom() },
                    onSignInWithGoogle = launchGoogleSignIn,
                    onSignOut = {
                        viewModel.leaveCoopRoom()
                        if (isGuestMode) {
                            onExitGuestToAuth()
                        } else {
                            signOutGoogle(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    }
                )
            }
            is AppScreen.Arena -> {
                val idx = WebSectors.all.indexOfFirst { it.id == current.sector.id }
                val hasNext = !current.isCoop && idx in 0 until WebSectors.all.lastIndex
                ArenaScreen(
                    sector = current.sector,
                    progress = progress,
                    hasNextStage = hasNext,
                    isCoop = current.isCoop,
                    coopSession = coopSession,
                    arenaCoachState = arenaCoachState,
                    inGameDialogState = inGameDialogState,
                    inGameDialogVisible = inGameDialogVisible,
                    inGameDialogHistory = inGameDialogHistory,
                    onTriggerInGameDialog = { trigger, hpPct, wave, nodesIndexed, totalNodes, bossName, question ->
                        viewModel.triggerInGameDialog(
                            trigger = trigger,
                            sector = current.sector,
                            hpPercent = hpPct,
                            wave = wave,
                            nodesIndexed = nodesIndexed,
                            totalNodes = totalNodes,
                            bossName = bossName,
                            isCoop = current.isCoop,
                            customQuestion = question
                        )
                    },
                    onDismissInGameDialog = { viewModel.dismissInGameDialog() },
                    onAskArenaCoach = { hpPct, wave ->
                        viewModel.askArenaCoach(current.sector, hpPct, wave, current.isCoop)
                    },
                    onPublishLocalPlayerState = { x, y, facing, hp, maxHp, guard, slash, combo, walk, hitSeq, hitEnemy, hitDmg ->
                        viewModel.publishLocalPlayerState(
                            x, y, facing, hp, maxHp, guard, slash, combo, walk, hitSeq, hitEnemy, hitDmg
                        )
                    },
                    onPublishHostWorldState = { snapshot, status ->
                        viewModel.publishHostWorldState(snapshot, status)
                    },
                    onToggleHandedness = { left -> viewModel.setLeftHanded(left) },
                    onToggleMusic = { enabled -> viewModel.setMusicEnabled(enabled) },
                    onCycleMusicTrack = { viewModel.cycleMusicTrack() },
                    onMusicDuckChanged = { ducked -> musicManager.setDucking(ducked) },
                    onBossStateChanged = { bossActive -> isBossFightActive = bossActive },
                    onNextStage = {
                        viewModel.resetArenaCoach()
                        viewModel.launchNextStage(current.sector)
                    },
                    onMissionComplete = { coins, stars, victory, v, a, h, xp ->
                        viewModel.recordMissionResult(
                            sectorId = current.sector.id,
                            coinsEarned = coins,
                            starsEarned = stars,
                            victory = victory,
                            virusesKilled = v,
                            adwareKilled = a,
                            hackersKilled = h,
                            xpEarned = xp
                        )
                    },
                    onExitToHub = {
                        viewModel.resetArenaCoach()
                        viewModel.returnToHub()
                    }
                )
            }
        }
    }
}
