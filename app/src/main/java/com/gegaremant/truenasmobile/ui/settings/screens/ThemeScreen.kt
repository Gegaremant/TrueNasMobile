package com.gegaremant.truenasmobile.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gegaremant.truenasmobile.data.api.TrueNASApiManager
import com.gegaremant.truenasmobile.data.helpers.PersonalizationManager
import com.gegaremant.truenasmobile.data.helpers.ThemeMode
import com.gegaremant.truenasmobile.ui.components.UnifiedScreenHeader
import com.gegaremant.truenasmobile.ui.theme.AppTheme
import com.gegaremant.truenasmobile.ui.theme.ForestDarkColors
import com.gegaremant.truenasmobile.ui.theme.ForestLightColors
import com.gegaremant.truenasmobile.ui.theme.LavenderDarkColors
import com.gegaremant.truenasmobile.ui.theme.LavenderLightColors
import com.gegaremant.truenasmobile.ui.theme.MonochromeDarkColors
import com.gegaremant.truenasmobile.ui.theme.MonochromeLightColors
import com.gegaremant.truenasmobile.ui.theme.OceanDarkColors
import com.gegaremant.truenasmobile.ui.theme.OceanLightColors
import com.gegaremant.truenasmobile.ui.theme.SunsetDarkColors
import com.gegaremant.truenasmobile.ui.theme.SunsetLightColors
import com.gegaremant.truenasmobile.ui.theme.TrueNasMobileDarkColors
import com.gegaremant.truenasmobile.ui.theme.TrueNasMobileLightColors

@Composable
fun ThemeScreen(
    currentTheme: AppTheme,
    themeMode: ThemeMode,
    onThemeSelected: (AppTheme) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit = {},
    manager: TrueNASApiManager?,
    userKey: String
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    val personalization by PersonalizationManager.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        UnifiedScreenHeader(
            title = "Тема",
            subtitle = "Выберите цветовую схему и режим",
            onDismissError = {},
            onBackPressed = onNavigateBack,
            isLoading = false,
            isRefreshing = false,
            error = null,
            manager = manager!!,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Цветовая схема",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        isSelected = theme == selectedTheme,
                        onClick = {
                            selectedTheme = theme
                            onThemeSelected(theme)
                        }
                    )
                }
            }


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Тёмный режим",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Светлая, тёмная или по умолчанию из системы",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = themeMode == mode
                            Surface(
                                onClick = { onThemeModeSelected(mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Search bar alignment ─────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Search Bar at Bottom",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Align the unified search field to the bottom of the screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = personalization.searchBarBottom,
                        onCheckedChange = { isChecked ->
                            PersonalizationManager.saveSearchBarBottom(context, userKey, isChecked)
                        }
                    )
                }
            }

        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()


    val samplePrimary = when(theme) {
        AppTheme.DYNAMIC -> MaterialTheme.colorScheme.primary
        AppTheme.TRUENASMOBILE -> if (isSystemDark) TrueNasMobileDarkColors.primary else TrueNasMobileLightColors.primary
        AppTheme.OCEAN -> if (isSystemDark) OceanDarkColors.primary else OceanLightColors.primary
        AppTheme.FOREST -> if (isSystemDark) ForestDarkColors.primary else ForestLightColors.primary
        AppTheme.SUNSET -> if (isSystemDark) SunsetDarkColors.primary else SunsetLightColors.primary
        AppTheme.LAVENDER -> if (isSystemDark) LavenderDarkColors.primary else LavenderLightColors.primary
        AppTheme.MONOCHROME -> if (isSystemDark) MonochromeDarkColors.primary else MonochromeLightColors.primary
    }

    val sampleSecondary = when(theme) {
        AppTheme.DYNAMIC -> MaterialTheme.colorScheme.secondary
        AppTheme.TRUENASMOBILE -> if (isSystemDark) TrueNasMobileDarkColors.secondary else TrueNasMobileLightColors.secondary
        AppTheme.OCEAN -> if (isSystemDark) OceanDarkColors.secondary else OceanLightColors.secondary
        AppTheme.FOREST -> if (isSystemDark) ForestDarkColors.secondary else ForestLightColors.secondary
        AppTheme.SUNSET -> if (isSystemDark) SunsetDarkColors.secondary else SunsetLightColors.secondary
        AppTheme.LAVENDER -> if (isSystemDark) LavenderDarkColors.secondary else LavenderLightColors.secondary
        AppTheme.MONOCHROME -> if (isSystemDark) MonochromeDarkColors.secondary else MonochromeLightColors.secondary
    }

    val sampleSurface = when(theme) {
        AppTheme.DYNAMIC -> MaterialTheme.colorScheme.surfaceVariant
        AppTheme.TRUENASMOBILE -> if (isSystemDark) Color(0xFF1E1E1E) else Color(0xFFEAEAEA)
        AppTheme.OCEAN -> if (isSystemDark) Color(0xFF1A2230) else Color(0xFFE5ECF4)
        AppTheme.FOREST -> if (isSystemDark) Color(0xFF1B221A) else Color(0xFFE6EFE5)
        AppTheme.SUNSET -> if (isSystemDark) Color(0xFF261812) else Color(0xFFFBECE6)
        AppTheme.LAVENDER -> if (isSystemDark) Color(0xFF211A26) else Color(0xFFF5EEFA)
        AppTheme.MONOCHROME -> if (isSystemDark) Color(0xFF222222) else Color(0xFFEEEEEE)
    }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.width(130.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, samplePrimary) else null,
            colors = CardDefaults.cardColors(containerColor = sampleSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "Abc",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = samplePrimary
                )


                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(samplePrimary)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(samplePrimary.copy(alpha = 0.6f))
                )

                Spacer(modifier = Modifier.weight(1f))


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) samplePrimary else sampleSecondary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}