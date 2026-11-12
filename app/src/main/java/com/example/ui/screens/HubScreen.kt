package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.WebAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerProgress
import com.example.data.StatType
import com.example.data.UpgradeType
import com.example.game.SectorDefinition
import com.example.game.WebSectors
import com.example.ui.components.GeminiCharacterEmblem
import com.example.ui.theme.AdwareMagenta
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GoogleGold
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.HackerNeonGreen
import com.example.ui.theme.VirusCrimson

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.ui.window.Dialog
import com.example.data.ThemeMode

private data class GooglePalette(
    val bg: Color,
    val surfaceGray: Color,
    val border: Color,
    val linkBlue: Color,
    val activeBlue: Color,
    val textDark: Color,
    val textSub: Color,
    val urlGreen: Color,
    val borderWidth: androidx.compose.ui.unit.Dp
)

@Composable
private fun rememberGooglePalette(themeMode: ThemeMode, highContrast: Boolean): GooglePalette {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    return remember(isDark, highContrast) {
        if (isDark) {
            GooglePalette(
                bg = if (highContrast) Color.Black else Color(0xFF202124),
                surfaceGray = if (highContrast) Color(0xFF121212) else Color(0xFF303134),
                border = if (highContrast) Color.White else Color(0xFF5F6368),
                linkBlue = Color(0xFF8AB4F8),
                activeBlue = Color(0xFF8AB4F8),
                textDark = if (highContrast) Color.White else Color(0xFFE8EAED),
                textSub = if (highContrast) Color(0xFFE8EAED) else Color(0xFF9AA0A6),
                urlGreen = Color(0xFF81C995),
                borderWidth = if (highContrast) 2.dp else 1.dp
            )
        } else {
            GooglePalette(
                bg = Color(0xFFFFFFFF),
                surfaceGray = if (highContrast) Color(0xFFF1F3F4) else Color(0xFFF8F9FA),
                border = if (highContrast) Color.Black else Color(0xFFDADCE0),
                linkBlue = Color(0xFF1A0DAB),
                activeBlue = Color(0xFF1A73E8),
                textDark = if (highContrast) Color.Black else Color(0xFF202124),
                textSub = if (highContrast) Color(0xFF202124) else Color(0xFF5F6368),
                urlGreen = Color(0xFF006621),
                borderWidth = if (highContrast) 2.dp else 1.dp
            )
        }
    }
}

@Composable
fun HubScreen(
    progress: PlayerProgress,
    onLaunchSector: (SectorDefinition) -> Unit,
    onPurchaseUpgrade: (UpgradeType) -> Unit,
    onAllocateSkillPoint: (StatType) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSetLargeTouchTargets: (Boolean) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onToggleHandedness: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val palette = rememberGooglePalette(progress.themeMode, progress.highContrast)

    Scaffold(
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Google Account / Stats Bar
            GoogleTopAccountBar(
                progress = progress,
                palette = palette,
                onOpenSettings = { showSettingsDialog = true }
            )

            // Centered Google-style Homepage Wordmark + Search Bar
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GoogleGeminiWordmark(palette = palette)

                Spacer(modifier = Modifier.height(12.dp))

                // Authentic Pill Search Bar
                Surface(
                    color = palette.surfaceGray,
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(4.dp, RoundedCornerShape(28.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = palette.textSub,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search Web Sectors, Bosses, or Upgrades...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSub,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textDark),
                                cursorBrush = SolidColor(palette.activeBlue),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = GeminiBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = GoogleGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Google Homepage Quick Action Buttons ("Play Stage" & "I'm Feeling Lucky")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val unlockedSectors = WebSectors.all.filter { progress.totalStars >= it.unlockRequirementStars }
                    Button(
                        onClick = { onLaunchSector(unlockedSectors.lastOrNull() ?: WebSectors.all.first()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.surfaceGray,
                            contentColor = palette.textDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = palette.activeBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Google Search Battle", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }

                    OutlinedButton(
                        onClick = {
                            val randomSector = unlockedSectors.randomOrNull() ?: WebSectors.all.first()
                            onLaunchSector(randomSector)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = palette.surfaceGray,
                            contentColor = palette.textDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Rounded.Casino, contentDescription = null, tint = VirusCrimson, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("I'm Feeling Lucky", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Google Search Tab Strip
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GoogleSearchTab(
                    label = "All Sectors",
                    icon = Icons.Rounded.Explore,
                    selected = selectedTab == 0,
                    palette = palette,
                    testTag = "tab_sectors",
                    onClick = { selectedTab = 0 }
                )
                GoogleSearchTab(
                    label = "Tech Lab",
                    icon = Icons.Rounded.Memory,
                    selected = selectedTab == 1,
                    palette = palette,
                    testTag = "tab_tech_lab",
                    onClick = { selectedTab = 1 }
                )
                GoogleSearchTab(
                    label = "Threat Codex",
                    icon = Icons.Rounded.Security,
                    selected = selectedTab == 2,
                    palette = palette,
                    testTag = "tab_codex",
                    onClick = { selectedTab = 2 }
                )
            }
            HorizontalDivider(color = palette.border, thickness = palette.borderWidth)

            // Search Results Feed
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .background(palette.surfaceGray)
            ) {
                when (selectedTab) {
                    0 -> SectorsTab(
                        progress = progress,
                        palette = palette,
                        filterQuery = searchQuery,
                        onLaunchSector = onLaunchSector
                    )
                    1 -> TechLabTab(
                        progress = progress,
                        palette = palette,
                        onPurchaseUpgrade = onPurchaseUpgrade,
                        onAllocateSkillPoint = onAllocateSkillPoint
                    )
                    else -> CodexTab(progress = progress, palette = palette)
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsAndCreditsDialog(
            progress = progress,
            palette = palette,
            onDismiss = { showSettingsDialog = false },
            onSetThemeMode = onSetThemeMode,
            onSetHighContrast = onSetHighContrast,
            onSetLargeTouchTargets = onSetLargeTouchTargets,
            onSetReducedMotion = onSetReducedMotion,
            onToggleHandedness = onToggleHandedness
        )
    }
}

@Composable
private fun GoogleTopAccountBar(
    progress: PlayerProgress,
    palette: GooglePalette,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            GeminiCharacterEmblem(
                size = 42.dp,
                modifier = Modifier.testTag("hub_gemini_emblem")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Gemini Lv.${progress.level}",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Google-verse Knight",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = palette.surfaceGray,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(palette.borderWidth, GoogleGold)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonetizationOn,
                        contentDescription = "Google Coins",
                        tint = GoogleGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${progress.googleCoins} G",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textDark
                    )
                }
            }

            Surface(
                color = palette.surfaceGray,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.activeBlue)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "User Rating Stars",
                        tint = GoogleGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${progress.totalStars}/${progress.maxPossibleStars}",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textDark
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceGray)
                    .border(palette.borderWidth, palette.border, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings, Accessibility, and Credits",
                    tint = palette.textDark
                )
            }
        }
    }
}

@Composable
private fun GoogleGeminiWordmark(palette: GooglePalette) {
    val wordmark = buildAnnotatedString {
        withStyle(SpanStyle(color = GeminiBlue)) { append("G") }
        withStyle(SpanStyle(color = VirusCrimson)) { append("e") }
        withStyle(SpanStyle(color = GoogleGold)) { append("m") }
        withStyle(SpanStyle(color = GeminiBlue)) { append("i") }
        withStyle(SpanStyle(color = GoogleGreen)) { append("n") }
        withStyle(SpanStyle(color = VirusCrimson)) { append("i") }
        withStyle(SpanStyle(color = palette.textSub, fontSize = 18.sp, fontWeight = FontWeight.Medium)) {
            append("  Odyssey")
        }
    }
    Text(
        text = wordmark,
        style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, letterSpacing = (-0.8).sp)
    )
}

@Composable
private fun GoogleSearchTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    palette: GooglePalette,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(top = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) palette.activeBlue else palette.textSub,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) palette.activeBlue else palette.textSub
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(3.dp)
                .background(if (selected) palette.activeBlue else Color.Transparent)
        )
    }
}

@Composable
private fun SectorsTab(
    progress: PlayerProgress,
    palette: GooglePalette,
    filterQuery: String,
    onLaunchSector: (SectorDefinition) -> Unit
) {
    val filtered = remember(filterQuery) {
        if (filterQuery.isBlank()) WebSectors.all
        else WebSectors.all.filter {
            it.name.contains(filterQuery, ignoreCase = true) ||
                it.userSearchQuery.contains(filterQuery, ignoreCase = true) ||
                it.bossName.contains(filterQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "About ${filtered.size} Web Dimensions (0.04 seconds)",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub
            )
        }

        items(filtered, key = { it.id }) { sector ->
            val earnedStars = progress.sectorStars[sector.id] ?: 0
            val unlocked = progress.totalStars >= sector.unlockRequirementStars
            SectorSearchResultCard(
                sector = sector,
                earnedStars = earnedStars,
                unlocked = unlocked,
                palette = palette,
                onLaunch = { onLaunchSector(sector) }
            )
        }
    }
}

@Composable
private fun SectorSearchResultCard(
    sector: SectorDefinition,
    earnedStars: Int,
    unlocked: Boolean,
    palette: GooglePalette,
    onLaunch: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.bg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(sector.primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(sector.primaryColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "https://web.googleverse.gg › ${sector.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.urlGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= earnedStars) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = null,
                            tint = if (i <= earnedStars) GoogleGold else palette.border,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${sector.name} — ${sector.subtitle}",
                style = MaterialTheme.typography.titleLarge,
                color = if (unlocked) palette.linkBlue else palette.textSub
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "User Query: ${sector.userSearchQuery} • ${sector.difficultyLabel} • Boss: ${sector.bossName}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSub
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = palette.surfaceGray,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border)
                ) {
                    Text(
                        text = "Bonus: +${sector.coinBonus} G",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                if (unlocked) {
                    Button(
                        onClick = onLaunch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.activeBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("launch_${sector.id}")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ENTER STAGE", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Surface(
                        color = palette.surfaceGray,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = palette.textSub, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Need ${sector.unlockRequirementStars} ★", style = MaterialTheme.typography.labelLarge, color = palette.textSub)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TechLabTab(
    progress: PlayerProgress,
    palette: GooglePalette,
    onPurchaseUpgrade: (UpgradeType) -> Unit,
    onAllocateSkillPoint: (StatType) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.bg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.activeBlue.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GEMINI LEVEL ${progress.level}",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.activeBlue
                        )
                        Surface(
                            color = if (progress.skillPoints > 0) GoogleGold else palette.surfaceGray,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${progress.skillPoints} SKILL PTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (progress.skillPoints > 0) Color(0xFF202124) else palette.textDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "XP: ${progress.xp} / ${progress.xpForNextLevel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSub
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (progress.xp.toFloat() / progress.xpForNextLevel.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape),
                        color = palette.activeBlue,
                        trackColor = palette.surfaceGray
                    )
                }
            }
        }

        item {
            Text(
                text = "XP STAT UPGRADES",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub
            )
        }

        items(StatType.entries) { stat ->
            val currentVal = when (stat) {
                StatType.ATTACK -> progress.attackStat
                StatType.DEFENSE -> progress.defenseStat
                StatType.STAMINA -> progress.staminaStat
                StatType.HEALTH -> progress.healthStat
            }
            val color = when (stat) {
                StatType.ATTACK -> VirusCrimson
                StatType.DEFENSE -> GeminiBlue
                StatType.STAMINA -> GoogleGold
                StatType.HEALTH -> GoogleGreen
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.bg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${stat.title} (Lv.$currentVal)",
                            style = MaterialTheme.typography.titleMedium,
                            color = color
                        )
                        Text(
                            text = stat.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSub
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onAllocateSkillPoint(stat) },
                        enabled = progress.skillPoints > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color,
                            contentColor = Color.White,
                            disabledContainerColor = palette.surfaceGray,
                            disabledContentColor = palette.textSub
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("stat_${stat.name.lowercase()}")
                    ) {
                        Text("+1 PT", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "GOOGLE TECH LAB • COIN UPGRADES",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub
            )
        }

        items(UpgradeType.entries) { type ->
            val level = progress.upgradeLevel(type)
            val cost = type.costForLevel(level)
            val isMax = level >= type.maxLevel
            val canAfford = progress.googleCoins >= cost && !isMax

            val (icon, color) = when (type) {
                UpgradeType.TPU_CORE -> Icons.Rounded.AutoAwesome to GeminiBlue
                UpgradeType.CONTEXT_WINDOW -> Icons.Rounded.Shield to GoogleGreen
                UpgradeType.FIBER_OPTICS -> Icons.Rounded.Speed to VirusCrimson
                UpgradeType.NEURAL_MAGNET -> Icons.Rounded.MonetizationOn to GoogleGold
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = palette.bg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = color.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = type.title, style = MaterialTheme.typography.titleMedium, color = palette.textDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "LVL $level/${type.maxLevel}", style = MaterialTheme.typography.labelSmall, color = color)
                        }
                        Text(text = type.subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSub)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { level.toFloat() / type.maxLevel.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = color,
                            trackColor = palette.surfaceGray
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { onPurchaseUpgrade(type) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.activeBlue,
                            contentColor = Color.White,
                            disabledContainerColor = palette.surfaceGray,
                            disabledContentColor = palette.textSub
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("upgrade_${type.name.lowercase()}")
                    ) {
                        Text(
                            text = if (isMax) "MAX" else "$cost G",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodexTab(progress: PlayerProgress, palette: GooglePalette) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "KNOWLEDGE PANEL • SERVICE RECORD",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.bg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CodexStat("Missions Completed", "${progress.missionsCompleted}", palette.activeBlue, palette)
                    CodexStat("Total Google Coins Earned", "${progress.totalCoinsCollected} G", GoogleGold, palette)
                    CodexStat("Total User Rating Stars", "${progress.totalStars} / ${progress.maxPossibleStars} ★", GoogleGreen, palette)
                }
            }
        }

        item {
            Text(
                text = "WEB THREAT INTEL",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub
            )
        }

        item {
            ThreatIntelCard(
                name = "Trojan Imp",
                description = "Aggressive crimson imp unit that rushes Gemini Knight on sight.",
                count = progress.virusesPurged,
                icon = Icons.Rounded.BugReport,
                color = VirusCrimson,
                palette = palette
            )
        }
        item {
            ThreatIntelCard(
                name = "Ad-Ware Bot",
                description = "3D hover-bot that orbits at mid-range and launches distracting spam bolts.",
                count = progress.adwareBlocked,
                icon = Icons.Rounded.WebAsset,
                color = AdwareMagenta,
                palette = palette
            )
        }
        item {
            ThreatIntelCard(
                name = "Hacker Rogue",
                description = "Erratic green glitch-runner that flanks rapidly and drops bonus Google Coins.",
                count = progress.hackersOutsmarted,
                icon = Icons.Rounded.Terminal,
                color = HackerNeonGreen,
                palette = palette
            )
        }
    }
}

@Composable
private fun CodexStat(label: String, value: String, color: Color, palette: GooglePalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = palette.textSub)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun ThreatIntelCard(
    name: String,
    description: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    palette: GooglePalette
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.bg),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = color.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = name, tint = color, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium, color = palette.textDark)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = palette.textSub)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "$count", style = MaterialTheme.typography.titleLarge, color = color)
                Text(text = "PURGED", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
            }
        }
    }
}

@Composable
private fun SettingsAndCreditsDialog(
    progress: PlayerProgress,
    palette: GooglePalette,
    onDismiss: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSetLargeTouchTargets: (Boolean) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onToggleHandedness: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.bg),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AccessibilityNew,
                            contentDescription = null,
                            tint = palette.activeBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Settings & Credits",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textDark
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_settings_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Settings", tint = palette.textSub)
                    }
                }

                HorizontalDivider(color = palette.border, thickness = palette.borderWidth)

                Text(
                    text = "VISUAL THEME",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = progress.themeMode == mode
                        OutlinedButton(
                            onClick = { onSetThemeMode(mode) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) palette.activeBlue else palette.surfaceGray,
                                contentColor = if (selected) Color.White else palette.textDark
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                palette.borderWidth,
                                if (selected) palette.activeBlue else palette.border
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("theme_${mode.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ThemeMode.SYSTEM -> Icons.Rounded.AutoAwesome
                                    ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                    ThemeMode.DARK -> Icons.Rounded.DarkMode
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(mode.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }

                Text(
                    text = "ACCESSIBILITY & CONTROLS",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub
                )

                AccessibilityToggleRow(
                    title = "High Contrast UI",
                    subtitle = "Stronger borders and maximum text contrast",
                    checked = progress.highContrast,
                    palette = palette,
                    testTag = "toggle_high_contrast",
                    onCheckedChange = onSetHighContrast
                )
                AccessibilityToggleRow(
                    title = "Large Touch Targets",
                    subtitle = "Enlarges joystick and action buttons in combat",
                    checked = progress.largeTouchTargets,
                    palette = palette,
                    testTag = "toggle_large_touch",
                    onCheckedChange = onSetLargeTouchTargets
                )
                AccessibilityToggleRow(
                    title = "Reduced Motion",
                    subtitle = "Disables idle floating and bobbing animations",
                    checked = progress.reducedMotion,
                    palette = palette,
                    testTag = "toggle_reduced_motion",
                    onCheckedChange = onSetReducedMotion
                )
                AccessibilityToggleRow(
                    title = "Left-Handed Controls",
                    subtitle = "Places action buttons left and joystick right",
                    checked = progress.isLeftHanded,
                    palette = palette,
                    testTag = "toggle_left_handed",
                    onCheckedChange = onToggleHandedness
                )

                HorizontalDivider(color = palette.border, thickness = palette.borderWidth)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = palette.activeBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CREDITS & ATTRIBUTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSub
                    )
                }

                Surface(
                    color = palette.surfaceGray,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(palette.borderWidth, palette.border)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Google & Gemini Likeness Attribution",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textDark
                        )
                        Text(
                            text = "Google, Gemini, Google Search UI motifs, and the four-color palette (Blue, Red, Yellow, Green) are trademarks and likenesses of Google LLC. This application is an independent fan-tribute game created for entertainment and is not affiliated with, sponsored by, or endorsed by Google LLC.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSub
                        )
                        Text(
                            text = "Typography & Icons: Space Grotesk, Plus Jakarta Sans (Google Fonts), and Material Symbols.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSub
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessibilityToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    palette: GooglePalette,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = palette.textDark)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
