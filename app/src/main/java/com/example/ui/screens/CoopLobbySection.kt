package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlayerProgress
import com.example.game.WebSectors
import com.example.multiplayer.CoopMode
import com.example.multiplayer.CoopSessionState
import com.example.multiplayer.DiscoveredLanHost
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GoogleGold
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.VirusCrimson

@Composable
internal fun CoopLobbyTab(
    progress: PlayerProgress,
    coopSession: CoopSessionState,
    discoveredLanHosts: List<DiscoveredLanHost>,
    currentUserId: String,
    isGuestMode: Boolean = false,
    palette: GooglePalette,
    onHostOnlineRoom: (String) -> Unit,
    onJoinOnlineRoom: (String) -> Unit,
    onHostLanRoom: (String) -> Unit,
    onJoinLanHost: (DiscoveredLanHost) -> Unit,
    onJoinLanByAddress: (String) -> Unit,
    onStartLanDiscovery: () -> Unit,
    onStopLanDiscovery: () -> Unit,
    onToggleReady: () -> Unit,
    onSelectSector: (String) -> Unit,
    onStartCoopStage: () -> Unit,
    onLeaveCoopRoom: () -> Unit,
    onSignInWithGoogle: () -> Unit = {}
) {
    var selectedMode by remember { mutableStateOf(CoopMode.ONLINE) }
    var onlineCodeInput by remember { mutableStateOf("") }
    var lanAddressInput by remember { mutableStateOf("") }

    DisposableEffect(selectedMode, coopSession.isActive) {
        if (selectedMode == CoopMode.LOCAL_LAN && !coopSession.isActive) {
            onStartLanDiscovery()
        } else {
            onStopLanDiscovery()
        }
        onDispose { onStopLanDiscovery() }
    }

    val unlockedSectors = remember(progress.totalStars) {
        WebSectors.all.filter { progress.totalStars >= it.unlockRequirementStars }
    }
    val defaultSectorId = unlockedSectors.firstOrNull()?.id ?: WebSectors.all.first().id

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // Crisp Hero Banner for 4-Player Co-Op using generated artwork
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(palette.borderWidth, palette.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_web_universe_banner),
                        contentDescription = "Gemini Odyssey 4-Player Co-Op Web Arena",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0A1026).copy(alpha = 0.88f),
                                        Color(0xFF0A1026).copy(alpha = 0.45f)
                                    )
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Groups,
                                    contentDescription = null,
                                    tint = GoogleGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "4-PLAYER CO-OP SQUAD",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GoogleGold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Battle on one stage with up to 4 friends",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Online Room Codes or Local Wi-Fi Devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDCE3F9)
                            )
                        }
                    }
                }
            }
        }

        if (coopSession.isActive) {
            item {
                ActiveCoopRoomCard(
                    coopSession = coopSession,
                    currentUserId = currentUserId,
                    palette = palette,
                    onToggleReady = onToggleReady,
                    onSelectSector = onSelectSector,
                    onStartCoopStage = onStartCoopStage,
                    onLeaveCoopRoom = onLeaveCoopRoom
                )
            }
        } else {
            item {
                // Mode Switcher: Online vs Local Devices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoopModePill(
                        label = "Online (Cloud)",
                        icon = Icons.Rounded.CloudQueue,
                        selected = selectedMode == CoopMode.ONLINE,
                        palette = palette,
                        testTag = "coop_mode_online",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMode = CoopMode.ONLINE }
                    )
                    CoopModePill(
                        label = "Local Devices",
                        icon = Icons.Rounded.Devices,
                        selected = selectedMode == CoopMode.LOCAL_LAN,
                        palette = palette,
                        testTag = "coop_mode_lan",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMode = CoopMode.LOCAL_LAN }
                    )
                }
            }

            if (coopSession.isConnecting) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = palette.bg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(palette.borderWidth, palette.activeBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = palette.activeBlue, strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Connecting to squad room...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textDark
                            )
                        }
                    }
                }
            }

            coopSession.errorMessage?.let { err ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = palette.bg),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(palette.borderWidth, VirusCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = VirusCrimson,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (selectedMode == CoopMode.ONLINE) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = palette.bg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(palette.borderWidth, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGuestMode) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "ONLINE CLOUD CO-OP • GOOGLE SIGN-IN REQUIRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.activeBlue
                                )
                                Text(
                                    text = "Guest Mode supports full Local Wi-Fi Connected Devices co-op right now! Sign in with Google to host or join Online Room Codes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSub
                                )
                                Button(
                                    onClick = onSignInWithGoogle,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.activeBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("coop_guest_sign_in_button")
                                ) {
                                    Text("SIGN IN WITH GOOGLE", style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = { selectedMode = CoopMode.LOCAL_LAN },
                                    border = BorderStroke(palette.borderWidth, GoogleGreen),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = GoogleGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SWITCH TO LOCAL WI-FI", color = GoogleGreen, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                            Text(
                                text = "ONLINE ROOMS (UP TO 4 FRIENDS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSub
                            )
                            Button(
                                onClick = { onHostOnlineRoom(defaultSectorId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.activeBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("host_online_room_button")
                            ) {
                                Icon(Icons.Rounded.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CREATE ONLINE ROOM", style = MaterialTheme.typography.labelLarge)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = onlineCodeInput,
                                    onValueChange = { onlineCodeInput = it.uppercase().take(8) },
                                    singleLine = true,
                                    label = { Text("Room Code (e.g. GM4X9K)") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("online_room_code_input")
                                )
                                Button(
                                    onClick = { onJoinOnlineRoom(onlineCodeInput) },
                                    enabled = onlineCodeInput.trim().length >= 4,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoogleGreen,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("join_online_room_button")
                                ) {
                                    Text("JOIN", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = palette.bg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(palette.borderWidth, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "LOCAL CONNECTED DEVICES (WI-FI / HOTSPOT)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSub
                                    )
                                    Text(
                                        text = "Play with nearby friends who downloaded the app on the same Wi-Fi.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSub
                                    )
                                }
                                IconButton(
                                    onClick = onStartLanDiscovery,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh nearby devices", tint = palette.activeBlue)
                                }
                            }

                            Button(
                                onClick = { onHostLanRoom(defaultSectorId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.activeBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("host_lan_room_button")
                            ) {
                                Icon(Icons.Rounded.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("HOST LOCAL WI-FI ROOM", style = MaterialTheme.typography.labelLarge)
                            }

                            if (discoveredLanHosts.isEmpty()) {
                                Surface(
                                    color = palette.surfaceGray,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(palette.borderWidth, palette.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Scanning local network for hosted Gemini Odyssey devices...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSub,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                discoveredLanHosts.forEach { host ->
                                    Surface(
                                        color = palette.surfaceGray,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(palette.borderWidth, palette.activeBlue.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(host.displayName, style = MaterialTheme.typography.titleSmall, color = palette.textDark)
                                                Text("${host.hostAddress}:${host.port}", style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                                            }
                                            Button(
                                                onClick = { onJoinLanHost(host) },
                                                colors = ButtonDefaults.buttonColors(containerColor = GoogleGreen, contentColor = Color.White),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.height(42.dp)
                                            ) {
                                                Text("CONNECT", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = lanAddressInput,
                                    onValueChange = { lanAddressInput = it },
                                    singleLine = true,
                                    label = { Text("Direct IP:Port (192.168.x.x:port)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { onJoinLanByAddress(lanAddressInput) },
                                    enabled = lanAddressInput.contains(":"),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("JOIN IP")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoopModePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    palette: GooglePalette,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) palette.activeBlue else palette.bg,
            contentColor = if (selected) Color.White else palette.textDark
        ),
        border = BorderStroke(
            palette.borderWidth,
            if (selected) palette.activeBlue else palette.border
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ActiveCoopRoomCard(
    coopSession: CoopSessionState,
    currentUserId: String,
    palette: GooglePalette,
    onToggleReady: () -> Unit,
    onSelectSector: (String) -> Unit,
    onStartCoopStage: () -> Unit,
    onLeaveCoopRoom: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.bg),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(palette.borderWidth, palette.activeBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (coopSession.mode == CoopMode.ONLINE) "ONLINE SQUAD ROOM" else "LOCAL WI-FI SQUAD ROOM",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.activeBlue
                    )
                    Text(
                        text = "Code: ${coopSession.roomCode} (${coopSession.playerCount}/4)",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textDark,
                        fontWeight = FontWeight.Bold
                    )
                    coopSession.localHostEndpoint?.let { endpoint ->
                        Text(
                            text = "Direct LAN IP: $endpoint",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.urlGreen
                        )
                    }
                }

                IconButton(
                    onClick = onLeaveCoopRoom,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("leave_coop_room_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Leave Room", tint = VirusCrimson)
                }
            }

            // 4 Squad Slots
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (slot in 0 until 4) {
                    val member = coopSession.members.getOrNull(slot)
                    val slotColor = when (slot) {
                        0 -> GeminiBlue
                        1 -> VirusCrimson
                        2 -> GoogleGold
                        else -> GoogleGreen
                    }
                    Surface(
                        color = palette.surfaceGray,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            palette.borderWidth,
                            if (member != null) slotColor.copy(alpha = 0.75f) else palette.border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (member != null) slotColor else palette.border)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = member?.let {
                                        buildString {
                                            append(it.displayName)
                                            if (it.uid == currentUserId) append(" (You)")
                                            if (it.isHost) append(" • HOST")
                                        }
                                    } ?: "Open Player Slot ${slot + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (member != null) palette.textDark else palette.textSub,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (member != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = if (member.ready || member.isHost) GoogleGreen else palette.textSub,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (member.isHost) "HOST" else if (member.ready) "READY" else "WAITING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (member.ready || member.isHost) GoogleGreen else palette.textSub
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stage Selector (Host) or Stage Preview (Guest)
            Text(
                text = "SELECTED STAGE",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub
            )
            if (coopSession.isHost) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WebSectors.all.forEach { sec ->
                        val selected = coopSession.sectorId == sec.id
                        OutlinedButton(
                            onClick = { onSelectSector(sec.id) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) sec.primaryColor.copy(alpha = 0.18f) else palette.surfaceGray,
                                contentColor = palette.textDark
                            ),
                            border = BorderStroke(
                                palette.borderWidth,
                                if (selected) sec.primaryColor else palette.border
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Text(
                                text = sec.name.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                val secName = WebSectors.all.firstOrNull { it.id == coopSession.sectorId }?.name ?: "Searchlight Plaza"
                Text(text = secName, style = MaterialTheme.typography.titleMedium, color = palette.linkBlue)
            }

            if (coopSession.isHost) {
                Button(
                    onClick = onStartCoopStage,
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_coop_stage_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START 4P STAGE (${coopSession.playerCount}/4)", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                val amReady = coopSession.members.firstOrNull { it.uid == currentUserId }?.ready == true
                Button(
                    onClick = onToggleReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (amReady) palette.surfaceGray else palette.activeBlue,
                        contentColor = if (amReady) palette.textDark else Color.White
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("toggle_coop_ready_button")
                ) {
                    Text(if (amReady) "CANCEL READY" else "READY UP", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
