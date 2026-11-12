package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gemini_odyssey_save")

enum class UpgradeType(
    val title: String,
    val subtitle: String,
    val baseCost: Int,
    val maxLevel: Int = 10
) {
    TPU_CORE("TPU Blade", "Boosts Sword Slash & Spin Cleave damage", 40),
    CONTEXT_WINDOW("Aegis Core", "Increases Max HP & Shield Guard strength", 35),
    FIBER_OPTICS("Fiber Boots", "Increases move speed & cuts Dash cooldown", 45),
    NEURAL_MAGNET("Neural Magnet", "Expands Coin magnet & Critical Slash rate", 50);

    fun costForLevel(currentLevel: Int): Int = baseCost + (currentLevel - 1) * 35
}

enum class StatType(val title: String, val description: String) {
    ATTACK("Attack", "+12% Sword & Anti-Virus damage per point"),
    DEFENSE("Defense", "+6% Damage Reduction & Shield block power"),
    STAMINA("Stamina", "+15 Max Shield Stamina & faster cooldowns"),
    HEALTH("Health", "+22 Max HP per point")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class PlayerProgress(
    val googleCoins: Int = 0,
    val totalCoinsCollected: Int = 0,
    val tpuCoreLevel: Int = 1,
    val contextWindowLevel: Int = 1,
    val fiberOpticLevel: Int = 1,
    val neuralMagnetLevel: Int = 1,
    val sectorStars: Map<String, Int> = emptyMap(),
    val missionsCompleted: Int = 0,
    val virusesPurged: Int = 0,
    val adwareBlocked: Int = 0,
    val hackersOutsmarted: Int = 0,
    val isLeftHanded: Boolean = false,
    val xp: Int = 0,
    val level: Int = 1,
    val skillPoints: Int = 0,
    val attackStat: Int = 1,
    val defenseStat: Int = 1,
    val staminaStat: Int = 1,
    val healthStat: Int = 1,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val highContrast: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val reducedMotion: Boolean = false
) {
    val xpForNextLevel: Int
        get() = 80 + (level - 1) * 45
    val totalStars: Int
        get() = sectorStars.values.sum()

    val maxPossibleStars: Int
        get() = 20 // 4 sectors * 5 stars

    val averageUserRating: Float
        get() {
            val completed = sectorStars.values.filter { it > 0 }
            return if (completed.isEmpty()) 0.0f else completed.average().toFloat()
        }

    fun upgradeLevel(type: UpgradeType): Int = when (type) {
        UpgradeType.TPU_CORE -> tpuCoreLevel
        UpgradeType.CONTEXT_WINDOW -> contextWindowLevel
        UpgradeType.FIBER_OPTICS -> fiberOpticLevel
        UpgradeType.NEURAL_MAGNET -> neuralMagnetLevel
    }
}

class PlayerProgressRepository(private val context: Context) {

    private object Keys {
        val GOOGLE_COINS = intPreferencesKey("google_coins")
        val TOTAL_COINS = intPreferencesKey("total_coins")
        val TPU_CORE = intPreferencesKey("tpu_core_level")
        val CONTEXT_WINDOW = intPreferencesKey("context_window_level")
        val FIBER_OPTICS = intPreferencesKey("fiber_optics_level")
        val NEURAL_MAGNET = intPreferencesKey("neural_magnet_level")
        val MISSIONS_COMPLETED = intPreferencesKey("missions_completed")
        val VIRUSES_PURGED = intPreferencesKey("viruses_purged")
        val ADWARE_BLOCKED = intPreferencesKey("adware_blocked")
        val HACKERS_OUTSMARTED = intPreferencesKey("hackers_outsmarted")
        val IS_LEFT_HANDED = booleanPreferencesKey("is_left_handed")
        val XP = intPreferencesKey("player_xp")
        val LEVEL = intPreferencesKey("player_level")
        val SKILL_POINTS = intPreferencesKey("player_skill_points")
        val STAT_ATTACK = intPreferencesKey("stat_attack")
        val STAT_DEFENSE = intPreferencesKey("stat_defense")
        val STAT_STAMINA = intPreferencesKey("stat_stamina")
        val STAT_HEALTH = intPreferencesKey("stat_health")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val LARGE_TOUCH = booleanPreferencesKey("large_touch_targets")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")

        fun sectorStarKey(sectorId: String) = intPreferencesKey("sector_stars_$sectorId")
    }

    private val sectorIds = listOf("sector_plaza", "sector_cloud", "sector_darknet", "sector_quantum")

    val progressFlow: Flow<PlayerProgress> = context.dataStore.data.map { prefs ->
        val starsMap = sectorIds.associateWith { id -> prefs[Keys.sectorStarKey(id)] ?: 0 }
        PlayerProgress(
            googleCoins = prefs[Keys.GOOGLE_COINS] ?: 50,
            totalCoinsCollected = prefs[Keys.TOTAL_COINS] ?: 50,
            tpuCoreLevel = prefs[Keys.TPU_CORE] ?: 1,
            contextWindowLevel = prefs[Keys.CONTEXT_WINDOW] ?: 1,
            fiberOpticLevel = prefs[Keys.FIBER_OPTICS] ?: 1,
            neuralMagnetLevel = prefs[Keys.NEURAL_MAGNET] ?: 1,
            sectorStars = starsMap,
            missionsCompleted = prefs[Keys.MISSIONS_COMPLETED] ?: 0,
            virusesPurged = prefs[Keys.VIRUSES_PURGED] ?: 0,
            adwareBlocked = prefs[Keys.ADWARE_BLOCKED] ?: 0,
            hackersOutsmarted = prefs[Keys.HACKERS_OUTSMARTED] ?: 0,
            isLeftHanded = prefs[Keys.IS_LEFT_HANDED] ?: false,
            xp = prefs[Keys.XP] ?: 0,
            level = prefs[Keys.LEVEL] ?: 1,
            skillPoints = prefs[Keys.SKILL_POINTS] ?: 0,
            attackStat = prefs[Keys.STAT_ATTACK] ?: 1,
            defenseStat = prefs[Keys.STAT_DEFENSE] ?: 1,
            staminaStat = prefs[Keys.STAT_STAMINA] ?: 1,
            healthStat = prefs[Keys.STAT_HEALTH] ?: 1,
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
            largeTouchTargets = prefs[Keys.LARGE_TOUCH] ?: false,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
    }

    suspend fun setLargeTouchTargets(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LARGE_TOUCH] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun addXp(earnedXp: Int) {
        if (earnedXp <= 0) return
        context.dataStore.edit { prefs ->
            var currentXp = (prefs[Keys.XP] ?: 0) + earnedXp
            var currentLevel = prefs[Keys.LEVEL] ?: 1
            var currentPoints = prefs[Keys.SKILL_POINTS] ?: 0
            while (currentXp >= (80 + (currentLevel - 1) * 45)) {
                currentXp -= (80 + (currentLevel - 1) * 45)
                currentLevel++
                currentPoints++
            }
            prefs[Keys.XP] = currentXp
            prefs[Keys.LEVEL] = currentLevel
            prefs[Keys.SKILL_POINTS] = currentPoints
        }
    }

    suspend fun allocateSkillPoint(stat: StatType): Boolean {
        var upgraded = false
        context.dataStore.edit { prefs ->
            val pts = prefs[Keys.SKILL_POINTS] ?: 0
            if (pts > 0) {
                val key = when (stat) {
                    StatType.ATTACK -> Keys.STAT_ATTACK
                    StatType.DEFENSE -> Keys.STAT_DEFENSE
                    StatType.STAMINA -> Keys.STAT_STAMINA
                    StatType.HEALTH -> Keys.STAT_HEALTH
                }
                prefs[key] = (prefs[key] ?: 1) + 1
                prefs[Keys.SKILL_POINTS] = pts - 1
                upgraded = true
            }
        }
        return upgraded
    }

    suspend fun setLeftHanded(leftHanded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LEFT_HANDED] = leftHanded
        }
    }

    suspend fun recordMissionResult(
        sectorId: String,
        coinsEarned: Int,
        starsEarned: Int,
        victory: Boolean,
        virusesKilled: Int,
        adwareKilled: Int,
        hackersKilled: Int
    ) {
        context.dataStore.edit { prefs ->
            val currentCoins = prefs[Keys.GOOGLE_COINS] ?: 50
            val totalCoins = prefs[Keys.TOTAL_COINS] ?: 50
            prefs[Keys.GOOGLE_COINS] = currentCoins + coinsEarned
            prefs[Keys.TOTAL_COINS] = totalCoins + coinsEarned
            prefs[Keys.VIRUSES_PURGED] = (prefs[Keys.VIRUSES_PURGED] ?: 0) + virusesKilled
            prefs[Keys.ADWARE_BLOCKED] = (prefs[Keys.ADWARE_BLOCKED] ?: 0) + adwareKilled
            prefs[Keys.HACKERS_OUTSMARTED] = (prefs[Keys.HACKERS_OUTSMARTED] ?: 0) + hackersKilled

            if (victory) {
                prefs[Keys.MISSIONS_COMPLETED] = (prefs[Keys.MISSIONS_COMPLETED] ?: 0) + 1
                val key = Keys.sectorStarKey(sectorId)
                val prevStars = prefs[key] ?: 0
                if (starsEarned > prevStars) {
                    prefs[key] = starsEarned
                }
            }
        }
    }

    suspend fun purchaseUpgrade(type: UpgradeType): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val key = when (type) {
                UpgradeType.TPU_CORE -> Keys.TPU_CORE
                UpgradeType.CONTEXT_WINDOW -> Keys.CONTEXT_WINDOW
                UpgradeType.FIBER_OPTICS -> Keys.FIBER_OPTICS
                UpgradeType.NEURAL_MAGNET -> Keys.NEURAL_MAGNET
            }
            val currentLevel = prefs[key] ?: 1
            val coins = prefs[Keys.GOOGLE_COINS] ?: 50
            val cost = type.costForLevel(currentLevel)
            if (currentLevel < type.maxLevel && coins >= cost) {
                prefs[Keys.GOOGLE_COINS] = coins - cost
                prefs[key] = currentLevel + 1
                success = true
            }
        }
        return success
    }
}
