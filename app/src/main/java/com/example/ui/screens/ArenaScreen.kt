package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import com.example.ai.GeminiChatMessage
import com.example.ai.GeminiChatUiState
import com.example.ai.InGameDialogTrigger
import com.example.audio.ClassicalTechnoTracks
import kotlinx.coroutines.delay
import com.example.audio.SoundEffectManager
import com.example.data.PlayerProgress
import com.example.game.ArenaHudState
import com.example.game.ArenaTouchController
import com.example.game.AutopilotStance
import com.example.game.GameEngine
import com.example.game.HapticEvent
import com.example.game.SectorDefinition
import com.example.game.arenaTouchController
import com.example.multiplayer.CoopMode
import com.example.multiplayer.CoopRoomStatus
import com.example.multiplayer.CoopSessionState
import com.example.multiplayer.CoopWorldSnapshot
import com.example.ui.components.AbilityButton
import com.example.ui.components.GeminiCharacterEmblem
import com.example.ui.components.HoldableGuardButton
import com.example.ui.components.VirtualJoystick
import com.example.ui.components.WebArenaCanvas
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.DeepWebVoid
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GoogleGold
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VirusCrimson

@Composable
fun ArenaScreen(
    sector: SectorDefinition,
    progress: PlayerProgress,
    hasNextStage: Boolean,
    isCoop: Boolean = false,
    coopSession: CoopSessionState = CoopSessionState(),
    arenaCoachState: GeminiChatUiState = GeminiChatUiState.Idle,
    inGameDialogState: GeminiChatUiState = GeminiChatUiState.Idle,
    inGameDialogVisible: Boolean = false,
    inGameDialogHistory: List<GeminiChatMessage> = emptyList(),
    onTriggerInGameDialog: (
        trigger: InGameDialogTrigger,
        hpPercent: Int,
        wave: Int,
        nodesIndexed: Int,
        totalNodes: Int,
        bossName: String?,
        customQuestion: String?
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onDismissInGameDialog: () -> Unit = {},
    onAskArenaCoach: (hpPercent: Int, wave: Int) -> Unit = { _, _ -> },
    onPublishLocalPlayerState: (
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onPublishHostWorldState: (CoopWorldSnapshot, CoopRoomStatus) -> Unit = { _, _ -> },
    onToggleHandedness: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit = {},
    onCycleMusicTrack: () -> Unit = {},
    onMusicDuckChanged: (Boolean) -> Unit = {},
    onBossStateChanged: (Boolean) -> Unit = {},
    onNextStage: () -> Unit,
    onMissionComplete: (
        coinsEarned: Int,
        starsEarned: Int,
        victory: Boolean,
        virusesKilled: Int,
        adwareKilled: Int,
        hackersKilled: Int,
        xpEarned: Int
    ) -> Unit,
    onExitToHub: () -> Unit
) {
    var sessionKey by remember { mutableLongStateOf(0L) }
    val engine = remember(sector.id, sessionKey, isCoop, coopSession.isHost) {
        GameEngine(sector, progress).apply {
            isCoopGuest = isCoop && !coopSession.isHost
        }
    }
    val touchController = remember(engine) { ArenaTouchController(engine) }
    var hudState by remember(sector.id, sessionKey) { mutableStateOf(engine.snapshotHud()) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var recordedResult by remember(sector.id, sessionKey) { mutableStateOf(false) }
    var showGeminiDialogModal by remember(sector.id, sessionKey) { mutableStateOf(false) }
    var lowHpWarned by remember(sector.id, sessionKey) { mutableStateOf(false) }

    BackHandler {
        if (showGeminiDialogModal) {
            showGeminiDialogModal = false
            engine.isPaused = false
            hudState = engine.snapshotHud()
        } else if (!hudState.isMissionOver) {
            engine.isPaused = !engine.isPaused
            hudState = engine.snapshotHud()
        } else {
            onExitToHub()
        }
    }

    val view = LocalView.current
    val soundManager = remember { SoundEffectManager() }
    val processedHitSeqByUid = remember(sessionKey) { mutableMapOf<String, Long>() }

    LaunchedEffect(progress.soundEnabled) {
        soundManager.enabled = progress.soundEnabled
    }

    LaunchedEffect(hudState.isPaused, hudState.isMissionOver, showGeminiDialogModal) {
        onMusicDuckChanged(hudState.isPaused || hudState.isMissionOver || showGeminiDialogModal)
    }

    val isBossActive = !hudState.isMissionOver && hudState.bossName != null && (hudState.bossHp ?: 0f) > 0f
    LaunchedEffect(isBossActive) {
        onBossStateChanged(isBossActive)
    }
    DisposableEffect(Unit) {
        onDispose { onBossStateChanged(false) }
    }

    // Automatic Event-Driven Gemini AI Companion Dialogs during battle
    val currentHpPercent = ((hudState.hp / hudState.maxHp.coerceAtLeast(1f)) * 100f).toInt().coerceIn(0, 100)
    LaunchedEffect(sector.id, sessionKey) {
        onTriggerInGameDialog(
            InGameDialogTrigger.STAGE_ENTRY,
            100,
            1,
            0,
            sector.waves,
            null,
            null
        )
    }

    LaunchedEffect(hudState.wave, sector.id, sessionKey) {
        if (hudState.wave > 1 && !hudState.isMissionOver) {
            onTriggerInGameDialog(
                InGameDialogTrigger.WAVE_ADVANCE,
                currentHpPercent,
                hudState.wave,
                hudState.nodesIndexed,
                hudState.totalNodes,
                hudState.bossName,
                null
            )
        }
    }

    LaunchedEffect(hudState.bossName != null, sector.id, sessionKey) {
        val bName = hudState.bossName
        if (bName != null && !hudState.isMissionOver) {
            onTriggerInGameDialog(
                InGameDialogTrigger.BOSS_SPAWN,
                currentHpPercent,
                hudState.wave,
                hudState.nodesIndexed,
                hudState.totalNodes,
                bName,
                null
            )
        }
    }

    LaunchedEffect(currentHpPercent <= 35, sector.id, sessionKey) {
        if (currentHpPercent in 1..35 && !lowHpWarned && !hudState.isMissionOver) {
            lowHpWarned = true
            onTriggerInGameDialog(
                InGameDialogTrigger.LOW_HP,
                currentHpPercent,
                hudState.wave,
                hudState.nodesIndexed,
                hudState.totalNodes,
                hudState.bossName,
                null
            )
        }
    }

    LaunchedEffect(inGameDialogState, inGameDialogVisible, showGeminiDialogModal) {
        if (inGameDialogVisible && !showGeminiDialogModal) {
            when (inGameDialogState) {
                is GeminiChatUiState.Success -> {
                    delay(9_500L)
                    onDismissInGameDialog()
                }
                is GeminiChatUiState.Error -> {
                    delay(10_000L)
                    onDismissInGameDialog()
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(isCoop, coopSession.remotePlayers, coopSession.hostWorldSnapshot) {
        if (isCoop) {
            engine.updateRemotePlayers(coopSession.remotePlayers)
            if (coopSession.isHost) {
                coopSession.remotePlayers.forEach { rp ->
                    val prevSeq = processedHitSeqByUid[rp.uid] ?: 0L
                    if (rp.hitSeq > prevSeq) {
                        processedHitSeqByUid[rp.uid] = rp.hitSeq
                        engine.applyRemotePlayerHit(rp.lastHitEnemyId, rp.lastHitDamage)
                    }
                }
            } else {
                coopSession.hostWorldSnapshot?.let { snap ->
                    engine.applyHostWorldSnapshot(snap)
                }
            }
        }
    }

    DisposableEffect(soundManager) {
        onDispose { soundManager.release() }
    }

    val latestCoopSession by rememberUpdatedState(coopSession)
    val latestOnPublishLocalPlayerState by rememberUpdatedState(onPublishLocalPlayerState)
    val latestOnPublishHostWorldState by rememberUpdatedState(onPublishHostWorldState)

    LaunchedEffect(engine, progress.hapticsEnabled, isCoop) {
        var lastNanos = 0L
        var lastSyncNanos = 0L
        var lastReportedHitSeq = 0L

        while (true) {
            withFrameNanos { nanos ->
                val activeSession = latestCoopSession
                val syncIntervalNanos = if (activeSession.mode == CoopMode.LOCAL_LAN) 55_000_000L else 110_000_000L
                if (lastNanos == 0L) lastNanos = nanos
                val dt = (nanos - lastNanos) / 1_000_000_000f
                lastNanos = nanos

                engine.tick(dt, touchController.dpadInput.x, touchController.dpadInput.y)
                val haptics = engine.drainHapticEvents()
                if (progress.hapticsEnabled) {
                    for (event in haptics) {
                        val flag = when (event) {
                            HapticEvent.LIGHT_ATTACK -> HapticFeedbackConstants.KEYBOARD_TAP
                            HapticEvent.HEAVY_ATTACK -> HapticFeedbackConstants.LONG_PRESS
                            HapticEvent.DAMAGE_TAKEN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                HapticFeedbackConstants.REJECT
                            } else {
                                HapticFeedbackConstants.LONG_PRESS
                            }
                            HapticEvent.SHIELD_BLOCK -> HapticFeedbackConstants.CONTEXT_CLICK
                            HapticEvent.ITEM_COLLECTED -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                HapticFeedbackConstants.CONFIRM
                            } else {
                                HapticFeedbackConstants.KEYBOARD_TAP
                            }
                            HapticEvent.DASH -> HapticFeedbackConstants.VIRTUAL_KEY
                        }
                        view.performHapticFeedback(flag)
                    }
                }

                val sounds = engine.drainSoundEvents()
                for (sound in sounds) {
                    soundManager.play(sound)
                }

                val snap = engine.snapshotHud()
                hudState = snap
                frameTick++

                val activeCoop = latestCoopSession
                if (isCoop && activeCoop.isActive) {
                    val hitChanged = engine.hitSeqCounter > lastReportedHitSeq
                    val intervalElapsed = nanos - lastSyncNanos >= syncIntervalNanos
                    if (hitChanged || intervalElapsed || (snap.isMissionOver && !recordedResult)) {
                        lastSyncNanos = nanos
                        lastReportedHitSeq = engine.hitSeqCounter
                        val p = engine.player
                        latestOnPublishLocalPlayerState(
                            p.x,
                            p.y,
                            p.facingAngle,
                            p.hp,
                            p.maxHp,
                            p.isGuarding,
                            p.slashActiveTimer > 0f,
                            p.slashComboStep,
                            p.walkPhase,
                            engine.hitSeqCounter,
                            engine.lastHitEnemyId,
                            engine.lastHitDamage
                        )
                        if (activeCoop.isHost) {
                            val status = if (snap.isMissionOver) CoopRoomStatus.FINISHED else CoopRoomStatus.PLAYING
                            latestOnPublishHostWorldState(engine.exportWorldSnapshot(), status)
                        }
                    }
                }

                if (snap.isMissionOver && !recordedResult) {
                    recordedResult = true
                    onMissionComplete(
                        snap.coinsCollected,
                        snap.starsEarned,
                        snap.isVictory,
                        snap.virusesKilled,
                        snap.adwareKilled,
                        snap.hackersKilled,
                        snap.xpEarnedThisStage
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepWebVoid)
    ) {
        // 3D Projected Cyber-Web Arena wired to ArenaTouchController
        WebArenaCanvas(
            engine = engine,
            frameTick = frameTick,
            reducedMotion = progress.reducedMotion,
            modifier = Modifier
                .fillMaxSize()
                .arenaTouchController(touchController)
        )

        // Top Tactical HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colorful Gemini Character Emblem + HP & Shield Panel
                GeminiCharacterEmblem(
                    size = 50.dp,
                    modifier = Modifier.testTag("arena_gemini_emblem")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (progress.highContrast) Color.Black else Color(0xFF101B3D).copy(alpha = 0.90f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (progress.highContrast) 2.dp else 1.5.dp,
                        if (progress.highContrast) Color.White else GeminiCyan.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GEMINI CORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = GeminiCyan
                            )
                            Text(
                                text = "${hudState.hp.toInt()} / ${hudState.maxHp.toInt()}" +
                                    if (hudState.isGuarding) " [GUARD]" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hudState.isGuarding) GoogleGold else TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = { (hudState.hp / hudState.maxHp).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape),
                            color = if (hudState.hp / hudState.maxHp > 0.3f) GoogleGreen else VirusCrimson,
                            trackColor = DeepWebVoid
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (hudState.shieldStamina / hudState.maxShieldStamina).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = GoogleGold,
                            trackColor = DeepWebVoid
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Coins & Wave Pill
                Surface(
                    color = Color(0xFF101B3D).copy(alpha = 0.90f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoogleGold.copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MonetizationOn,
                            contentDescription = "Google Coins",
                            tint = GoogleGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${hudState.coinsCollected}",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoogleGold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "W${hudState.wave}/${hudState.totalWaves}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Handedness Toggle (R / L)
                Surface(
                    onClick = { onToggleHandedness(!progress.isLeftHanded) },
                    color = CyberSurface.copy(alpha = 0.85f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GeminiCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("handedness_toggle_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (progress.isLeftHanded) "L" else "R",
                            style = MaterialTheme.typography.labelLarge,
                            color = GeminiCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Live Gemini AI In-Game Comms Button
                IconButton(
                    onClick = {
                        engine.isPaused = true
                        hudState = engine.snapshotHud()
                        showGeminiDialogModal = true
                        if (inGameDialogState is GeminiChatUiState.Idle) {
                            onTriggerInGameDialog(
                                InGameDialogTrigger.MANUAL_CALLOUT,
                                currentHpPercent,
                                hudState.wave,
                                hudState.nodesIndexed,
                                hudState.totalNodes,
                                hudState.bossName,
                                null
                            )
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GeminiBlue.copy(alpha = 0.85f), GoogleGreen.copy(alpha = 0.85f))
                            )
                        )
                        .border(1.dp, GoogleGold.copy(alpha = 0.85f), CircleShape)
                        .testTag("arena_gemini_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Ask Gemini AI In-Game",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Pause / Menu Button
                IconButton(
                    onClick = {
                        engine.isPaused = !engine.isPaused
                        hudState = engine.snapshotHud()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyberSurface.copy(alpha = 0.85f))
                        .border(1.dp, CyberSurfaceBorder, CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = if (hudState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = "Pause Mission",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Search Request Banner
            Surface(
                color = CyberSurface.copy(alpha = 0.78f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, sector.primaryColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = sector.primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sector.userSearchQuery,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nodes ${hudState.nodesIndexed}/${hudState.totalNodes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoogleGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // AI Autopilot Control Strip (one-tap toggle & stance selector)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        engine.setAutopilot(!hudState.isAutopilotEnabled)
                        hudState = engine.snapshotHud()
                    },
                    color = if (hudState.isAutopilotEnabled) {
                        GoogleGreen.copy(alpha = 0.24f)
                    } else {
                        CyberSurface.copy(alpha = 0.82f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.25.dp,
                        color = if (hudState.isAutopilotEnabled) GoogleGreen else CyberSurfaceBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("arena_autopilot_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = if (hudState.isAutopilotEnabled) {
                                "Disable AI Autopilot"
                            } else {
                                "Enable AI Autopilot"
                            },
                            tint = if (hudState.isAutopilotEnabled) GoogleGreen else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hudState.isAutopilotEnabled) {
                                "AUTOPILOT ON • ${hudState.autopilotStatus}"
                            } else {
                                "AI AUTOPILOT: OFF (TAP TO AUTO-PLAY)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hudState.isAutopilotEnabled) Color.White else TextSecondary,
                            fontWeight = if (hudState.isAutopilotEnabled) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (hudState.isAutopilotEnabled) {
                    Surface(
                        onClick = {
                            engine.cycleAutopilotStance()
                            hudState = engine.snapshotHud()
                        },
                        color = CyberSurfaceElevated.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoogleGold.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("arena_autopilot_stance_button")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = hudState.autopilotStance.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = GoogleGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Active Grabbed Item Buffs Row
            if (hudState.hasAntiVirusGun || hudState.hasAdBlockBubble || hudState.hasSpeedUp || hudState.hasAttackMultiplier) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hudState.hasAntiVirusGun) BuffChip("AUTO-GUN", GoogleGreen)
                    if (hudState.hasAdBlockBubble) BuffChip("AD-BLOCK", GeminiCyan)
                    if (hudState.hasSpeedUp) BuffChip("SPEED+", GeminiBlue)
                    if (hudState.hasAttackMultiplier) BuffChip("2X ATK", GoogleGold)
                }
            }

            // 4-Player Co-Op Squad Bar
            if (isCoop && coopSession.remotePlayers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    coopSession.remotePlayers.take(3).forEach { rp ->
                        val slotColor = when (rp.colorIndex.mod(4)) {
                            0 -> GeminiBlue
                            1 -> VirusCrimson
                            2 -> GoogleGold
                            else -> GoogleGreen
                        }
                        Surface(
                            color = CyberSurface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, slotColor.copy(alpha = 0.8f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(
                                    text = rp.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = slotColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { (rp.hp / rp.maxHp.coerceAtLeast(1f)).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = if (rp.isAlive) GoogleGreen else VirusCrimson,
                                    trackColor = DeepWebVoid
                                )
                            }
                        }
                    }
                }
            }

            // Boss Health Bar
            if (hudState.bossHp != null && hudState.bossMaxHp != null && hudState.bossName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = VirusCrimson.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VirusCrimson.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(
                            text = "BOSS • ${hudState.bossName!!.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VirusCrimson
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (hudState.bossHp!! / hudState.bossMaxHp!!).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = VirusCrimson,
                            trackColor = DeepWebVoid
                        )
                    }
                }
            }

            // Wave Alert Banner
            AnimatedVisibility(
                visible = hudState.bannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
            ) {
                Surface(
                    color = DeepWebVoid.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoogleGold.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = hudState.bannerMessage.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = GoogleGold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Live In-Game Gemini AI Companion Dialog Banner (non-blocking during gameplay)
            AnimatedVisibility(
                visible = inGameDialogVisible && !hudState.isMissionOver && !showGeminiDialogModal && inGameDialogState !is GeminiChatUiState.Idle,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            ) {
                InGameGeminiDialogBubble(
                    dialogState = inGameDialogState,
                    highContrast = progress.highContrast,
                    onAskFollowUp = {
                        engine.isPaused = true
                        hudState = engine.snapshotHud()
                        showGeminiDialogModal = true
                    },
                    onRetry = {
                        onTriggerInGameDialog(
                            InGameDialogTrigger.MANUAL_CALLOUT,
                            currentHpPercent,
                            hudState.wave,
                            hudState.nodesIndexed,
                            hudState.totalNodes,
                            hudState.bossName,
                            null
                        )
                    },
                    onDismiss = onDismissInGameDialog
                )
            }
        }

        // Bottom Controls: Circle Movement Joystick & Interactive Touch Buttons
        // Right-handed (standard): Joystick Left, Action Buttons Right
        // Left-handed: Action Buttons Left, Joystick Right
        if (!hudState.isMissionOver && !hudState.isPaused) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val touchScale = if (progress.largeTouchTargets) 1.18f else 1f
                val joystickSlot: @Composable () -> Unit = {
                    VirtualJoystick(
                        size = if (progress.largeTouchTargets) 156.dp else 132.dp,
                        onMove = { x, y ->
                            touchController.onDirectionalPadMove(x, y)
                        }
                    )
                }

                val buttonsSlot: @Composable () -> Unit = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (progress.isLeftHanded) {
                            AbilityButton(
                                label = "SLASH",
                                icon = Icons.Rounded.AutoAwesome,
                                accentColor = GeminiCyan,
                                readyFraction = hudState.slashReadyFraction,
                                size = (76 * touchScale).dp,
                                testTag = "ability_slash_button",
                                onClick = { engine.triggerSwordSlash() }
                            )
                            HoldableGuardButton(
                                label = "GUARD",
                                icon = Icons.Rounded.Shield,
                                accentColor = GoogleGold,
                                isGuarding = hudState.isGuarding,
                                size = (64 * touchScale).dp,
                                testTag = "ability_guard_button",
                                onGuardChanged = { active -> engine.setGuarding(active) }
                            )
                            AbilityButton(
                                label = "BASH",
                                icon = Icons.Rounded.Bolt,
                                accentColor = GoogleGold,
                                readyFraction = hudState.shieldBashReadyFraction,
                                size = (58 * touchScale).dp,
                                testTag = "ability_bash_button",
                                onClick = { engine.triggerShieldBash() }
                            )
                            AbilityButton(
                                label = "DASH",
                                icon = Icons.Rounded.FlashOn,
                                accentColor = GeminiCyan,
                                readyFraction = hudState.dashReadyFraction,
                                size = (54 * touchScale).dp,
                                testTag = "ability_dash_button",
                                onClick = { engine.triggerDash(touchController.dpadInput.x, touchController.dpadInput.y) }
                            )
                        } else {
                            AbilityButton(
                                label = "DASH",
                                icon = Icons.Rounded.FlashOn,
                                accentColor = GeminiCyan,
                                readyFraction = hudState.dashReadyFraction,
                                size = (54 * touchScale).dp,
                                testTag = "ability_dash_button",
                                onClick = { engine.triggerDash(touchController.dpadInput.x, touchController.dpadInput.y) }
                            )
                            AbilityButton(
                                label = "BASH",
                                icon = Icons.Rounded.Bolt,
                                accentColor = GoogleGold,
                                readyFraction = hudState.shieldBashReadyFraction,
                                size = (58 * touchScale).dp,
                                testTag = "ability_bash_button",
                                onClick = { engine.triggerShieldBash() }
                            )
                            HoldableGuardButton(
                                label = "GUARD",
                                icon = Icons.Rounded.Shield,
                                accentColor = GoogleGold,
                                isGuarding = hudState.isGuarding,
                                size = (64 * touchScale).dp,
                                testTag = "ability_guard_button",
                                onGuardChanged = { active -> engine.setGuarding(active) }
                            )
                            AbilityButton(
                                label = "SLASH",
                                icon = Icons.Rounded.AutoAwesome,
                                accentColor = GeminiCyan,
                                readyFraction = hudState.slashReadyFraction,
                                size = (76 * touchScale).dp,
                                testTag = "ability_slash_button",
                                onClick = { engine.triggerSwordSlash() }
                            )
                        }
                    }
                }

                if (progress.isLeftHanded) {
                    buttonsSlot()
                    joystickSlot()
                } else {
                    joystickSlot()
                    buttonsSlot()
                }
            }
        }

        // Pause Overlay
        if (hudState.isPaused && !hudState.isMissionOver && !showGeminiDialogModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepWebVoid.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberSurfaceBorder),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MISSION PAUSED",
                            style = MaterialTheme.typography.headlineSmall,
                            color = GeminiCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sector.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // AI Autopilot Toggle inside Pause Menu
                        OutlinedButton(
                            onClick = {
                                engine.setAutopilot(!hudState.isAutopilotEnabled)
                                hudState = engine.snapshotHud()
                            },
                            border = BorderStroke(
                                1.dp,
                                if (hudState.isAutopilotEnabled) GoogleGreen else GeminiCyan
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pause_autopilot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SmartToy,
                                contentDescription = null,
                                tint = if (hudState.isAutopilotEnabled) GoogleGreen else GeminiCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hudState.isAutopilotEnabled) {
                                    "AI AUTOPILOT: ON (${hudState.autopilotStance.label})"
                                } else {
                                    "ENABLE AI AUTOPILOT"
                                },
                                color = if (hudState.isAutopilotEnabled) GoogleGreen else GeminiCyan,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Classical Techno Soundtrack Controls inside Pause Menu
                        val resolvedTrack = ClassicalTechnoTracks.resolveTrack(
                            trackId = progress.selectedMusicTrackId,
                            sectorId = sector.id,
                            isBossFight = isBossActive
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onToggleMusic(!progress.musicEnabled) },
                                border = BorderStroke(
                                    1.dp,
                                    if (progress.musicEnabled) {
                                        if (resolvedTrack.isBossTrack) VirusCrimson else GeminiBlue
                                    } else CyberSurfaceBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_music_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (progress.musicEnabled) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff,
                                    contentDescription = if (progress.musicEnabled) "Mute Music" else "Enable Music",
                                    tint = if (progress.musicEnabled) {
                                        if (resolvedTrack.isBossTrack) VirusCrimson else GeminiCyan
                                    } else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (progress.musicEnabled) "MUSIC ON" else "MUSIC OFF",
                                    color = if (progress.musicEnabled) {
                                        if (resolvedTrack.isBossTrack) VirusCrimson else GeminiCyan
                                    } else TextSecondary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            OutlinedButton(
                                onClick = onCycleMusicTrack,
                                border = BorderStroke(1.dp, if (resolvedTrack.isBossTrack) VirusCrimson else GeminiBlue),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("pause_music_cycle_button")
                            ) {
                                Text(
                                    text = if (progress.selectedMusicTrackId == ClassicalTechnoTracks.AUTO_TRACK_ID) {
                                        if (resolvedTrack.isBossTrack) "BOSS: ${resolvedTrack.title}" else "AUTO: ${resolvedTrack.composer}"
                                    } else {
                                        resolvedTrack.title
                                    },
                                    color = if (resolvedTrack.isBossTrack) VirusCrimson else TextPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Gemini AI Tactical Coach inside Pause Menu
                        val hpPct = ((hudState.hp / hudState.maxHp.coerceAtLeast(1f)) * 100f).toInt().coerceIn(0, 100)
                        OutlinedButton(
                            onClick = { onAskArenaCoach(hpPct, hudState.wave) },
                            enabled = arenaCoachState !is GeminiChatUiState.Loading,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoogleGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ask_gemini_coach_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = GoogleGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (arenaCoachState is GeminiChatUiState.Loading) "CONSULTING GEMINI..." else "ASK GEMINI COACH",
                                color = GoogleGold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        when (arenaCoachState) {
                            is GeminiChatUiState.Success -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = CyberSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GeminiCyan.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = arenaCoachState.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            is GeminiChatUiState.Error -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = VirusCrimson.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VirusCrimson),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = arenaCoachState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VirusCrimson,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            else -> Unit
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                engine.isPaused = false
                                hudState = engine.snapshotHud()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("resume_mission_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GeminiCyan, contentColor = DeepWebVoid)
                        ) {
                            Text("RESUME MISSION", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onExitToHub,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("abort_mission_button")
                        ) {
                            Text("STAGE MAP & TECH LAB", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Interactive In-Game Gemini AI Tactical Dialog Modal (Pause-Safe)
        if (showGeminiDialogModal && !hudState.isMissionOver) {
            InGameGeminiTacticalModal(
                sector = sector,
                hpPercent = currentHpPercent,
                wave = hudState.wave,
                nodesIndexed = hudState.nodesIndexed,
                totalNodes = hudState.totalNodes,
                bossName = hudState.bossName,
                dialogState = inGameDialogState,
                dialogHistory = inGameDialogHistory,
                onSendQuestion = { question ->
                    onTriggerInGameDialog(
                        InGameDialogTrigger.USER_QUESTION,
                        currentHpPercent,
                        hudState.wave,
                        hudState.nodesIndexed,
                        hudState.totalNodes,
                        hudState.bossName,
                        question
                    )
                },
                onRetry = {
                    onTriggerInGameDialog(
                        InGameDialogTrigger.MANUAL_CALLOUT,
                        currentHpPercent,
                        hudState.wave,
                        hudState.nodesIndexed,
                        hudState.totalNodes,
                        hudState.bossName,
                        null
                    )
                },
                onResume = {
                    showGeminiDialogModal = false
                    engine.isPaused = false
                    hudState = engine.snapshotHud()
                }
            )
        }

        // Mission Summary Overlay (Victory / Defeat)
        if (hudState.isMissionOver) {
            MissionResultOverlay(
                hudState = hudState,
                hasNextStage = hasNextStage,
                onNextStage = onNextStage,
                onRetry = { sessionKey++ },
                onReturnToHub = onExitToHub
            )
        }
    }
}

@Composable
private fun MissionResultOverlay(
    hudState: ArenaHudState,
    hasNextStage: Boolean,
    onNextStage: () -> Unit,
    onRetry: () -> Unit,
    onReturnToHub: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepWebVoid.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (hudState.isVictory) GoogleGold else VirusCrimson
            ),
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hudState.isVictory) "SEARCH DELIVERED!" else "CONNECTION LOST",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (hudState.isVictory) GoogleGold else VirusCrimson,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (hudState.isVictory) {
                        "User Rating Stars Earned for ${hudState.sector.name}"
                    } else {
                        "Viruses overwhelmed Gemini Core. Upgrade in the Tech Lab!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stars of User Rating
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..5) {
                        val earned = i <= hudState.starsEarned
                        Icon(
                            imageVector = if (earned) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = "Star $i",
                            tint = if (earned) GoogleGold else TextSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Reward Breakdown
                Surface(
                    color = DeepWebVoid.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatRow("Google Coins Collected", "+${hudState.coinsCollected} G", GoogleGold)
                        StatRow("Experience Points", "+${hudState.xpEarnedThisStage} XP", GeminiCyan)
                        StatRow("Search Nodes Indexed", "${hudState.nodesIndexed} / ${hudState.totalNodes}", GeminiCyan)
                        StatRow(
                            "Threats Purged",
                            "${hudState.virusesKilled + hudState.adwareKilled + hudState.hackersKilled}",
                            GoogleGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                if (hudState.isVictory && hasNextStage) {
                    Button(
                        onClick = onNextStage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("next_stage_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoogleGold,
                            contentColor = DeepWebVoid
                        )
                    ) {
                        Text(
                            text = "NEXT STAGE →",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = onReturnToHub,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("return_to_hub_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hudState.isVictory && !hasNextStage) GoogleGold else GeminiCyan,
                        contentColor = DeepWebVoid
                    )
                ) {
                    Text(
                        text = "STAGE MAP & TECH LAB",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("retry_mission_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REPLAY SECTOR", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun BuffChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.22f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = valueColor)
    }
}

@Composable
private fun InGameGeminiDialogBubble(
    dialogState: GeminiChatUiState,
    highContrast: Boolean,
    onAskFollowUp: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val isError = dialogState is GeminiChatUiState.Error
    val surfaceColor = if (highContrast) {
        Color.Black.copy(alpha = 0.94f)
    } else {
        CyberSurfaceElevated.copy(alpha = 0.92f)
    }
    val borderColor = when {
        isError -> VirusCrimson
        highContrast -> GoogleGold
        else -> GeminiCyan.copy(alpha = 0.85f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.25.dp, borderColor),
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .testTag("in_game_gemini_dialog_bubble")
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GeminiCharacterEmblem(
                size = 30.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isError) "GEMINI COMMS OFFLINE" else "GEMINI ORACLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) VirusCrimson else GoogleGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                when (dialogState) {
                    is GeminiChatUiState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = GeminiCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Analyzing combat telemetry...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    is GeminiChatUiState.Streaming -> {
                        Text(
                            text = dialogState.partialText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is GeminiChatUiState.Success -> {
                        Text(
                            text = dialogState.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is GeminiChatUiState.Error -> {
                        Text(
                            text = dialogState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = VirusCrimson,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    GeminiChatUiState.Idle -> Unit
                }
            }
            if (isError) {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("retry_in_game_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Retry Gemini dialog",
                        tint = GoogleGold
                    )
                }
            } else {
                IconButton(
                    onClick = onAskFollowUp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("open_in_game_dialog_modal_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "Ask Gemini tactical question",
                        tint = GeminiCyan
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("dismiss_in_game_dialog_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss Gemini dialog",
                    tint = TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InGameGeminiTacticalModal(
    sector: SectorDefinition,
    hpPercent: Int,
    wave: Int,
    nodesIndexed: Int,
    totalNodes: Int,
    bossName: String?,
    dialogState: GeminiChatUiState,
    dialogHistory: List<GeminiChatMessage>,
    onSendQuestion: (String) -> Unit,
    onRetry: () -> Unit,
    onResume: () -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    val quickPrompts = remember(sector.id, bossName) {
        listOf(
            "How do I beat ${bossName ?: sector.bossName}?",
            "Best combo right now?",
            "Need survival tips!",
            "How to clear Search Nodes fast?"
        )
    }

    Dialog(onDismissRequest = onResume) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, GeminiCyan.copy(alpha = 0.75f)),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .testTag("in_game_gemini_modal")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GeminiCharacterEmblem(
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GEMINI IN-GAME COMMS",
                            style = MaterialTheme.typography.titleSmall,
                            color = GeminiCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sector.name} • Wave $wave/${sector.waves} • HP $hpPercent% • Nodes $nodesIndexed/$totalNodes",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onResume,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_in_game_dialog_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Gemini comms and resume",
                            tint = TextSecondary
                        )
                    }
                }

                // Live Conversation History + Active Stream
                Surface(
                    color = DeepWebVoid.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyberSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dialogHistory, key = { it.id }) { msg ->
                            val isUser = msg.isUser
                            Surface(
                                color = if (isUser) GeminiBlue.copy(alpha = 0.22f) else CyberSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isUser) GeminiBlue.copy(alpha = 0.6f) else GeminiCyan.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isUser) "YOU" else "GEMINI ORACLE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUser) GeminiCyan else GoogleGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        when (dialogState) {
                            is GeminiChatUiState.Loading -> {
                                item(key = "streaming_loading") {
                                    Row(
                                        modifier = Modifier.padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = GeminiCyan
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Gemini Oracle is responding...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            is GeminiChatUiState.Streaming -> {
                                item(key = "streaming_active") {
                                    Surface(
                                        color = CyberSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, GeminiCyan),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "GEMINI ORACLE • LIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoogleGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = dialogState.partialText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                            is GeminiChatUiState.Error -> {
                                item(key = "streaming_error") {
                                    Surface(
                                        color = VirusCrimson.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, VirusCrimson),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = dialogState.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = VirusCrimson
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedButton(
                                                onClick = onRetry,
                                                border = BorderStroke(1.dp, GoogleGold),
                                                modifier = Modifier
                                                    .height(48.dp)
                                                    .testTag("modal_retry_in_game_dialog_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Refresh,
                                                    contentDescription = null,
                                                    tint = GoogleGold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("RETRY COMMS", color = GoogleGold)
                                            }
                                        }
                                    }
                                }
                            }
                            else -> Unit
                        }
                    }
                }

                // Quick Tactical Prompt Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickPrompts.forEachIndexed { idx, promptLabel ->
                        OutlinedButton(
                            onClick = { onSendQuestion(promptLabel) },
                            enabled = dialogState !is GeminiChatUiState.Loading && dialogState !is GeminiChatUiState.Streaming,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, GeminiCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("quick_prompt_chip_$idx")
                        ) {
                            Text(
                                text = promptLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = GeminiCyan
                            )
                        }
                    }
                }

                // Custom Question Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        placeholder = {
                            Text(
                                text = "Ask Gemini for battle advice...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GeminiCyan,
                            unfocusedBorderColor = CyberSurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("in_game_dialog_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val trimmed = questionText.trim()
                            if (trimmed.isNotEmpty()) {
                                questionText = ""
                                onSendQuestion(trimmed)
                            }
                        },
                        enabled = questionText.isNotBlank() &&
                            dialogState !is GeminiChatUiState.Loading &&
                            dialogState !is GeminiChatUiState.Streaming,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GeminiCyan.copy(alpha = 0.2f))
                            .border(1.dp, GeminiCyan, CircleShape)
                            .testTag("in_game_dialog_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send question to Gemini",
                            tint = GeminiCyan
                        )
                    }
                }

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleGreen,
                        contentColor = DeepWebVoid
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_battle_from_gemini_button")
                ) {
                    Text(
                        text = "RESUME BATTLE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

