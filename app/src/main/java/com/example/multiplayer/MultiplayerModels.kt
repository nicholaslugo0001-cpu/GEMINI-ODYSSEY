package com.example.multiplayer

enum class CoopMode {
    ONLINE,
    LOCAL_LAN
}

enum class CoopRoomStatus(val wire: String) {
    WAITING("waiting"),
    PLAYING("playing"),
    FINISHED("finished");

    companion object {
        fun fromWire(value: String): CoopRoomStatus =
            entries.firstOrNull { it.wire == value } ?: WAITING
    }
}

data class CoopMember(
    val uid: String,
    val displayName: String,
    val colorIndex: Int,
    val ready: Boolean = false,
    val isHost: Boolean = false
)

data class RemotePlayerState(
    val uid: String,
    val displayName: String,
    val colorIndex: Int,
    val x: Float,
    val y: Float,
    val facingAngle: Float,
    val hp: Float,
    val maxHp: Float,
    val isGuarding: Boolean = false,
    val isSlashing: Boolean = false,
    val slashComboStep: Int = 0,
    val walkPhase: Float = 0f,
    val isAlive: Boolean = true,
    val hitSeq: Long = 0L,
    val lastHitEnemyId: Int = -1,
    val lastHitDamage: Float = 0f
)

data class CoopEnemySnapshot(
    val id: Int,
    val typeName: String,
    val x: Float,
    val y: Float,
    val hp: Float,
    val maxHp: Float,
    val facingAngle: Float,
    val stunTimer: Float = 0f
)

data class CoopWorldSnapshot(
    val wave: Int,
    val totalWaves: Int,
    val nodesIndexed: Int,
    val totalNodes: Int,
    val bossHp: Float? = null,
    val bossMaxHp: Float? = null,
    val isMissionOver: Boolean = false,
    val isVictory: Boolean = false,
    val starsEarned: Int = 0,
    val enemies: List<CoopEnemySnapshot> = emptyList()
)

data class PendingEnemyHit(
    val hitSeq: Long,
    val enemyId: Int,
    val damage: Float
)

data class DiscoveredLanHost(
    val serviceName: String,
    val displayName: String,
    val roomCode: String,
    val hostAddress: String,
    val port: Int
)

data class CoopSessionState(
    val isActive: Boolean = false,
    val mode: CoopMode = CoopMode.ONLINE,
    val roomCode: String = "",
    val hostUid: String = "",
    val localUid: String = "",
    val isHost: Boolean = false,
    val sectorId: String = "sector_plaza",
    val status: CoopRoomStatus = CoopRoomStatus.WAITING,
    val members: List<CoopMember> = emptyList(),
    val remotePlayers: List<RemotePlayerState> = emptyList(),
    val hostWorldSnapshot: CoopWorldSnapshot? = null,
    val localHostEndpoint: String? = null,
    val errorMessage: String? = null,
    val isConnecting: Boolean = false
) {
    val playerCount: Int get() = members.size.coerceAtMost(4)
    val isFull: Boolean get() = members.size >= 4
}
