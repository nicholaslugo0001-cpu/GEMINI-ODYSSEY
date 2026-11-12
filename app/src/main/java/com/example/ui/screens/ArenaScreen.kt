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
import com.example.data.PlayerProgress
import com.example.game.ArenaHudState
import com.example.game.ArenaTouchController
import com.example.game.GameEngine
import com.example.game.HapticEvent
import com.example.game.SectorDefinition
import com.example.game.arenaTouchController
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
    onToggleHandedness: (Boolean) -> Unit,
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
    val engine = remember(sector.id, sessionKey) { GameEngine(sector, progress) }
    val touchController = remember(engine) { ArenaTouchController(engine) }
    var hudState by remember(sector.id, sessionKey) { mutableStateOf(engine.snapshotHud()) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var recordedResult by remember(sector.id, sessionKey) { mutableStateOf(false) }

    BackHandler {
        if (!hudState.isMissionOver) {
            engine.isPaused = !engine.isPaused
            hudState = engine.snapshotHud()
        } else {
            onExitToHub()
        }
    }

    val view = LocalView.current

    LaunchedEffect(engine) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (lastNanos == 0L) lastNanos = nanos
                val dt = (nanos - lastNanos) / 1_000_000_000f
                lastNanos = nanos

                engine.tick(dt, touchController.dpadInput.x, touchController.dpadInput.y)
                val haptics = engine.drainHapticEvents()
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

                val snap = engine.snapshotHud()
                hudState = snap
                frameTick++

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

                Spacer(modifier = Modifier.width(8.dp))

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

                Spacer(modifier = Modifier.width(8.dp))

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

            // Active Grabbed Item Buffs Row
            if (hudState.hasAntiVirusGun || hudState.hasAdBlockBubble || hudState.hasSpeedUp || hudState.hasAttackMultiplier) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hudState.hasAntiVirusGun) BuffChip("AUTO-GUN", GoogleGreen)
                    if (hudState.hasAdBlockBubble) BuffChip("AD-BLOCK", GeminiCyan)
                    if (hudState.hasSpeedUp) BuffChip("SPEED+", com.example.ui.theme.GeminiBlue)
                    if (hudState.hasAttackMultiplier) BuffChip("2X ATK", GoogleGold)
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
        if (hudState.isPaused && !hudState.isMissionOver) {
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
                        Spacer(modifier = Modifier.height(22.dp))
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
