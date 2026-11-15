package com.example.multiplayer

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@IgnoreExtraProperties
data class FirestoreCoopRoom(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomCode: String = "",
    val hostUid: String = "",
    val sectorId: String = "sector_plaza",
    val status: String = "waiting",
    val memberUids: List<String> = emptyList(),
    val wave: Long = 1L,
    val nodesIndexed: Long = 0L,
    val bossHp: Double = 0.0,
    val bossMaxHp: Double = 0.0,
    val enemySnapshots: List<Map<String, Any>> = emptyList(),
    val isVictory: Boolean = false,
    val starsEarned: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreCoopPlayer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomId: String = "",
    val displayName: String = "Gemini Knight",
    val colorIndex: Long = 0L,
    val ready: Boolean = false,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val facingAngle: Double = -1.57,
    val hp: Double = 130.0,
    val maxHp: Double = 130.0,
    val isGuarding: Boolean = false,
    val isSlashing: Boolean = false,
    val slashComboStep: Long = 0L,
    val walkPhase: Double = 0.0,
    val hitSeq: Long = 0L,
    val lastHitEnemyId: Long = -1L,
    val lastHitDamage: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class OnlineMultiplayerRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun currentUid(): String? = Firebase.auth.currentUser?.uid

    private fun generateRoomCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) { append(alphabet[Random.nextInt(alphabet.length)]) }
        }
    }

    suspend fun createRoom(
        sectorId: String,
        displayName: String,
        maxHp: Float
    ): Result<String> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Sign in with Google to host an online room."))
        val roomCode = generateRoomCode()
        val safeName = displayName.trim().ifEmpty { "Gemini Knight" }.take(64)
        val roomRef = db.collection("rooms").document(roomCode)
        val playerRef = roomRef.collection("players").document(uid)

        return try {
            val roomPayload = mapOf(
                "roomId" to roomCode,
                "roomCode" to roomCode,
                "hostUid" to uid,
                "sectorId" to sectorId,
                "status" to CoopRoomStatus.WAITING.wire,
                "memberUids" to listOf(uid),
                "wave" to 1L,
                "nodesIndexed" to 0L,
                "bossHp" to 0.0,
                "bossMaxHp" to 0.0,
                "enemySnapshots" to emptyList<Map<String, Any>>(),
                "isVictory" to false,
                "starsEarned" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            roomRef.set(roomPayload).await()

            val playerPayload = initialPlayerPayload(
                uid = uid,
                roomId = roomCode,
                displayName = safeName,
                colorIndex = 0,
                maxHp = maxHp,
                ready = true
            )
            playerRef.set(playerPayload).await()
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(
        rawCode: String,
        displayName: String,
        maxHp: Float
    ): Result<String> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Sign in with Google to join an online room."))
        val roomCode = rawCode.trim().uppercase()
        if (!roomCode.matches(Regex("^[a-zA-Z0-9_\\-]{4,16}$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid 6-character room code."))
        }
        val safeName = displayName.trim().ifEmpty { "Gemini Knight" }.take(64)
        val roomRef = db.collection("rooms").document(roomCode)
        val playerRef = roomRef.collection("players").document(uid)

        return try {
            val snap = roomRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("Room $roomCode not found."))
            }
            val room = snap.toObject(FirestoreCoopRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Unable to read room $roomCode."))

            val alreadyMember = uid in room.memberUids
            if (!alreadyMember) {
                if (room.status != CoopRoomStatus.WAITING.wire) {
                    return Result.failure(IllegalStateException("Room $roomCode is already in progress."))
                }
                if (room.memberUids.size >= 4) {
                    return Result.failure(IllegalStateException("Room $roomCode is full (4/4 players)."))
                }
                val updatedMembers = room.memberUids + uid
                roomRef.update(
                    mapOf(
                        "memberUids" to updatedMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val colorIdx = (room.memberUids.indexOf(uid).takeIf { it >= 0 } ?: room.memberUids.size)
                .coerceIn(0, 3)

            val existingPlayer = playerRef.get().await()
            if (!existingPlayer.exists()) {
                playerRef.set(
                    initialPlayerPayload(
                        uid = uid,
                        roomId = roomCode,
                        displayName = safeName,
                        colorIndex = colorIdx,
                        maxHp = maxHp,
                        ready = false
                    )
                ).await()
            }
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(roomCode: String) {
        val uid = currentUid() ?: return
        val roomRef = db.collection("rooms").document(roomCode)
        val playerRef = roomRef.collection("players").document(uid)
        try {
            val snap = roomRef.get().await()
            if (!snap.exists()) return
            val room = snap.toObject(FirestoreCoopRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return
            if (room.hostUid == uid) {
                if (room.status != CoopRoomStatus.FINISHED.wire) {
                    roomRef.update(
                        mapOf(
                            "status" to CoopRoomStatus.FINISHED.wire,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else if (uid in room.memberUids && room.status != CoopRoomStatus.FINISHED.wire) {
                roomRef.update(
                    mapOf(
                        "memberUids" to room.memberUids.filterNot { it == uid },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                playerRef.delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
        }
    }

    suspend fun setReady(roomCode: String, ready: Boolean) {
        val uid = currentUid() ?: return
        val playerRef = db.collection("rooms").document(roomCode).collection("players").document(uid)
        try {
            playerRef.update(
                mapOf(
                    "ready" to ready,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, playerRef.path)
        }
    }

    suspend fun selectSector(roomCode: String, sectorId: String) {
        val roomRef = db.collection("rooms").document(roomCode)
        try {
            roomRef.update(
                mapOf(
                    "sectorId" to sectorId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
        }
    }

    suspend fun startGame(roomCode: String): Result<Unit> {
        val roomRef = db.collection("rooms").document(roomCode)
        return try {
            roomRef.update(
                mapOf(
                    "status" to CoopRoomStatus.PLAYING.wire,
                    "wave" to 1L,
                    "nodesIndexed" to 0L,
                    "bossHp" to 0.0,
                    "bossMaxHp" to 0.0,
                    "enemySnapshots" to emptyList<Map<String, Any>>(),
                    "isVictory" to false,
                    "starsEarned" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            Result.failure(e)
        }
    }

    suspend fun publishLocalPlayerState(
        roomCode: String,
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
        val uid = currentUid() ?: return
        val playerRef = db.collection("rooms").document(roomCode).collection("players").document(uid)
        try {
            playerRef.update(
                mapOf(
                    "x" to x.toDouble().coerceIn(-900.0, 900.0),
                    "y" to y.toDouble().coerceIn(-900.0, 900.0),
                    "facingAngle" to facingAngle.toDouble().coerceIn(-9.0, 9.0),
                    "hp" to hp.toDouble().coerceIn(0.0, 5000.0),
                    "maxHp" to maxHp.toDouble().coerceIn(1.0, 5000.0),
                    "isGuarding" to isGuarding,
                    "isSlashing" to isSlashing,
                    "slashComboStep" to slashComboStep.coerceIn(0, 2).toLong(),
                    "walkPhase" to walkPhase.toDouble().coerceIn(0.0, 999999.0),
                    "hitSeq" to hitSeq.coerceAtLeast(0L),
                    "lastHitEnemyId" to lastHitEnemyId.coerceAtLeast(-1).toLong(),
                    "lastHitDamage" to lastHitDamage.toDouble().coerceIn(0.0, 5000.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Transient sync errors during rapid combat ticks should not crash the frame loop
        }
    }

    suspend fun publishHostWorldState(
        roomCode: String,
        snapshot: CoopWorldSnapshot,
        status: CoopRoomStatus
    ) {
        val roomRef = db.collection("rooms").document(roomCode)
        val encodedEnemies = snapshot.enemies.take(25).map { enemy ->
            mapOf(
                "id" to enemy.id.toLong(),
                "type" to enemy.typeName,
                "x" to enemy.x.toDouble(),
                "y" to enemy.y.toDouble(),
                "hp" to enemy.hp.toDouble(),
                "maxHp" to enemy.maxHp.toDouble(),
                "facing" to enemy.facingAngle.toDouble(),
                "stun" to enemy.stunTimer.toDouble()
            )
        }
        try {
            roomRef.update(
                mapOf(
                    "status" to status.wire,
                    "wave" to snapshot.wave.coerceIn(1, 20).toLong(),
                    "nodesIndexed" to snapshot.nodesIndexed.coerceIn(0, 20).toLong(),
                    "bossHp" to (snapshot.bossHp?.toDouble() ?: 0.0).coerceIn(0.0, 50000.0),
                    "bossMaxHp" to (snapshot.bossMaxHp?.toDouble() ?: 0.0).coerceIn(0.0, 50000.0),
                    "enemySnapshots" to encodedEnemies,
                    "isVictory" to snapshot.isVictory,
                    "starsEarned" to snapshot.starsEarned.coerceIn(0, 5).toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Ignore transient frame-sync drops
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSession(roomCode: String): Flow<CoopSessionState> {
        val uid = currentUid().orEmpty()
        val roomRef = db.collection("rooms").document(roomCode)

        return roomRef.snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(FirestoreCoopRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .flatMapLatest { room ->
                if (room == null) {
                    flowOf(
                        CoopSessionState(
                            isActive = false,
                            mode = CoopMode.ONLINE,
                            roomCode = roomCode,
                            localUid = uid,
                            errorMessage = "Room closed by host."
                        )
                    )
                } else {
                    val memberFlows = room.memberUids.take(4).map { memberUid ->
                        roomRef.collection("players").document(memberUid).snapshots()
                            .map { pSnap ->
                                pSnap.toObject(
                                    FirestoreCoopPlayer::class.java,
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )
                            }
                            .catch { emit(null) }
                    }
                    if (memberFlows.isEmpty()) {
                        flowOf(mapRoomToSession(room, emptyList(), uid))
                    } else {
                        combine(memberFlows) { playersArray ->
                            mapRoomToSession(room, playersArray.filterNotNull(), uid)
                        }
                    }
                }
            }
            .catch { error ->
                val fsEx = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsEx == null && error is CancellationException) throw error
                if (fsEx != null && currentUid() != null) {
                    handleFirestoreError(fsEx, OperationType.GET, roomRef.path)
                }
                emit(
                    CoopSessionState(
                        isActive = false,
                        mode = CoopMode.ONLINE,
                        roomCode = roomCode,
                        localUid = uid,
                        errorMessage = fsEx?.message ?: error.message ?: "Connection lost"
                    )
                )
            }
    }

    private fun mapRoomToSession(
        room: FirestoreCoopRoom,
        players: List<FirestoreCoopPlayer>,
        localUid: String
    ): CoopSessionState {
        val byUid = players.associateBy { it.userId }
        val members = room.memberUids.mapIndexed { idx, memberUid ->
            val p = byUid[memberUid]
            CoopMember(
                uid = memberUid,
                displayName = p?.displayName ?: "Knight ${idx + 1}",
                colorIndex = (p?.colorIndex?.toInt() ?: idx).coerceIn(0, 3),
                ready = p?.ready ?: false,
                isHost = memberUid == room.hostUid
            )
        }
        val remotes = players
            .filter { it.userId != localUid }
            .map { p ->
                RemotePlayerState(
                    uid = p.userId,
                    displayName = p.displayName,
                    colorIndex = p.colorIndex.toInt().coerceIn(0, 3),
                    x = p.x.toFloat(),
                    y = p.y.toFloat(),
                    facingAngle = p.facingAngle.toFloat(),
                    hp = p.hp.toFloat(),
                    maxHp = p.maxHp.toFloat(),
                    isGuarding = p.isGuarding,
                    isSlashing = p.isSlashing,
                    slashComboStep = p.slashComboStep.toInt(),
                    walkPhase = p.walkPhase.toFloat(),
                    isAlive = p.hp > 0.0,
                    hitSeq = p.hitSeq,
                    lastHitEnemyId = p.lastHitEnemyId.toInt(),
                    lastHitDamage = p.lastHitDamage.toFloat()
                )
            }

        val enemies = room.enemySnapshots.mapNotNull { m ->
            val id = (m["id"] as? Number)?.toInt() ?: return@mapNotNull null
            val typeName = m["type"] as? String ?: return@mapNotNull null
            CoopEnemySnapshot(
                id = id,
                typeName = typeName,
                x = (m["x"] as? Number)?.toFloat() ?: 0f,
                y = (m["y"] as? Number)?.toFloat() ?: 0f,
                hp = (m["hp"] as? Number)?.toFloat() ?: 50f,
                maxHp = (m["maxHp"] as? Number)?.toFloat() ?: 50f,
                facingAngle = (m["facing"] as? Number)?.toFloat() ?: 0f,
                stunTimer = (m["stun"] as? Number)?.toFloat() ?: 0f
            )
        }
        val status = CoopRoomStatus.fromWire(room.status)
        val snapshot = CoopWorldSnapshot(
            wave = room.wave.toInt(),
            totalWaves = 5,
            nodesIndexed = room.nodesIndexed.toInt(),
            totalNodes = 4,
            bossHp = if (room.bossMaxHp > 0.0) room.bossHp.toFloat() else null,
            bossMaxHp = if (room.bossMaxHp > 0.0) room.bossMaxHp.toFloat() else null,
            isMissionOver = status == CoopRoomStatus.FINISHED,
            isVictory = room.isVictory,
            starsEarned = room.starsEarned.toInt(),
            enemies = enemies
        )

        return CoopSessionState(
            isActive = true,
            mode = CoopMode.ONLINE,
            roomCode = room.roomCode,
            hostUid = room.hostUid,
            localUid = localUid,
            isHost = room.hostUid == localUid,
            sectorId = room.sectorId,
            status = status,
            members = members,
            remotePlayers = remotes,
            hostWorldSnapshot = snapshot
        )
    }

    private fun initialPlayerPayload(
        uid: String,
        roomId: String,
        displayName: String,
        colorIndex: Int,
        maxHp: Float,
        ready: Boolean
    ): Map<String, Any> = mapOf(
        "userId" to uid,
        "roomId" to roomId,
        "displayName" to displayName,
        "colorIndex" to colorIndex.coerceIn(0, 3).toLong(),
        "ready" to ready,
        "x" to 0.0,
        "y" to 0.0,
        "facingAngle" to -1.57,
        "hp" to maxHp.toDouble().coerceIn(1.0, 5000.0),
        "maxHp" to maxHp.toDouble().coerceIn(1.0, 5000.0),
        "isGuarding" to false,
        "isSlashing" to false,
        "slashComboStep" to 0L,
        "walkPhase" to 0.0,
        "hitSeq" to 0L,
        "lastHitEnemyId" to -1L,
        "lastHitDamage" to 0.0,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
