package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.HubScreen
import com.example.ui.theme.GeminiOdysseyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val viewModel: MainViewModel = viewModel()
            val progress by viewModel.playerProgress.collectAsStateWithLifecycle()
            val screen by viewModel.currentScreen.collectAsStateWithLifecycle()

            GeminiOdysseyTheme(
                themeMode = progress.themeMode,
                highContrast = progress.highContrast
            ) {
                when (val current = screen) {
                    is AppScreen.Hub -> {
                        HubScreen(
                            progress = progress,
                            onLaunchSector = { sector -> viewModel.launchSector(sector) },
                            onPurchaseUpgrade = { type -> viewModel.purchaseUpgrade(type) },
                            onAllocateSkillPoint = { stat -> viewModel.allocateSkillPoint(stat) },
                            onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
                            onSetHighContrast = { enabled -> viewModel.setHighContrast(enabled) },
                            onSetLargeTouchTargets = { enabled -> viewModel.setLargeTouchTargets(enabled) },
                            onSetReducedMotion = { enabled -> viewModel.setReducedMotion(enabled) },
                            onToggleHandedness = { left -> viewModel.setLeftHanded(left) }
                        )
                    }
                    is AppScreen.Arena -> {
                        val idx = com.example.game.WebSectors.all.indexOfFirst { it.id == current.sector.id }
                        val hasNext = idx in 0 until com.example.game.WebSectors.all.lastIndex
                        ArenaScreen(
                            sector = current.sector,
                            progress = progress,
                            hasNextStage = hasNext,
                            onToggleHandedness = { left -> viewModel.setLeftHanded(left) },
                            onNextStage = { viewModel.launchNextStage(current.sector) },
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
                            onExitToHub = { viewModel.returnToHub() }
                        )
                    }
                }
            }
        }
    }
}
