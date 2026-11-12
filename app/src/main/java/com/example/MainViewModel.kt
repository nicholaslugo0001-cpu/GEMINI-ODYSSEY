package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PlayerProgress
import com.example.data.PlayerProgressRepository
import com.example.data.StatType
import com.example.data.ThemeMode
import com.example.data.UpgradeType
import com.example.game.SectorDefinition
import com.example.game.WebSectors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data object Hub : AppScreen
    data class Arena(val sector: SectorDefinition) : AppScreen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlayerProgressRepository(application.applicationContext)

    val playerProgress: StateFlow<PlayerProgress> = repository.progressFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerProgress()
    )

    // Open directly into Stage 1 of the 3D interactive world
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Arena(WebSectors.all.first()))
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun launchSector(sector: SectorDefinition) {
        _currentScreen.value = AppScreen.Arena(sector)
    }

    fun launchNextStage(currentSector: SectorDefinition) {
        val idx = WebSectors.all.indexOfFirst { it.id == currentSector.id }
        if (idx in 0 until WebSectors.all.lastIndex) {
            _currentScreen.value = AppScreen.Arena(WebSectors.all[idx + 1])
        } else {
            _currentScreen.value = AppScreen.Hub
        }
    }

    fun returnToHub() {
        _currentScreen.value = AppScreen.Hub
    }

    fun setLeftHanded(leftHanded: Boolean) {
        viewModelScope.launch {
            repository.setLeftHanded(leftHanded)
        }
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

    fun purchaseUpgrade(type: UpgradeType) {
        viewModelScope.launch {
            repository.purchaseUpgrade(type)
        }
    }

    fun allocateSkillPoint(stat: StatType) {
        viewModelScope.launch {
            repository.allocateSkillPoint(stat)
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
        }
    }
}
