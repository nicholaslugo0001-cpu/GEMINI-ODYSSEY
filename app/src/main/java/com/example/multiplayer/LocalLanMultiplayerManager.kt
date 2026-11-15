package com.example.multiplayer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class LocalLanMultiplayerManager(context: Context) {

    companion object {
        const val SERVICE_TYPE = "_geminiodyssey._tcp."
        private const val SERVICE_PREFIX = "GeminiOdyssey-"
    }

    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow(CoopSessionState(mode = CoopMode.LOCAL_LAN))
    val session: StateFlow<CoopSessionState> = _session.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredLanHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredLanHost>> = _discoveredHosts.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var guestSocket: Socket? = null
    private var guestWriter: BufferedWriter? = null
    private var guestReadJob: Job? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private data class ConnectedGuest(
        val uid: String,
        val socket: Socket,
        val writer: BufferedWriter,
        var member: CoopMember,
        var playerState: RemotePlayerState
    )

    private val hostGuests = ConcurrentHashMap<String, ConnectedGuest>()
    private var hostSelfState: RemotePlayerState? = null
    private var hostMember: CoopMember? = null
    private var hostRoomCode: String = ""
    private var hostSectorId: String = "sector_plaza"
    private var hostStatus: CoopRoomStatus = CoopRoomStatus.WAITING

    fun startHost(uid: String, displayName: String, sectorId: String, maxHp: Float) {
        stopSession()
        val safeName = displayName.trim().ifEmpty { "Gemini Host" }.take(32)
        val roomCode = "LAN-" + (1000 + Random.nextInt(9000))
        hostRoomCode = roomCode
        hostSectorId = sectorId
        hostStatus = CoopRoomStatus.WAITING
        hostMember = CoopMember(
            uid = uid,
            displayName = safeName,
            colorIndex = 0,
            ready = true,
            isHost = true
        )
        hostSelfState = RemotePlayerState(
            uid = uid,
            displayName = safeName,
            colorIndex = 0,
            x = 0f,
            y = 0f,
            facingAngle = -1.57f,
            hp = maxHp,
            maxHp = maxHp,
            isAlive = true
        )

        acceptJob = scope.launch {
            try {
                val server = ServerSocket(0)
                serverSocket = server
                val port = server.localPort
                val localIp = findLocalIpv4Address() ?: "127.0.0.1"

                _session.value = CoopSessionState(
                    isActive = true,
                    mode = CoopMode.LOCAL_LAN,
                    roomCode = roomCode,
                    hostUid = uid,
                    localUid = uid,
                    isHost = true,
                    sectorId = sectorId,
                    status = CoopRoomStatus.WAITING,
                    members = listOfNotNull(hostMember),
                    localHostEndpoint = "$localIp:$port"
                )

                registerNsdService(roomCode = roomCode, port = port)

                while (!server.isClosed) {
                    val socket = server.accept()
                    if (hostGuests.size >= 3) {
                        runCatching {
                            val w = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                            w.write(JSONObject().put("type", "full").toString() + "\n")
                            w.flush()
                            socket.close()
                        }
                        continue
                    }
                    launch { handleGuestConnection(socket) }
                }
            } catch (e: Exception) {
                if (_session.value.isActive) {
                    _session.update { it.copy(errorMessage = e.localizedMessage ?: "LAN host stopped") }
                }
            }
        }
    }

    private fun handleGuestConnection(socket: Socket) {
        var guestUid: String? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            while (!socket.isClosed) {
                val line = reader.readLine() ?: break
                val json = JSONObject(line)
                when (json.optString("type")) {
                    "hello" -> {
                        val uid = json.optString("uid").ifBlank { "guest_${Random.nextInt(10000)}" }
                        val name = json.optString("name", "Friend").take(32)
                        val maxHp = json.optDouble("maxHp", 130.0).toFloat()
                        if (hostGuests.size >= 3 && !hostGuests.containsKey(uid)) {
                            writer.write(JSONObject().put("type", "full").toString() + "\n")
                            writer.flush()
                            socket.close()
                            return
                        }
                        guestUid = uid
                        val usedColors = hostGuests.values.map { it.member.colorIndex }.toSet()
                        val assignedColor = (1..3).firstOrNull { it !in usedColors } ?: 1
                        val member = CoopMember(
                            uid = uid,
                            displayName = name,
                            colorIndex = assignedColor,
                            ready = false,
                            isHost = false
                        )
                        val pState = RemotePlayerState(
                            uid = uid,
                            displayName = name,
                            colorIndex = assignedColor,
                            x = (assignedColor - 1.5f) * 50f,
                            y = 40f,
                            facingAngle = -1.57f,
                            hp = maxHp,
                            maxHp = maxHp,
                            isAlive = true
                        )
                        hostGuests[uid] = ConnectedGuest(uid, socket, writer, member, pState)
                        emitAndBroadcastLobby()
                    }
                    "ready" -> {
                        val uid = json.optString("uid")
                        val ready = json.optBoolean("ready", false)
                        hostGuests[uid]?.let { g ->
                            g.member = g.member.copy(ready = ready)
                            emitAndBroadcastLobby()
                        }
                    }
                    "player_state" -> {
                        val uid = json.optString("uid")
                        hostGuests[uid]?.let { g ->
                            g.playerState = g.playerState.copy(
                                x = json.optDouble("x", 0.0).toFloat(),
                                y = json.optDouble("y", 0.0).toFloat(),
                                facingAngle = json.optDouble("facing", -1.57).toFloat(),
                                hp = json.optDouble("hp", 130.0).toFloat(),
                                maxHp = json.optDouble("maxHp", 130.0).toFloat(),
                                isGuarding = json.optBoolean("guard", false),
                                isSlashing = json.optBoolean("slash", false),
                                slashComboStep = json.optInt("combo", 0),
                                walkPhase = json.optDouble("walk", 0.0).toFloat(),
                                isAlive = json.optDouble("hp", 130.0) > 0.0,
                                hitSeq = json.optLong("hitSeq", g.playerState.hitSeq),
                                lastHitEnemyId = json.optInt("hitEnemy", -1),
                                lastHitDamage = json.optDouble("hitDmg", 0.0).toFloat()
                            )
                            _session.update { cur ->
                                cur.copy(remotePlayers = hostGuests.values.map { it.playerState })
                            }
                        }
                    }
                    "leave" -> break
                }
            }
        } catch (_: Exception) {
            // Guest disconnected
        } finally {
            guestUid?.let { uid ->
                hostGuests.remove(uid)
                emitAndBroadcastLobby()
            }
            runCatching { socket.close() }
        }
    }

    fun startDiscovery() {
        stopDiscovery()
        acquireMulticastLock()
        val manager = nsdManager ?: return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (!service.serviceType.contains("geminiodyssey")) return
                if (!service.serviceName.startsWith(SERVICE_PREFIX)) return
                @Suppress("DEPRECATION")
                manager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        @Suppress("DEPRECATION")
                        val hostAddr = serviceInfo.host?.hostAddress ?: return
                        val code = serviceInfo.serviceName.removePrefix(SERVICE_PREFIX)
                        val found = DiscoveredLanHost(
                            serviceName = serviceInfo.serviceName,
                            displayName = "Room $code",
                            roomCode = code,
                            hostAddress = hostAddr,
                            port = serviceInfo.port
                        )
                        _discoveredHosts.update { current ->
                            (current.filterNot { it.serviceName == found.serviceName } + found)
                        }
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredHosts.update { list -> list.filterNot { it.serviceName == service.serviceName } }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                runCatching { manager.stopServiceDiscovery(this) }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        discoveryListener = listener
        runCatching {
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    fun stopDiscovery() {
        val listener = discoveryListener
        discoveryListener = null
        if (listener != null) {
            runCatching { nsdManager?.stopServiceDiscovery(listener) }
        }
        releaseMulticastLock()
    }

    fun joinLanHost(
        hostAddress: String,
        port: Int,
        uid: String,
        displayName: String,
        maxHp: Float
    ) {
        stopSession()
        _session.value = CoopSessionState(
            isActive = false,
            mode = CoopMode.LOCAL_LAN,
            localUid = uid,
            isConnecting = true
        )
        val safeName = displayName.trim().ifEmpty { "Gemini Knight" }.take(32)

        guestReadJob = scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(hostAddress.trim(), port), 4500)
                guestSocket = socket
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                guestWriter = writer
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                val hello = JSONObject()
                    .put("type", "hello")
                    .put("uid", uid)
                    .put("name", safeName)
                    .put("maxHp", maxHp.toDouble())
                writer.write(hello.toString() + "\n")
                writer.flush()

                while (!socket.isClosed) {
                    val line = reader.readLine() ?: break
                    val json = JSONObject(line)
                    when (json.optString("type")) {
                        "full" -> {
                            _session.value = CoopSessionState(
                                isActive = false,
                                mode = CoopMode.LOCAL_LAN,
                                errorMessage = "Room is full (4/4 players)."
                            )
                            break
                        }
                        "lobby" -> handleLobbyMessage(json, uid)
                        "world" -> handleWorldMessage(json, uid)
                    }
                }
            } catch (e: Exception) {
                _session.value = CoopSessionState(
                    isActive = false,
                    mode = CoopMode.LOCAL_LAN,
                    errorMessage = e.localizedMessage ?: "Unable to connect to $hostAddress:$port"
                )
            }
        }
    }

    fun setReady(uid: String, ready: Boolean) {
        if (_session.value.isHost) {
            hostMember = hostMember?.copy(ready = ready)
            emitAndBroadcastLobby()
        } else {
            scope.launch {
                sendToHost(JSONObject().put("type", "ready").put("uid", uid).put("ready", ready))
            }
        }
    }

    fun selectSector(sectorId: String) {
        if (!_session.value.isHost) return
        hostSectorId = sectorId
        emitAndBroadcastLobby()
    }

    fun startGame() {
        if (!_session.value.isHost) return
        hostStatus = CoopRoomStatus.PLAYING
        emitAndBroadcastLobby()
    }

    fun publishLocalPlayerState(
        uid: String,
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
        if (_session.value.isHost) {
            hostSelfState = hostSelfState?.copy(
                x = x,
                y = y,
                facingAngle = facingAngle,
                hp = hp,
                maxHp = maxHp,
                isGuarding = isGuarding,
                isSlashing = isSlashing,
                slashComboStep = slashComboStep,
                walkPhase = walkPhase,
                isAlive = hp > 0f
            )
            return
        }
        scope.launch {
            val msg = JSONObject()
                .put("type", "player_state")
                .put("uid", uid)
                .put("x", x.toDouble())
                .put("y", y.toDouble())
                .put("facing", facingAngle.toDouble())
                .put("hp", hp.toDouble())
                .put("maxHp", maxHp.toDouble())
                .put("guard", isGuarding)
                .put("slash", isSlashing)
                .put("combo", slashComboStep)
                .put("walk", walkPhase.toDouble())
                .put("hitSeq", hitSeq)
                .put("hitEnemy", lastHitEnemyId)
                .put("hitDmg", lastHitDamage.toDouble())
            sendToHost(msg)
        }
    }

    fun publishHostWorldState(snapshot: CoopWorldSnapshot, status: CoopRoomStatus) {
        if (!_session.value.isHost) return
        hostStatus = status
        val allPlayers = listOfNotNull(hostSelfState) + hostGuests.values.map { it.playerState }
        val pArray = JSONArray()
        allPlayers.forEach { p ->
            pArray.put(
                JSONObject()
                    .put("uid", p.uid)
                    .put("name", p.displayName)
                    .put("color", p.colorIndex)
                    .put("x", p.x.toDouble())
                    .put("y", p.y.toDouble())
                    .put("facing", p.facingAngle.toDouble())
                    .put("hp", p.hp.toDouble())
                    .put("maxHp", p.maxHp.toDouble())
                    .put("guard", p.isGuarding)
                    .put("slash", p.isSlashing)
                    .put("combo", p.slashComboStep)
                    .put("walk", p.walkPhase.toDouble())
            )
        }
        val eArray = JSONArray()
        snapshot.enemies.take(25).forEach { e ->
            eArray.put(
                JSONObject()
                    .put("id", e.id)
                    .put("type", e.typeName)
                    .put("x", e.x.toDouble())
                    .put("y", e.y.toDouble())
                    .put("hp", e.hp.toDouble())
                    .put("maxHp", e.maxHp.toDouble())
                    .put("facing", e.facingAngle.toDouble())
                    .put("stun", e.stunTimer.toDouble())
            )
        }
        val msg = JSONObject()
            .put("type", "world")
            .put("wave", snapshot.wave)
            .put("totalWaves", snapshot.totalWaves)
            .put("nodes", snapshot.nodesIndexed)
            .put("totalNodes", snapshot.totalNodes)
            .put("bossHp", (snapshot.bossHp ?: 0f).toDouble())
            .put("bossMaxHp", (snapshot.bossMaxHp ?: 0f).toDouble())
            .put("over", snapshot.isMissionOver)
            .put("victory", snapshot.isVictory)
            .put("stars", snapshot.starsEarned)
            .put("players", pArray)
            .put("enemies", eArray)

        scope.launch {
            broadcastToGuests(msg)
        }
    }

    private fun handleLobbyMessage(json: JSONObject, localUid: String) {
        val roomCode = json.optString("roomCode")
        val hostUid = json.optString("hostUid")
        val sectorId = json.optString("sectorId", "sector_plaza")
        val status = CoopRoomStatus.fromWire(json.optString("status", "waiting"))
        val arr = json.optJSONArray("members") ?: JSONArray()
        val members = mutableListOf<CoopMember>()
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            members.add(
                CoopMember(
                    uid = m.optString("uid"),
                    displayName = m.optString("name"),
                    colorIndex = m.optInt("color"),
                    ready = m.optBoolean("ready"),
                    isHost = m.optBoolean("host")
                )
            )
        }
        _session.value = _session.value.copy(
            isActive = true,
            mode = CoopMode.LOCAL_LAN,
            roomCode = roomCode,
            hostUid = hostUid,
            localUid = localUid,
            isHost = false,
            sectorId = sectorId,
            status = status,
            members = members,
            isConnecting = false,
            errorMessage = null
        )
    }

    private fun handleWorldMessage(json: JSONObject, localUid: String) {
        val pArray = json.optJSONArray("players") ?: JSONArray()
        val remotes = mutableListOf<RemotePlayerState>()
        for (i in 0 until pArray.length()) {
            val p = pArray.getJSONObject(i)
            val uid = p.optString("uid")
            if (uid != localUid) {
                remotes.add(
                    RemotePlayerState(
                        uid = uid,
                        displayName = p.optString("name"),
                        colorIndex = p.optInt("color"),
                        x = p.optDouble("x").toFloat(),
                        y = p.optDouble("y").toFloat(),
                        facingAngle = p.optDouble("facing").toFloat(),
                        hp = p.optDouble("hp").toFloat(),
                        maxHp = p.optDouble("maxHp").toFloat(),
                        isGuarding = p.optBoolean("guard"),
                        isSlashing = p.optBoolean("slash"),
                        slashComboStep = p.optInt("combo"),
                        walkPhase = p.optDouble("walk").toFloat(),
                        isAlive = p.optDouble("hp") > 0.0
                    )
                )
            }
        }
        val eArray = json.optJSONArray("enemies") ?: JSONArray()
        val enemies = mutableListOf<CoopEnemySnapshot>()
        for (i in 0 until eArray.length()) {
            val e = eArray.getJSONObject(i)
            enemies.add(
                CoopEnemySnapshot(
                    id = e.optInt("id"),
                    typeName = e.optString("type"),
                    x = e.optDouble("x").toFloat(),
                    y = e.optDouble("y").toFloat(),
                    hp = e.optDouble("hp").toFloat(),
                    maxHp = e.optDouble("maxHp").toFloat(),
                    facingAngle = e.optDouble("facing").toFloat(),
                    stunTimer = e.optDouble("stun").toFloat()
                )
            )
        }
        val bossMax = json.optDouble("bossMaxHp", 0.0).toFloat()
        val bossHp = json.optDouble("bossHp", 0.0).toFloat()
        val isOver = json.optBoolean("over", false)
        val snapshot = CoopWorldSnapshot(
            wave = json.optInt("wave", 1),
            totalWaves = json.optInt("totalWaves", 3),
            nodesIndexed = json.optInt("nodes", 0),
            totalNodes = json.optInt("totalNodes", 2),
            bossHp = if (bossMax > 0f) bossHp else null,
            bossMaxHp = if (bossMax > 0f) bossMax else null,
            isMissionOver = isOver,
            isVictory = json.optBoolean("victory", false),
            starsEarned = json.optInt("stars", 0),
            enemies = enemies
        )
        _session.update { cur ->
            cur.copy(
                status = if (isOver) CoopRoomStatus.FINISHED else CoopRoomStatus.PLAYING,
                remotePlayers = remotes,
                hostWorldSnapshot = snapshot
            )
        }
    }

    private fun emitAndBroadcastLobby() {
        val members = listOfNotNull(hostMember) + hostGuests.values.map { it.member }
        val remotes = hostGuests.values.map { it.playerState }
        _session.update { cur ->
            cur.copy(
                isActive = true,
                sectorId = hostSectorId,
                status = hostStatus,
                members = members,
                remotePlayers = remotes
            )
        }
        val arr = JSONArray()
        members.forEach { m ->
            arr.put(
                JSONObject()
                    .put("uid", m.uid)
                    .put("name", m.displayName)
                    .put("color", m.colorIndex)
                    .put("ready", m.ready)
                    .put("host", m.isHost)
            )
        }
        val msg = JSONObject()
            .put("type", "lobby")
            .put("roomCode", hostRoomCode)
            .put("hostUid", hostMember?.uid.orEmpty())
            .put("sectorId", hostSectorId)
            .put("status", hostStatus.wire)
            .put("members", arr)
        scope.launch {
            broadcastToGuests(msg)
        }
    }

    private suspend fun sendToHost(json: JSONObject) = withContext(Dispatchers.IO) {
        val writer = guestWriter ?: return@withContext
        synchronized(writer) {
            runCatching {
                writer.write(json.toString() + "\n")
                writer.flush()
            }
        }
    }

    private suspend fun broadcastToGuests(json: JSONObject) = withContext(Dispatchers.IO) {
        val line = json.toString() + "\n"
        hostGuests.values.forEach { guest ->
            synchronized(guest.writer) {
                runCatching {
                    guest.writer.write(line)
                    guest.writer.flush()
                }
            }
        }
    }

    private fun registerNsdService(roomCode: String, port: Int) {
        val manager = nsdManager ?: return
        val info = NsdServiceInfo().apply {
            serviceName = "$SERVICE_PREFIX$roomCode"
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        registrationListener = listener
        runCatching {
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    fun stopSession() {
        acceptJob?.cancel()
        acceptJob = null
        guestReadJob?.cancel()
        guestReadJob = null

        val regListener = registrationListener
        registrationListener = null
        if (regListener != null) {
            runCatching { nsdManager?.unregisterService(regListener) }
        }

        hostGuests.values.forEach { runCatching { it.socket.close() } }
        hostGuests.clear()

        runCatching { serverSocket?.close() }
        serverSocket = null

        runCatching { guestSocket?.close() }
        guestSocket = null
        guestWriter = null

        _session.value = CoopSessionState(mode = CoopMode.LOCAL_LAN)
    }

    fun release() {
        stopDiscovery()
        stopSession()
        scope.cancel()
    }

    private fun acquireMulticastLock() {
        if (multicastLock?.isHeld == true) return
        runCatching {
            val lock = wifiManager?.createMulticastLock("GeminiOdysseyNsd")?.apply {
                setReferenceCounted(false)
                acquire()
            }
            multicastLock = lock
        }
    }

    private fun releaseMulticastLock() {
        runCatching {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        }
        multicastLock = null
    }

    private fun findLocalIpv4Address(): String? {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (!nif.isUp || nif.isLoopback) continue
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        }.getOrNull()
    }
}
