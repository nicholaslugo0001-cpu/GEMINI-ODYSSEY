package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class CloudUserProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Gemini Knight",
    val googleCoins: Long = 0L,
    val totalCoinsCollected: Long = 0L,
    val level: Long = 1L,
    val xp: Long = 0L,
    val skillPoints: Long = 0L,
    val attackStat: Long = 1L,
    val defenseStat: Long = 1L,
    val staminaStat: Long = 1L,
    val healthStat: Long = 1L,
    val tpuCoreLevel: Long = 1L,
    val contextWindowLevel: Long = 1L,
    val fiberOpticLevel: Long = 1L,
    val neuralMagnetLevel: Long = 1L,
    val sectorStars: Map<String, Long> = emptyMap(),
    val missionsCompleted: Long = 0L,
    val virusesPurged: Long = 0L,
    val adwareBlocked: Long = 0L,
    val hackersOutsmarted: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Gemini Knight",
    val level: Long = 1L,
    val totalStars: Long = 0L,
    val totalCoinsCollected: Long = 0L,
    val missionsCompleted: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudSyncRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun syncProgress(progress: PlayerProgress, displayName: String): Result<Unit> {
        val uid = requireUserId()
        val safeName = displayName.trim().ifEmpty { "Gemini Knight" }.take(64)
        val userDoc = db.collection("users").document(uid)
        val leaderDoc = db.collection("leaderboard").document(uid)

        return try {
            val existingUser = userDoc.get().await()
            val userPayload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "googleCoins" to progress.googleCoins.toLong(),
                "totalCoinsCollected" to progress.totalCoinsCollected.toLong(),
                "level" to progress.level.toLong(),
                "xp" to progress.xp.toLong(),
                "skillPoints" to progress.skillPoints.toLong(),
                "attackStat" to progress.attackStat.toLong(),
                "defenseStat" to progress.defenseStat.toLong(),
                "staminaStat" to progress.staminaStat.toLong(),
                "healthStat" to progress.healthStat.toLong(),
                "tpuCoreLevel" to progress.tpuCoreLevel.toLong(),
                "contextWindowLevel" to progress.contextWindowLevel.toLong(),
                "fiberOpticLevel" to progress.fiberOpticLevel.toLong(),
                "neuralMagnetLevel" to progress.neuralMagnetLevel.toLong(),
                "sectorStars" to progress.sectorStars.mapValues { it.value.toLong() },
                "missionsCompleted" to progress.missionsCompleted.toLong(),
                "virusesPurged" to progress.virusesPurged.toLong(),
                "adwareBlocked" to progress.adwareBlocked.toLong(),
                "hackersOutsmarted" to progress.hackersOutsmarted.toLong(),
                "createdAt" to (existingUser.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userDoc.set(userPayload).await()

            val existingLeader = leaderDoc.get().await()
            val leaderPayload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "level" to progress.level.toLong(),
                "totalStars" to progress.totalStars.toLong(),
                "totalCoinsCollected" to progress.totalCoinsCollected.toLong(),
                "missionsCompleted" to progress.missionsCompleted.toLong(),
                "createdAt" to (existingLeader.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            leaderDoc.set(leaderPayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path)
            Result.failure(e)
        }
    }

    suspend fun fetchCloudProgress(): Result<CloudUserProgress?> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            val model = if (snap.exists()) {
                snap.toObject(CloudUserProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(model)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeLeaderboard(limit: Long = 20L): Flow<List<LeaderboardEntry>> = flow {
        val query = db.collection("leaderboard")
            .orderBy("totalStars", Query.Direction.DESCENDING)
            .limit(limit)
        query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<LeaderboardEntry> { it.totalStars }
                            .thenByDescending { it.totalCoinsCollected }
                    )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && Firebase.auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, "leaderboard")
                }
                throw error
            }
            .collect { emit(it) }
    }
}
