package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.GeminiBriefingUiState
import com.example.ai.GeminiChatMessage
import com.example.ai.GeminiChatUiState
import com.example.data.PlayerProgress
import com.example.game.SectorDefinition
import com.example.game.WebSectors
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GoogleGold
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.VirusCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GeminiAiModeTab(
    progress: PlayerProgress,
    chatState: GeminiChatUiState,
    chatMessages: List<GeminiChatMessage>,
    briefingState: GeminiBriefingUiState,
    palette: GooglePalette,
    onAskGemini: (String) -> Unit,
    onRefreshBriefing: () -> Unit,
    onClearChat: () -> Unit,
    onLaunchSector: (SectorDefinition) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }
    val quickPrompts = remember {
        listOf(
            "Best build for my level?",
            "How do I beat Pop-Up Titan?",
            "4P Co-Op squad synergy tips",
            "Which Tech Lab upgrade next?"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Gemini AI Mode Header + Structured Mission Briefing Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.bg),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(palette.borderWidth, palette.activeBlue.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(GeminiBlue, GoogleGreen, GoogleGold)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Gemini AI",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "GEMINI AI OVERVIEW",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.activeBlue
                                    )
                                    Text(
                                        text = "Powered by Firebase AI Logic • Lv.${progress.level}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSub
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onRefreshBriefing,
                                enabled = briefingState !is GeminiBriefingUiState.Loading,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(palette.borderWidth, palette.activeBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("gemini_refresh_briefing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Generate AI Briefing",
                                    tint = palette.activeBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (briefingState is GeminiBriefingUiState.Idle) "Briefing" else "Refresh",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.activeBlue
                                )
                            }
                        }

                        when (briefingState) {
                            is GeminiBriefingUiState.Idle -> {
                                Text(
                                    text = "Tap Briefing to let Gemini analyze your stars (${progress.totalStars}), coins (${progress.googleCoins} G), and upgrades for a live battle plan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSub
                                )
                            }
                            is GeminiBriefingUiState.Loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = palette.activeBlue,
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        text = "Analyzing Web Sector telemetry with Gemini...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textDark
                                    )
                                }
                            }
                            is GeminiBriefingUiState.Error -> {
                                Surface(
                                    color = VirusCrimson.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(palette.borderWidth, VirusCrimson),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.WarningAmber,
                                            contentDescription = null,
                                            tint = VirusCrimson,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = briefingState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VirusCrimson
                                        )
                                    }
                                }
                            }
                            is GeminiBriefingUiState.Success -> {
                                val briefing = briefingState.briefing
                                val targetSector = WebSectors.all.firstOrNull { it.id == briefing.recommendedSectorId }
                                    ?: WebSectors.all.first()

                                Text(
                                    text = briefing.tacticalSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textDark
                                )

                                BriefingDetailRow(
                                    icon = Icons.Rounded.Shield,
                                    tint = VirusCrimson,
                                    label = "Threat Counter",
                                    value = briefing.primaryThreatCounter,
                                    palette = palette
                                )
                                BriefingDetailRow(
                                    icon = Icons.Rounded.Upgrade,
                                    tint = GoogleGold,
                                    label = "Upgrade Priority",
                                    value = briefing.recommendedUpgrade,
                                    palette = palette
                                )
                                BriefingDetailRow(
                                    icon = Icons.Rounded.Psychology,
                                    tint = GoogleGreen,
                                    label = "Squad & Wave Tip",
                                    value = briefing.squadTip,
                                    palette = palette
                                )

                                Button(
                                    onClick = { onLaunchSector(targetSector) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoogleGreen,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("gemini_launch_recommended_sector")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DEPLOY: ${briefing.recommendedSectorName.uppercase()}",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Prompt Suggestion Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ASK GEMINI ORACLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub
                        )
                        if (chatMessages.isNotEmpty()) {
                            IconButton(
                                onClick = onClearChat,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("gemini_clear_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = "Clear AI Conversation",
                                    tint = palette.textSub
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPrompts.forEachIndexed { idx, quick ->
                            OutlinedButton(
                                onClick = { onAskGemini(quick) },
                                enabled = chatState !is GeminiChatUiState.Loading && chatState !is GeminiChatUiState.Streaming,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = palette.bg,
                                    contentColor = palette.linkBlue
                                ),
                                border = BorderStroke(palette.borderWidth, palette.border),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("gemini_quick_prompt_$idx")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = GeminiBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = quick, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Multi-turn Chat History
            items(chatMessages, key = { it.id }) { msg ->
                GeminiChatBubble(
                    message = msg,
                    palette = palette
                )
            }

            // Live Streaming / Loading / Error State Bubble
            when (chatState) {
                is GeminiChatUiState.Loading -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = palette.bg),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(palette.borderWidth, palette.activeBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = palette.activeBlue,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Gemini is thinking...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textDark
                                )
                            }
                        }
                    }
                }
                is GeminiChatUiState.Streaming -> {
                    item {
                        GeminiChatBubble(
                            message = GeminiChatMessage(
                                id = -1L,
                                isUser = false,
                                text = chatState.partialText.ifEmpty { "..." }
                            ),
                            palette = palette,
                            isStreaming = true
                        )
                    }
                }
                is GeminiChatUiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = palette.bg),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(palette.borderWidth, VirusCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_chat_error_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = chatState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VirusCrimson
                                )
                                val lastUserPrompt = chatMessages.lastOrNull { it.isUser }?.text
                                if (!lastUserPrompt.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { onAskGemini(lastUserPrompt) },
                                        border = BorderStroke(palette.borderWidth, VirusCrimson),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Retry", color = VirusCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }

        // Bottom Prompt Composer
        Surface(
            color = palette.bg,
            border = BorderStroke(palette.borderWidth, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            text = "Ask Gemini for strategy, builds, or lore...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gemini_prompt_input")
                )
                val isBusy = chatState is GeminiChatUiState.Loading || chatState is GeminiChatUiState.Streaming
                Button(
                    onClick = {
                        val trimmed = promptInput.trim()
                        if (trimmed.isNotEmpty()) {
                            promptInput = ""
                            onAskGemini(trimmed)
                        }
                    },
                    enabled = !isBusy && promptInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.activeBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("gemini_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Ask Gemini",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BriefingDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    value: String,
    palette: GooglePalette
) {
    Surface(
        color = palette.surfaceGray,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(palette.borderWidth, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp)
            )
            Column {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textDark
                )
            }
        }
    }
}

@Composable
private fun GeminiChatBubble(
    message: GeminiChatMessage,
    palette: GooglePalette,
    isStreaming: Boolean = false
) {
    val container = if (message.isUser) palette.activeBlue else palette.bg
    val textColor = if (message.isUser) Color.White else palette.textDark
    val border = if (message.isUser) {
        BorderStroke(palette.borderWidth, palette.activeBlue)
    } else {
        BorderStroke(palette.borderWidth, palette.border)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = container),
            shape = RoundedCornerShape(16.dp),
            border = border,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!message.isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = GoogleGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isStreaming) "GEMINI ORACLE • STREAMING" else "GEMINI ORACLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.activeBlue
                        )
                    }
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}
