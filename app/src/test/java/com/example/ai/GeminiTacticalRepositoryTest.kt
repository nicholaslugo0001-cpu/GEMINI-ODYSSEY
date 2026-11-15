package com.example.ai

import com.example.data.PlayerProgress
import com.example.game.WebSectors
import com.example.multiplayer.CoopMode
import com.example.multiplayer.CoopSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiTacticalRepositoryTest {

    @Test
    fun parseBriefingJson_validPayload_parsesFieldsAndClampsLockedSector() {
        val repo = GeminiTacticalRepository()
        val beginnerProgress = PlayerProgress(googleCoins = 45, level = 1)

        // Even if model recommends locked sector_quantum, parser safely clamps to unlocked sector_plaza
        val rawJson = """
            {
              "recommendedSectorId": "sector_quantum",
              "recommendedSectorName": "Searchlight Plaza",
              "primaryThreatCounter": "Use Shield Bash to stun Ad-Ware Bots and reflect projectiles.",
              "recommendedUpgrade": "Upgrade TPU Core Lv.2 to increase sword cleave damage.",
              "tacticalSummary": "Index Search Nodes rapidly in Searchlight Plaza before engaging Pop-Up Titan.",
              "squadTip": "Stay spread out to cover multiple Search Nodes in co-op."
            }
        """.trimIndent()

        val parsed = repo.parseBriefingJson(rawJson, beginnerProgress)
        assertEquals("sector_plaza", parsed.recommendedSectorId)
        assertEquals("Searchlight Plaza", parsed.recommendedSectorName)
        assertTrue(parsed.primaryThreatCounter.contains("Shield Bash"))
        assertTrue(parsed.recommendedUpgrade.contains("TPU Core"))
    }

    @Test
    fun buildPlayerContextSummary_includesTelemetryAndCoopSquad() {
        val repo = GeminiTacticalRepository()
        val progress = PlayerProgress(
            level = 4,
            googleCoins = 220,
            sectorStars = mapOf("sector_plaza" to 3, "sector_cloud" to 3),
            tpuCoreLevel = 3,
            contextWindowLevel = 2
        )
        val coop = CoopSessionState(
            isActive = true,
            mode = CoopMode.ONLINE,
            roomCode = "GM4X9K",
            sectorId = "sector_cloud",
            members = emptyList()
        )

        val summary = repo.buildPlayerContextSummary(progress, coop)
        assertTrue(summary.contains("Level: 4"))
        assertTrue(summary.contains("220 G"))
        assertTrue(summary.contains("TPU Core Lv.3"))
        assertTrue(summary.contains("sector_cloud"))
    }

    @Test
    fun buildInGameDialogPrompt_bossSpawn_includesBossNameAndLiveTelemetry() {
        val repo = GeminiTacticalRepository()
        val sector = WebSectors.all.first()
        val progress = PlayerProgress(level = 2, attackStat = 14, defenseStat = 11, tpuCoreLevel = 2)

        val prompt = repo.buildInGameDialogPrompt(
            trigger = InGameDialogTrigger.BOSS_SPAWN,
            sector = sector,
            progress = progress,
            hpPercent = 82,
            wave = 3,
            nodesIndexed = 3,
            totalNodes = 3,
            bossName = "Pop-Up Titan",
            isCoop = true
        )

        assertTrue(prompt.contains("Pop-Up Titan"))
        assertTrue(prompt.contains("Searchlight Plaza"))
        assertTrue(prompt.contains("4-Player Co-Op Squad"))
        assertTrue(prompt.contains("HP: 82%"))
        assertTrue(prompt.contains("Wave: 3/3"))
    }

    @Test
    fun buildInGameDialogPrompt_lowHp_includesCriticalHpWarning() {
        val repo = GeminiTacticalRepository()
        val sector = WebSectors.all.first()
        val progress = PlayerProgress(level = 1)

        val prompt = repo.buildInGameDialogPrompt(
            trigger = InGameDialogTrigger.LOW_HP,
            sector = sector,
            progress = progress,
            hpPercent = 24,
            wave = 2,
            nodesIndexed = 1,
            totalNodes = 3,
            bossName = null,
            isCoop = false
        )

        assertTrue(prompt.contains("24%"))
        assertTrue(prompt.contains("Solo"))
        assertTrue(prompt.contains("Search Nodes Indexed: 1/3"))
    }

    @Test
    fun buildInGameDialogPrompt_userQuestion_embedsCustomQuestion() {
        val repo = GeminiTacticalRepository()
        val sector = WebSectors.all.first()
        val progress = PlayerProgress(level = 3)

        val prompt = repo.buildInGameDialogPrompt(
            trigger = InGameDialogTrigger.USER_QUESTION,
            sector = sector,
            progress = progress,
            hpPercent = 65,
            wave = 2,
            nodesIndexed = 2,
            totalNodes = 3,
            bossName = null,
            isCoop = false,
            customQuestion = "Should I dash through the laser sweep?"
        )

        assertTrue(prompt.contains("Should I dash through the laser sweep?"))
        assertTrue(prompt.contains("Hero asks live in-game"))
    }
}
