package com.example.ai

import com.example.data.PlayerProgress
import com.example.game.SectorDefinition
import com.example.game.WebSectors
import com.example.multiplayer.CoopSessionState
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class InGameDialogTrigger {
    STAGE_ENTRY,
    WAVE_ADVANCE,
    LOW_HP,
    BOSS_SPAWN,
    MANUAL_CALLOUT,
    USER_QUESTION
}

data class GeminiChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String
)

data class GeminiMissionBriefing(
    val recommendedSectorId: String,
    val recommendedSectorName: String,
    val primaryThreatCounter: String,
    val recommendedUpgrade: String,
    val tacticalSummary: String,
    val squadTip: String
)

sealed interface GeminiChatUiState {
    data object Idle : GeminiChatUiState
    data object Loading : GeminiChatUiState
    data class Streaming(val partialText: String) : GeminiChatUiState
    data class Success(val text: String) : GeminiChatUiState
    data class Error(val message: String) : GeminiChatUiState
}

sealed interface GeminiBriefingUiState {
    data object Idle : GeminiBriefingUiState
    data object Loading : GeminiBriefingUiState
    data class Success(val briefing: GeminiMissionBriefing) : GeminiBriefingUiState
    data class Error(val message: String) : GeminiBriefingUiState
}

open class GeminiTacticalRepository {

    companion object {
        const val DEFAULT_MODEL_NAME = "gemini-3.8-flash"
        const val FAST_DIALOG_MODEL_NAME = "gemini-3.5-flash-lite"

        private val briefingSchema = Schema.obj(
            properties = mapOf(
                "recommendedSectorId" to Schema.enumeration(
                    values = WebSectors.all.map { it.id },
                    description = "Exact ID of the best unlocked Web Sector to play next"
                ),
                "recommendedSectorName" to Schema.string(
                    description = "Human-readable name of the recommended Web Sector"
                ),
                "primaryThreatCounter" to Schema.string(
                    description = "Specific enemy/boss counterplay tip using Dash, Guard, Shield Bash, or Sword combo"
                ),
                "recommendedUpgrade" to Schema.string(
                    description = "Best Tech Lab upgrade or Stat allocation given current coins and skill points"
                ),
                "tacticalSummary" to Schema.string(
                    description = "Concise 2-sentence battle plan tailored to the player's current build"
                ),
                "squadTip" to Schema.string(
                    description = "Short 1-sentence tip for 4-player Co-Op or solo wave clear"
                )
            ),
            optionalProperties = emptyList()
        )
    }

    private val tacticalSystemInstruction = content {
        text(
            """
            You are Gemini Oracle, the built-in tactical AI strategist inside the 3D action RPG "Gemini Odyssey".
            Keep answers concise, crisp, actionable, and grounded strictly in the game's actual mechanics:
            - Controls & Moves: 3-hit Sword Combo (Auto-target or Swipe), Holdable Shield Guard, Shield Bash (stun + reflect), Dash (invulnerability frames), and Search Nodes (index nodes to unlock Boss).
            - Pickups: Anti-Virus Gun, Ad-Block Bubble, Speed-Up, 2x Attack, Repair +35 HP.
            - Upgrades in Tech Lab: TPU Core (Damage), Context Window (Max HP & Shield), Fiber Optic Speed (Move Speed & Cooldowns), Neural Magnet (Coin & Drop Pull).
            - Sectors & Bosses:
              1) sector_plaza (Searchlight Plaza, 0+ stars, Boss: Pop-Up Titan)
              2) sector_cloud (Cloud Canopy, 3+ stars, Boss: Trojan Wyrm)
              3) sector_darknet (Dark-Net Trench, 7+ stars, Boss: 404 Leviathan)
              4) sector_quantum (Quantum Index, 12+ stars, Boss: Malware Overlord)
            - Co-Op: Up to 4 players online (Room Code) or Local Connected Devices (Wi-Fi LAN), color-coded Blue/Red/Gold/Green.
            Avoid markdown code blocks and keep responses easy to skim on mobile.
            """.trimIndent()
        )
    }

    private val tacticalChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL_NAME,
            systemInstruction = tacticalSystemInstruction
        )
    }

    private val inGameDialogModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_DIALOG_MODEL_NAME,
            generationConfig = generationConfig {
                maxOutputTokens = 120
                temperature = 0.7f
            },
            systemInstruction = tacticalSystemInstruction
        )
    }

    private val briefingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = briefingSchema
            }
        )
    }

    open fun buildPlayerContextSummary(
        progress: PlayerProgress,
        coopSession: CoopSessionState
    ): String {
        val unlockedSectors = WebSectors.all
            .filter { progress.totalStars >= it.unlockRequirementStars }
            .joinToString { "${it.id} (${it.name}, boss: ${it.bossName})" }

        return """
            Player Telemetry:
            - Level: ${progress.level} (XP: ${progress.xp}/${progress.xpForNextLevel})
            - Google Coins: ${progress.googleCoins} G
            - Stars: ${progress.totalStars}/${progress.maxPossibleStars}
            - Skill Points Available: ${progress.skillPoints}
            - Stats: ATK ${progress.attackStat}, DEF ${progress.defenseStat}, STA ${progress.staminaStat}, HP ${progress.healthStat}
            - Tech Lab Upgrades: TPU Core Lv.${progress.tpuCoreLevel}, Context Window Lv.${progress.contextWindowLevel}, Fiber Optic Lv.${progress.fiberOpticLevel}, Neural Magnet Lv.${progress.neuralMagnetLevel}
            - Unlocked Sectors: $unlockedSectors
            - Co-Op Status: ${if (coopSession.isActive) "In ${coopSession.mode.name} squad (${coopSession.playerCount}/4 players, sector ${coopSession.sectorId})" else "Solo"}
        """.trimIndent()
    }

    open fun streamTacticalPrompt(
        prompt: String,
        history: List<GeminiChatMessage>,
        progress: PlayerProgress,
        coopSession: CoopSessionState
    ): Flow<String> {
        val contextSummary = buildPlayerContextSummary(progress, coopSession)
        val chatHistory = mutableListOf<Content>()
        chatHistory += content("user") {
            text("Current Player Telemetry:\n$contextSummary")
        }
        chatHistory += content("model") {
            text("Telemetry synced. Ready to advise on builds, sectors, bosses, and 4P co-op strategy.")
        }
        history.takeLast(8).forEach { msg ->
            chatHistory += content(if (msg.isUser) "user" else "model") {
                text(msg.text)
            }
        }
        val chat = tacticalChatModel.startChat(history = chatHistory)
        return chat.sendMessageStream(prompt)
            .map { chunk -> chunk.text.orEmpty() }
            .flowOn(Dispatchers.IO)
    }

    open suspend fun generateStructuredBriefing(
        progress: PlayerProgress,
        coopSession: CoopSessionState
    ): GeminiMissionBriefing = withContext(Dispatchers.IO) {
        val contextSummary = buildPlayerContextSummary(progress, coopSession)
        val prompt = """
            Analyze this Gemini Odyssey player's live profile and generate a tailored mission briefing.
            Only recommend a sector ID from the player's Unlocked Sectors list.
            $contextSummary
        """.trimIndent()

        val response = briefingModel.generateContent(prompt)
        val jsonString = response.text ?: throw IllegalStateException("Gemini returned an empty briefing.")
        parseBriefingJson(jsonString, progress)
    }

    open suspend fun generateQuickArenaTip(
        sector: SectorDefinition,
        progress: PlayerProgress,
        hpPercent: Int,
        wave: Int,
        isCoop: Boolean
    ): String = withContext(Dispatchers.IO) {
        val prompt = buildInGameDialogPrompt(
            trigger = InGameDialogTrigger.MANUAL_CALLOUT,
            sector = sector,
            progress = progress,
            hpPercent = hpPercent,
            wave = wave,
            nodesIndexed = 0,
            totalNodes = sector.waves,
            bossName = null,
            isCoop = isCoop
        )
        val response = inGameDialogModel.generateContent(prompt)
        response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini returned an empty combat tip.")
    }

    fun buildInGameDialogPrompt(
        trigger: InGameDialogTrigger,
        sector: SectorDefinition,
        progress: PlayerProgress,
        hpPercent: Int,
        wave: Int,
        nodesIndexed: Int,
        totalNodes: Int,
        bossName: String?,
        isCoop: Boolean,
        customQuestion: String? = null
    ): String {
        val situation = when (trigger) {
            InGameDialogTrigger.STAGE_ENTRY ->
                "The hero just entered ${sector.name} (${sector.subtitle}). Give an energetic 1-2 sentence opening tactical callout."
            InGameDialogTrigger.WAVE_ADVANCE ->
                "Wave $wave of ${sector.waves} just began! Give a crisp 1-2 sentence battle callout."
            InGameDialogTrigger.LOW_HP ->
                "WARNING: Hero HP is critical at $hpPercent%! Give an urgent 1-2 sentence survival tip (Shield Guard, Dash i-frames, or Repair pickup)."
            InGameDialogTrigger.BOSS_SPAWN ->
                "ALERT: Sector Boss ${bossName ?: sector.bossName} just spawned! Give a sharp 1-2 sentence boss counterplay dialog."
            InGameDialogTrigger.MANUAL_CALLOUT ->
                "Hero requested live mid-battle comms during Wave $wave/${sector.waves} at $hpPercent% HP."
            InGameDialogTrigger.USER_QUESTION ->
                "Hero asks live in-game: \"${customQuestion.orEmpty().trim()}\""
        }

        return """
            Speak directly to the player in-character as Gemini Oracle, their live tactical AI wingman in combat.
            Keep dialog punchy, encouraging, and easy to glance at mid-fight (strictly 1-2 short sentences, under 35 words).
            Live Battle State:
            - Sector: ${sector.name} (Boss: ${sector.bossName})
            - Mode: ${if (isCoop) "4-Player Co-Op Squad" else "Solo"}
            - Hero Lv.${progress.level}, ATK ${progress.attackStat}, DEF ${progress.defenseStat}, TPU Core Lv.${progress.tpuCoreLevel}
            - Wave: $wave/${sector.waves} | HP: $hpPercent% | Search Nodes Indexed: $nodesIndexed/$totalNodes
            - Boss Active: ${bossName ?: "Not yet"}
            Event: $situation
        """.trimIndent()
    }

    open fun streamInGameDialog(
        trigger: InGameDialogTrigger,
        sector: SectorDefinition,
        progress: PlayerProgress,
        hpPercent: Int,
        wave: Int,
        nodesIndexed: Int,
        totalNodes: Int,
        bossName: String?,
        isCoop: Boolean,
        history: List<GeminiChatMessage>,
        customQuestion: String? = null
    ): Flow<String> {
        val prompt = buildInGameDialogPrompt(
            trigger = trigger,
            sector = sector,
            progress = progress,
            hpPercent = hpPercent,
            wave = wave,
            nodesIndexed = nodesIndexed,
            totalNodes = totalNodes,
            bossName = bossName,
            isCoop = isCoop,
            customQuestion = customQuestion
        )
        val rawFlow = if (history.isEmpty()) {
            inGameDialogModel.generateContentStream(prompt)
        } else {
            val chatHistory = history.takeLast(6).map { msg ->
                content(if (msg.isUser) "user" else "model") { text(msg.text) }
            }
            inGameDialogModel.startChat(history = chatHistory).sendMessageStream(prompt)
        }
        return rawFlow.map { chunk -> chunk.text.orEmpty() }.flowOn(Dispatchers.IO)
    }

    open suspend fun generateInGameDialogUnary(
        trigger: InGameDialogTrigger,
        sector: SectorDefinition,
        progress: PlayerProgress,
        hpPercent: Int,
        wave: Int,
        nodesIndexed: Int,
        totalNodes: Int,
        bossName: String?,
        isCoop: Boolean,
        history: List<GeminiChatMessage>,
        customQuestion: String? = null
    ): String = withContext(Dispatchers.IO) {
        val prompt = buildInGameDialogPrompt(
            trigger = trigger,
            sector = sector,
            progress = progress,
            hpPercent = hpPercent,
            wave = wave,
            nodesIndexed = nodesIndexed,
            totalNodes = totalNodes,
            bossName = bossName,
            isCoop = isCoop,
            customQuestion = customQuestion
        )
        val response = if (history.isEmpty()) {
            inGameDialogModel.generateContent(prompt)
        } else {
            val chatHistory = history.takeLast(6).map { msg ->
                content(if (msg.isUser) "user" else "model") { text(msg.text) }
            }
            inGameDialogModel.startChat(history = chatHistory).sendMessage(prompt)
        }
        response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini returned an empty dialog line.")
    }

    fun parseBriefingJson(
        jsonString: String,
        progress: PlayerProgress
    ): GeminiMissionBriefing {
        val json = JSONObject(jsonString)
        val unlockedIds = WebSectors.all
            .filter { progress.totalStars >= it.unlockRequirementStars }
            .map { it.id }
            .toSet()
        val rawSectorId = json.optString("recommendedSectorId", "sector_plaza")
        val safeSectorId = if (rawSectorId in unlockedIds) rawSectorId else unlockedIds.lastOrNull() ?: "sector_plaza"
        val fallbackSector = WebSectors.all.firstOrNull { it.id == safeSectorId } ?: WebSectors.all.first()

        return GeminiMissionBriefing(
            recommendedSectorId = safeSectorId,
            recommendedSectorName = json.optString("recommendedSectorName", fallbackSector.name)
                .ifBlank { fallbackSector.name },
            primaryThreatCounter = json.getString("primaryThreatCounter"),
            recommendedUpgrade = json.getString("recommendedUpgrade"),
            tacticalSummary = json.getString("tacticalSummary"),
            squadTip = json.getString("squadTip")
        )
    }
}
