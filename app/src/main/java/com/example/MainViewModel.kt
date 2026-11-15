package com.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiBriefingUiState
import com.example.ai.GeminiChatMessage
import com.example.ai.GeminiChatUiState
import com.example.ai.GeminiTacticalRepository
import com.example.ai.InGameDialogTrigger
import com.example.audio.ClassicalTechnoTracks
import com.example.data.CloudSyncRepository
import com.example.data.LeaderboardEntry
import com.example.data.PlayerProgress
import com.example.data.PlayerProgressRepository
import com.example.data.StatType
import com.example.data.ThemeMode
import com.example.data.UpgradeType
import com.example.game.SectorDefinition
import com.example.game.WebSectors
import com.example.multiplayer.CoopMode
import com.example.multiplayer.CoopRoomStatus
import com.example.multiplayer.CoopSessionState
import com.example.multiplayer.CoopWorldSnapshot
import com.example.multiplayer.DiscoveredLanHost
import com.example.multiplayer.LocalLanMultiplayerManager
import com.example.multiplayer.OnlineMultiplayerRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "MainViewModel"

sealed interface AppScreen {
    data object Hub : AppScreen
    data class Arena(val sector: SectorDefinition, val isCoop: Boolean = false) : AppScreen
}

sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data object GuestMode : LeaderboardUiState
    data class Success(val entries: List<LeaderboardEntry>) : LeaderboardUiState
    data class Error(val message: String) : LeaderboardUiState
}

class MainViewModel(
    private val repository: PlayerProgressRepository,
    private val cloudRepository: CloudSyncRepository,
    private val onlineMultiplayer: OnlineMultiplayerRepository,
    private val lanMultiplayer: LocalLanMultiplayerManager,
    private val currentUserId: String,
    private val displayName: String,
    private val isGuestMode: Boolean = false,
    private val geminiRepository: GeminiTacticalRepository = GeminiTacticalRepository()
) : ViewModel() {

    val playerProgress: StateFlow<PlayerProgress> = repository.progressFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerProgress()
    )

    // Open directly into the Google Homepage-styled Title Screen (Hub)
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Hub)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _coopSession = MutableStateFlow(CoopSessionState())
    val coopSession: StateFlow<CoopSessionState> = _coopSession.asStateFlow()

    val discoveredLanHosts: StateFlow<List<DiscoveredLanHost>> = lanMultiplayer.discoveredHosts

    private val _geminiChatState = MutableStateFlow<GeminiChatUiState>(GeminiChatUiState.Idle)
    val geminiChatState: StateFlow<GeminiChatUiState> = _geminiChatState.asStateFlow()

    private val _geminiChatMessages = MutableStateFlow<List<GeminiChatMessage>>(emptyList())
    val geminiChatMessages: StateFlow<List<GeminiChatMessage>> = _geminiChatMessages.asStateFlow()

    private val _geminiBriefingState = MutableStateFlow<GeminiBriefingUiState>(GeminiBriefingUiState.Idle)
    val geminiBriefingState: StateFlow<GeminiBriefingUiState> = _geminiBriefingState.asStateFlow()

    private val _arenaCoachState = MutableStateFlow<GeminiChatUiState>(GeminiChatUiState.Idle)
    val arenaCoachState: StateFlow<GeminiChatUiState> = _arenaCoachState.asStateFlow()

    private val _inGameDialogState = MutableStateFlow<GeminiChatUiState>(GeminiChatUiState.Idle)
    val inGameDialogState: StateFlow<GeminiChatUiState> = _inGameDialogState.asStateFlow()

    private val _inGameDialogVisible = MutableStateFlow(false)
    val inGameDialogVisible: StateFlow<Boolean> = _inGameDialogVisible.asStateFlow()

    private val _inGameDialogHistory = MutableStateFlow<List<GeminiChatMessage>>(emptyList())
    val inGameDialogHistory: StateFlow<List<GeminiChatMessage>> = _inGameDialogHistory.asStateFlow()

    private var onlineObserveJob: Job? = null
    private var lanObserveJob: Job? = null
    private var geminiChatJob: Job? = null
    private var geminiBriefingJob: Job? = null
    private var arenaCoachJob: Job? = null
    private var inGameDialogJob: Job? = null
    private var lastAutoDialogTimeMs = 0L
    private var nextMessageId = 1L

    val leaderboardState: StateFlow<LeaderboardUiState> = if (isGuestMode) {
        flowOf<LeaderboardUiState>(LeaderboardUiState.GuestMode).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LeaderboardUiState.GuestMode
        )
    } else {
        cloudRepository.observeLeaderboard()
            .map<List<LeaderboardEntry>, LeaderboardUiState> { LeaderboardUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(LeaderboardUiState.Error(rootError.message ?: "Unable to load leaderboard"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = LeaderboardUiState.Loading
            )
    }

    private fun computedMaxHp(): Float {
        val p = playerProgress.value
        return 130f + (p.contextWindowLevel - 1) * 30f + (p.healthStat - 1) * 22f
    }

    fun launchSector(sector: SectorDefinition) {
        _currentScreen.value = AppScreen.Arena(sector, isCoop = false)
    }

    fun launchNextStage(currentSector: SectorDefinition) {
        val idx = WebSectors.all.indexOfFirst { it.id == currentSector.id }
        if (idx in 0 until WebSectors.all.lastIndex) {
            _currentScreen.value = AppScreen.Arena(WebSectors.all[idx + 1], isCoop = false)
        } else {
            _currentScreen.value = AppScreen.Hub
        }
    }

    fun returnToHub() {
        val active = _coopSession.value
        if (active.isActive) {
            leaveCoopRoom()
        }
        _currentScreen.value = AppScreen.Hub
    }

    fun setLeftHanded(leftHanded: Boolean) {
        viewModelScope.launch { repository.setLeftHanded(leftHanded) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { repository.setHighContrast(enabled) }
    }

    fun setLargeTouchTargets(enabled: Boolean) {
        viewModelScope.launch { repository.setLargeTouchTargets(enabled) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { repository.setReducedMotion(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticsEnabled(enabled) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setMusicEnabled(enabled) }
    }

    fun setSelectedMusicTrack(trackId: String) {
        viewModelScope.launch { repository.setSelectedMusicTrack(trackId) }
    }

    fun cycleMusicTrack() {
        val nextId = ClassicalTechnoTracks.nextTrackId(playerProgress.value.selectedMusicTrackId)
        viewModelScope.launch { repository.setSelectedMusicTrack(nextId) }
    }

    fun purchaseUpgrade(type: UpgradeType) {
        viewModelScope.launch {
            repository.purchaseUpgrade(type)
            if (!isGuestMode) {
                cloudRepository.syncProgress(playerProgress.value, displayName)
            }
        }
    }

    fun allocateSkillPoint(stat: StatType) {
        viewModelScope.launch {
            repository.allocateSkillPoint(stat)
            if (!isGuestMode) {
                cloudRepository.syncProgress(playerProgress.value, displayName)
            }
        }
    }

    fun syncCloudNow() {
        if (isGuestMode) return
        viewModelScope.launch {
            cloudRepository.syncProgress(playerProgress.value, displayName)
        }
    }

    fun recordMissionResult(
        sectorId: String,
        coinsEarned: Int,
        starsEarned: Int,
        victory: Boolean,
        virusesKilled: Int,
        adwareKilled: Int,
        hackersKilled: Int,
        xpEarned: Int
    ) {
        viewModelScope.launch {
            repository.addXp(xpEarned)
            repository.recordMissionResult(
                sectorId = sectorId,
                coinsEarned = coinsEarned,
                starsEarned = starsEarned,
                victory = victory,
                virusesKilled = virusesKilled,
                adwareKilled = adwareKilled,
                hackersKilled = hackersKilled
            )
            if (!isGuestMode) {
                cloudRepository.syncProgress(playerProgress.value, displayName)
            }
        }
    }

    // --- 4-Player Co-Op Multiplayer (Online Firestore + Local NSD/TCP LAN) ---

    fun hostOnlineRoom(sectorId: String) {
        if (isGuestMode) {
            _coopSession.value = CoopSessionState(
                mode = CoopMode.ONLINE,
                errorMessage = "Online Cloud Rooms require Google Sign-In. Use Local Devices (Wi-Fi) or sign in with Google."
            )
            return
        }
        leaveCoopRoom()
        _coopSession.value = CoopSessionState(mode = CoopMode.ONLINE, isConnecting = true)
        viewModelScope.launch {
            val res = onlineMultiplayer.createRoom(sectorId, displayName, computedMaxHp())
            res.onSuccess { code ->
                observeOnlineRoom(code)
            }.onFailure { err ->
                _coopSession.value = CoopSessionState(
                    mode = CoopMode.ONLINE,
                    errorMessage = err.localizedMessage ?: "Failed to create online room"
                )
            }
        }
    }

    fun joinOnlineRoom(roomCode: String) {
        if (isGuestMode) {
            _coopSession.value = CoopSessionState(
                mode = CoopMode.ONLINE,
                errorMessage = "Online Cloud Rooms require Google Sign-In. Use Local Devices (Wi-Fi) or sign in with Google."
            )
            return
        }
        leaveCoopRoom()
        _coopSession.value = CoopSessionState(mode = CoopMode.ONLINE, isConnecting = true)
        viewModelScope.launch {
            val res = onlineMultiplayer.joinRoom(roomCode, displayName, computedMaxHp())
            res.onSuccess { code ->
                observeOnlineRoom(code)
            }.onFailure { err ->
                _coopSession.value = CoopSessionState(
                    mode = CoopMode.ONLINE,
                    errorMessage = err.localizedMessage ?: "Failed to join online room"
                )
            }
        }
    }

    private fun observeOnlineRoom(roomCode: String) {
        onlineObserveJob?.cancel()
        onlineObserveJob = viewModelScope.launch {
            onlineMultiplayer.observeSession(roomCode).collect { state ->
                _coopSession.value = state
                autoEnterCoopArenaIfStarted(state)
            }
        }
    }

    fun hostLanRoom(sectorId: String) {
        leaveCoopRoom()
        lanMultiplayer.startHost(currentUserId, displayName, sectorId, computedMaxHp())
        observeLanSession()
    }

    fun joinLanHost(host: DiscoveredLanHost) {
        leaveCoopRoom()
        lanMultiplayer.stopDiscovery()
        lanMultiplayer.joinLanHost(host.hostAddress, host.port, currentUserId, displayName, computedMaxHp())
        observeLanSession()
    }

    fun joinLanByAddress(endpoint: String) {
        val parts = endpoint.trim().split(":")
        val host = parts.getOrNull(0)?.trim().orEmpty()
        val port = parts.getOrNull(1)?.trim()?.toIntOrNull()
        if (host.isEmpty() || port == null || port !in 1..65535) {
            _coopSession.value = _coopSession.value.copy(
                mode = CoopMode.LOCAL_LAN,
                errorMessage = "Enter a valid IP:Port (for example 192.168.1.42:41234)"
            )
            return
        }
        leaveCoopRoom()
        lanMultiplayer.stopDiscovery()
        lanMultiplayer.joinLanHost(host, port, currentUserId, displayName, computedMaxHp())
        observeLanSession()
    }

    private fun observeLanSession() {
        lanObserveJob?.cancel()
        lanObserveJob = viewModelScope.launch {
            lanMultiplayer.session.collect { state ->
                _coopSession.value = state
                autoEnterCoopArenaIfStarted(state)
            }
        }
    }

    private fun autoEnterCoopArenaIfStarted(state: CoopSessionState) {
        if (state.isActive && state.status == CoopRoomStatus.PLAYING && _currentScreen.value is AppScreen.Hub) {
            val sector = WebSectors.all.firstOrNull { it.id == state.sectorId } ?: WebSectors.all.first()
            _currentScreen.value = AppScreen.Arena(sector, isCoop = true)
        }
    }

    fun startLanDiscovery() {
        lanMultiplayer.startDiscovery()
    }

    fun stopLanDiscovery() {
        lanMultiplayer.stopDiscovery()
    }

    fun toggleCoopReady() {
        val state = _coopSession.value
        if (!state.isActive) return
        val currentlyReady = state.members.firstOrNull { it.uid == currentUserId }?.ready ?: false
        if (state.mode == CoopMode.ONLINE) {
            viewModelScope.launch { onlineMultiplayer.setReady(state.roomCode, !currentlyReady) }
        } else {
            lanMultiplayer.setReady(currentUserId, !currentlyReady)
        }
    }

    fun selectCoopSector(sectorId: String) {
        val state = _coopSession.value
        if (!state.isActive || !state.isHost) return
        if (state.mode == CoopMode.ONLINE) {
            viewModelScope.launch { onlineMultiplayer.selectSector(state.roomCode, sectorId) }
        } else {
            lanMultiplayer.selectSector(sectorId)
        }
    }

    fun startCoopStage() {
        val state = _coopSession.value
        if (!state.isActive || !state.isHost) return
        if (state.mode == CoopMode.ONLINE) {
            viewModelScope.launch { onlineMultiplayer.startGame(state.roomCode) }
        } else {
            lanMultiplayer.startGame()
        }
    }

    fun leaveCoopRoom() {
        val state = _coopSession.value
        onlineObserveJob?.cancel()
        onlineObserveJob = null
        lanObserveJob?.cancel()
        lanObserveJob = null
        if (state.isActive && state.mode == CoopMode.ONLINE && state.roomCode.isNotBlank()) {
            viewModelScope.launch { onlineMultiplayer.leaveRoom(state.roomCode) }
        }
        lanMultiplayer.stopSession()
        _coopSession.value = CoopSessionState()
    }

    fun publishLocalPlayerState(
        x: Float,
        y: Float,
        facingAngle: Float,
        hp: Float,
        maxHp: Float,
        isGuarding: Boolean,
        isSlashing: Boolean,
        slashComboStep: Int,
        walkPhase: Float,
        hitSeq: Long,
        lastHitEnemyId: Int,
        lastHitDamage: Float
    ) {
        val state = _coopSession.value
        if (!state.isActive) return
        if (state.mode == CoopMode.ONLINE) {
            viewModelScope.launch {
                onlineMultiplayer.publishLocalPlayerState(
                    roomCode = state.roomCode,
                    x = x,
                    y = y,
                    facingAngle = facingAngle,
                    hp = hp,
                    maxHp = maxHp,
                    isGuarding = isGuarding,
                    isSlashing = isSlashing,
                    slashComboStep = slashComboStep,
                    walkPhase = walkPhase,
                    hitSeq = hitSeq,
                    lastHitEnemyId = lastHitEnemyId,
                    lastHitDamage = lastHitDamage
                )
            }
        } else {
            lanMultiplayer.publishLocalPlayerState(
                uid = currentUserId,
                x = x,
                y = y,
                facingAngle = facingAngle,
                hp = hp,
                maxHp = maxHp,
                isGuarding = isGuarding,
                isSlashing = isSlashing,
                slashComboStep = slashComboStep,
                walkPhase = walkPhase,
                hitSeq = hitSeq,
                lastHitEnemyId = lastHitEnemyId,
                lastHitDamage = lastHitDamage
            )
        }
    }

    fun publishHostWorldState(snapshot: CoopWorldSnapshot, status: CoopRoomStatus) {
        val state = _coopSession.value
        if (!state.isActive || !state.isHost) return
        if (state.mode == CoopMode.ONLINE) {
            viewModelScope.launch {
                onlineMultiplayer.publishHostWorldState(state.roomCode, snapshot, status)
            }
        } else {
            lanMultiplayer.publishHostWorldState(snapshot, status)
        }
    }

    // --- Firebase AI Logic (Gemini Tactical Oracle & Briefings) ---

    fun askGemini(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return

        geminiChatJob?.cancel()
        val previousHistory = _geminiChatMessages.value
        val userMsg = GeminiChatMessage(id = nextMessageId++, isUser = true, text = prompt)
        _geminiChatMessages.value = previousHistory + userMsg

        geminiChatJob = viewModelScope.launch {
            _geminiChatState.value = GeminiChatUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    geminiRepository.streamTacticalPrompt(
                        prompt = prompt,
                        history = previousHistory,
                        progress = playerProgress.value,
                        coopSession = _coopSession.value
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        _geminiChatState.value = GeminiChatUiState.Streaming(accumulated.toString())
                    }
                }
                val finalAnswer = accumulated.toString().trim()
                if (finalAnswer.isNotEmpty()) {
                    _geminiChatMessages.value = _geminiChatMessages.value + GeminiChatMessage(
                        id = nextMessageId++,
                        isUser = false,
                        text = finalAnswer
                    )
                    _geminiChatState.value = GeminiChatUiState.Success(finalAnswer)
                } else {
                    _geminiChatState.value = GeminiChatUiState.Error("Gemini returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                val partial = accumulated.toString().trim()
                if (partial.isNotEmpty()) {
                    _geminiChatMessages.value = _geminiChatMessages.value + GeminiChatMessage(
                        id = nextMessageId++,
                        isUser = false,
                        text = partial
                    )
                    _geminiChatState.value = GeminiChatUiState.Success(partial)
                } else {
                    Log.e(TAG, "Gemini streaming timed out", e)
                    _geminiChatState.value = GeminiChatUiState.Error(
                        "Gemini request timed out after 60 seconds. Please try again."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini AI streaming failed", e)
                _geminiChatState.value = GeminiChatUiState.Error(
                    e.localizedMessage ?: "Failed to connect to Gemini AI."
                )
            }
        }
    }

    fun clearGeminiChat() {
        geminiChatJob?.cancel()
        _geminiChatMessages.value = emptyList()
        _geminiChatState.value = GeminiChatUiState.Idle
    }

    fun refreshGeminiBriefing() {
        geminiBriefingJob?.cancel()
        geminiBriefingJob = viewModelScope.launch {
            _geminiBriefingState.value = GeminiBriefingUiState.Loading
            try {
                val briefing = withTimeout(60_000L) {
                    geminiRepository.generateStructuredBriefing(
                        progress = playerProgress.value,
                        coopSession = _coopSession.value
                    )
                }
                _geminiBriefingState.value = GeminiBriefingUiState.Success(briefing)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini briefing timed out", e)
                _geminiBriefingState.value = GeminiBriefingUiState.Error(
                    "Briefing timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini structured briefing failed", e)
                _geminiBriefingState.value = GeminiBriefingUiState.Error(
                    e.localizedMessage ?: "Failed to generate Gemini mission briefing."
                )
            }
        }
    }

    fun askArenaCoach(sector: SectorDefinition, hpPercent: Int, wave: Int, isCoop: Boolean) {
        arenaCoachJob?.cancel()
        arenaCoachJob = viewModelScope.launch {
            _arenaCoachState.value = GeminiChatUiState.Loading
            try {
                val tip = withTimeout(60_000L) {
                    geminiRepository.generateQuickArenaTip(
                        sector = sector,
                        progress = playerProgress.value,
                        hpPercent = hpPercent,
                        wave = wave,
                        isCoop = isCoop
                    )
                }
                _arenaCoachState.value = GeminiChatUiState.Success(tip)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini arena coach timed out", e)
                _arenaCoachState.value = GeminiChatUiState.Error(
                    "Coach request timed out after 60 seconds."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini arena coach failed", e)
                _arenaCoachState.value = GeminiChatUiState.Error(
                    e.localizedMessage ?: "Unable to fetch Gemini combat tip."
                )
            }
        }
    }

    fun resetArenaCoach() {
        arenaCoachJob?.cancel()
        _arenaCoachState.value = GeminiChatUiState.Idle
        resetInGameDialog()
    }

    fun triggerInGameDialog(
        trigger: InGameDialogTrigger,
        sector: SectorDefinition,
        hpPercent: Int,
        wave: Int,
        nodesIndexed: Int,
        totalNodes: Int,
        bossName: String?,
        isCoop: Boolean,
        customQuestion: String? = null
    ) {
        val isAutoTrigger = trigger == InGameDialogTrigger.STAGE_ENTRY ||
            trigger == InGameDialogTrigger.WAVE_ADVANCE ||
            trigger == InGameDialogTrigger.LOW_HP

        val nowMs = System.currentTimeMillis()
        if (isAutoTrigger && nowMs - lastAutoDialogTimeMs < 15_000L && trigger != InGameDialogTrigger.STAGE_ENTRY) {
            return
        }
        if (isAutoTrigger || trigger == InGameDialogTrigger.BOSS_SPAWN) {
            lastAutoDialogTimeMs = nowMs
        }

        val cleanedQuestion = customQuestion?.trim()?.takeIf { it.isNotEmpty() }
        if (trigger == InGameDialogTrigger.USER_QUESTION && cleanedQuestion == null) return

        inGameDialogJob?.cancel()
        val previousHistory = _inGameDialogHistory.value
        if (cleanedQuestion != null) {
            _inGameDialogHistory.value = previousHistory + GeminiChatMessage(
                id = nextMessageId++,
                isUser = true,
                text = cleanedQuestion
            )
        }

        _inGameDialogVisible.value = true
        inGameDialogJob = viewModelScope.launch {
            _inGameDialogState.value = GeminiChatUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(45_000L) {
                    val streamCompleted = withTimeoutOrNull(15_000L) {
                        geminiRepository.streamInGameDialog(
                            trigger = trigger,
                            sector = sector,
                            progress = playerProgress.value,
                            hpPercent = hpPercent,
                            wave = wave,
                            nodesIndexed = nodesIndexed,
                            totalNodes = totalNodes,
                            bossName = bossName,
                            isCoop = isCoop,
                            history = previousHistory,
                            customQuestion = cleanedQuestion
                        ).collect { chunk ->
                            accumulated.append(chunk)
                            _inGameDialogState.value = GeminiChatUiState.Streaming(accumulated.toString())
                        }
                        true
                    }
                    if (streamCompleted == null && accumulated.isBlank()) {
                        val unaryReply = geminiRepository.generateInGameDialogUnary(
                            trigger = trigger,
                            sector = sector,
                            progress = playerProgress.value,
                            hpPercent = hpPercent,
                            wave = wave,
                            nodesIndexed = nodesIndexed,
                            totalNodes = totalNodes,
                            bossName = bossName,
                            isCoop = isCoop,
                            history = previousHistory,
                            customQuestion = cleanedQuestion
                        )
                        accumulated.append(unaryReply)
                    }
                }
                val finalLine = accumulated.toString().trim()
                if (finalLine.isNotEmpty()) {
                    _inGameDialogHistory.value = _inGameDialogHistory.value + GeminiChatMessage(
                        id = nextMessageId++,
                        isUser = false,
                        text = finalLine
                    )
                    _inGameDialogState.value = GeminiChatUiState.Success(finalLine)
                    _arenaCoachState.value = GeminiChatUiState.Success(finalLine)
                } else {
                    _inGameDialogState.value = GeminiChatUiState.Error("Gemini returned an empty dialog line.")
                }
            } catch (e: TimeoutCancellationException) {
                val partial = accumulated.toString().trim()
                if (partial.isNotEmpty()) {
                    _inGameDialogHistory.value = _inGameDialogHistory.value + GeminiChatMessage(
                        id = nextMessageId++,
                        isUser = false,
                        text = partial
                    )
                    _inGameDialogState.value = GeminiChatUiState.Success(partial)
                    _arenaCoachState.value = GeminiChatUiState.Success(partial)
                } else {
                    Log.e(TAG, "In-game Gemini dialog timed out", e)
                    _inGameDialogState.value = GeminiChatUiState.Error(
                        "Gemini dialog timed out after 45 seconds. Tap retry."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "In-game Gemini dialog failed", e)
                _inGameDialogState.value = GeminiChatUiState.Error(
                    e.localizedMessage ?: "Gemini AI dialog unavailable."
                )
            }
        }
    }

    fun dismissInGameDialog() {
        _inGameDialogVisible.value = false
    }

    fun resetInGameDialog() {
        inGameDialogJob?.cancel()
        lastAutoDialogTimeMs = 0L
        _inGameDialogVisible.value = false
        _inGameDialogState.value = GeminiChatUiState.Idle
        _inGameDialogHistory.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        lanMultiplayer.release()
    }
}
