package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AdwareMagenta
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GoogleGold
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.HackerNeonGreen
import com.example.ui.theme.VirusCrimson

data class SectorDefinition(
    val id: String,
    val name: String,
    val subtitle: String,
    val userSearchQuery: String,
    val difficultyLabel: String,
    val waves: Int,
    val bossName: String,
    val primaryColor: Color,
    val accentColor: Color,
    val unlockRequirementStars: Int,
    val coinBonus: Int
)

object WebSectors {
    val all: List<SectorDefinition> = listOf(
        SectorDefinition(
            id = "sector_plaza",
            name = "Searchlight Plaza",
            subtitle = "Surface Web Gateway",
            userSearchQuery = "\"Best artisan coffee nearby without pop-up spam\"",
            difficultyLabel = "TIER I • NORMAL",
            waves = 3,
            bossName = "Pop-Up Titan",
            primaryColor = GeminiCyan,
            accentColor = GoogleGold,
            unlockRequirementStars = 0,
            coinBonus = 40
        ),
        SectorDefinition(
            id = "sector_cloud",
            name = "Cloud Canopy",
            subtitle = "Distributed Data Highways",
            userSearchQuery = "\"Sync family vacation album safely to Cloud Drive\"",
            difficultyLabel = "TIER II • ELEVATED",
            waves = 4,
            bossName = "Trojan Wyrm",
            primaryColor = GeminiBlue,
            accentColor = AdwareMagenta,
            unlockRequirementStars = 3,
            coinBonus = 75
        ),
        SectorDefinition(
            id = "sector_darknet",
            name = "Dark-Net Trench",
            subtitle = "Encrypted Firewall Abyss",
            userSearchQuery = "\"Block zero-day phishing link & recover account\"",
            difficultyLabel = "TIER III • HAZARD",
            waves = 4,
            bossName = "404 Leviathan",
            primaryColor = HackerNeonGreen,
            accentColor = VirusCrimson,
            unlockRequirementStars = 7,
            coinBonus = 110
        ),
        SectorDefinition(
            id = "sector_quantum",
            name = "Quantum Index",
            subtitle = "Core of the Google-verse",
            userSearchQuery = "\"Restore 5-Star Harmony across the entire Web\"",
            difficultyLabel = "TIER IV • OMEGA",
            waves = 5,
            bossName = "Malware Overlord",
            primaryColor = GoogleGold,
            accentColor = GeminiCyan,
            unlockRequirementStars = 12,
            coinBonus = 180
        )
    )
}

enum class EnemyType(
    val displayName: String,
    val color: Color,
    val baseHp: Float,
    val speed: Float,
    val radius: Float,
    val coinReward: Int
) {
    TROJAN_VIRUS("Trojan Imp", VirusCrimson, 50f, 130f, 24f, 6),
    POPUP_ADWARE("Ad-Ware Bot", AdwareMagenta, 60f, 95f, 25f, 8),
    PESKY_HACKER("Hacker Rogue", HackerNeonGreen, 75f, 165f, 23f, 12),
    SECTOR_BOSS("Sector Boss", VirusCrimson, 520f, 105f, 48f, 60)
}

data class PlayerEntity(
    val x: Float = 0f,
    val y: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val facingAngle: Float = -1.57f,
    val hp: Float = 120f,
    val maxHp: Float = 120f,
    val shieldStamina: Float = 100f,
    val maxShieldStamina: Float = 100f,
    val isGuarding: Boolean = false,
    val invulnTimer: Float = 0f,
    val slashCooldown: Float = 0f,
    val slashMaxCooldown: Float = 0.28f,
    val slashActiveTimer: Float = 0f,
    val slashComboStep: Int = 0, // 0, 1, 2 (2 = spin cleave)
    val shieldBashCooldown: Float = 0f,
    val shieldBashMaxCooldown: Float = 3.2f,
    val dashCooldown: Float = 0f,
    val dashMaxCooldown: Float = 2.0f,
    val dashActiveTimer: Float = 0f,
    val hitFlashTimer: Float = 0f,
    val walkPhase: Float = 0f,
    val antiVirusGunTimer: Float = 0f,
    val antiVirusFireCooldown: Float = 0f,
    val adBlockBubbleTimer: Float = 0f,
    val speedUpTimer: Float = 0f,
    val attackMultTimer: Float = 0f
)

enum class ItemUpgradeType(val label: String, val color: Color) {
    ANTI_VIRUS_GUN("ANTI-VIRUS GUN", GoogleGreen),
    AD_BLOCK_BUBBLE("AD-BLOCK BUBBLE", GeminiCyan),
    SPEED_UP("SPEED-UP", GeminiBlue),
    ATTACK_MULTIPLIER("2X ATTACK", GoogleGold),
    HEALTH_PACK("REPAIR +35", VirusCrimson)
}

data class ItemDropEntity(
    val id: Int,
    val type: ItemUpgradeType,
    val x: Float,
    val y: Float,
    val z: Float = 16f,
    val vz: Float = 110f,
    val bobPhase: Float = 0f
)

data class TetrisCell(val gx: Int, val gy: Int)

data class TetrisObstacle(
    val id: Int,
    val worldX: Float,
    val worldY: Float,
    val cellSize: Float = 44f,
    val cells: List<TetrisCell>,
    val color: Color
)

data class EnemyEntity(
    val id: Int,
    val type: EnemyType,
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val hp: Float,
    val maxHp: Float,
    val facingAngle: Float = 0f,
    val attackTimer: Float = 1.2f,
    val dashTimer: Float = 2.0f,
    val hitFlashTimer: Float = 0f,
    val stunTimer: Float = 0f,
    val walkPhase: Float = 0f,
    val wanderAngle: Float = 0f,
    val wanderTimer: Float = 0f
)

data class ProjectileEntity(
    val id: Int,
    val fromPlayer: Boolean,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val radius: Float,
    val color: Color,
    val life: Float = 2.0f
)

data class CoinEntity(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float = 18f,
    val vz: Float = 120f,
    val value: Int = 5,
    val spinPhase: Float = 0f,
    val magnetized: Boolean = false
)

data class SearchNodeEntity(
    val id: Int,
    val x: Float,
    val y: Float,
    val label: String,
    val progress: Float = 0f,
    val indexed: Boolean = false
)

data class FloatingText(
    val id: Int,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val life: Float = 0.85f,
    val maxLife: Float = 0.85f,
    val isCrit: Boolean = false
)

data class ParticleEffect(
    val x: Float,
    val y: Float,
    val z: Float = 12f,
    val vx: Float,
    val vy: Float,
    val vz: Float = 40f,
    val color: Color,
    val radius: Float,
    val life: Float,
    val maxLife: Float
)

data class ShockwaveRing(
    val x: Float,
    val y: Float,
    val radius: Float,
    val maxRadius: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float
)

data class ArenaHudState(
    val sector: SectorDefinition,
    val wave: Int = 1,
    val totalWaves: Int = 3,
    val hp: Float = 120f,
    val maxHp: Float = 120f,
    val shieldStamina: Float = 100f,
    val maxShieldStamina: Float = 100f,
    val isGuarding: Boolean = false,
    val coinsCollected: Int = 0,
    val nodesIndexed: Int = 0,
    val totalNodes: Int = 2,
    val bossHp: Float? = null,
    val bossMaxHp: Float? = null,
    val bossName: String? = null,
    val slashReadyFraction: Float = 1f,
    val shieldBashReadyFraction: Float = 1f,
    val dashReadyFraction: Float = 1f,
    val isPaused: Boolean = false,
    val isMissionOver: Boolean = false,
    val isVictory: Boolean = false,
    val starsEarned: Int = 0,
    val virusesKilled: Int = 0,
    val adwareKilled: Int = 0,
    val hackersKilled: Int = 0,
    val xpEarnedThisStage: Int = 0,
    val hasAntiVirusGun: Boolean = false,
    val hasAdBlockBubble: Boolean = false,
    val hasSpeedUp: Boolean = false,
    val hasAttackMultiplier: Boolean = false,
    val bannerMessage: String? = null
)

enum class HapticEvent {
    LIGHT_ATTACK,
    HEAVY_ATTACK,
    DAMAGE_TAKEN,
    SHIELD_BLOCK,
    ITEM_COLLECTED,
    DASH
}
