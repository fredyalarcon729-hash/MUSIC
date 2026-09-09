package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.EqualizerPreset
import com.example.core.model.VisualizerStyle
import com.example.core.model.PlayerUiState
import com.example.ui.player.ProfessionalEqualizerDialog
import com.example.ui.player.SleepTimerDialog
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SettingsScreen(
    playerState: PlayerUiState,
    isScanning: Boolean,
    themeMode: String,
    visualizerStyle: VisualizerStyle,
    customAccentColor: Long,
    filterVoiceNotes: Boolean,
    filterDuplicates: Boolean,
    isAmbientAuraEnabled: Boolean,
    ambientAuraStyle: com.example.core.model.AmbientAuraStyle,
    ambientAuraIntensity: Float,
    ambientAuraWeight: Float,
    isIgnoreAudioFocusEnabled: Boolean,
    isMultiAudioSupported: Boolean,
    isSyncingCloud: Boolean,
    onRescan: () -> Unit,
    onSyncCloud: () -> Unit,
    onSelectEqualizer: (com.example.core.model.EqualizerPreset) -> Unit,
    onSetBandLevel: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onSelectSleepTimer: (Int?) -> Unit,
    onToggleSkipSilence: (Boolean) -> Unit,
    onToggleFilterVoiceNotes: (Boolean) -> Unit,
    onToggleFilterDuplicates: (Boolean) -> Unit,
    onToggleAmbientAura: (Boolean) -> Unit,
    onToggleIgnoreAudioFocus: (Boolean) -> Unit,
    onSetAmbientAuraStyle: (com.example.core.model.AmbientAuraStyle) -> Unit,
    onSetAmbientAuraIntensity: (Float) -> Unit,
    onSetAmbientAuraWeight: (Float) -> Unit,
    onSetVisualizerStyle: (com.example.core.model.VisualizerStyle) -> Unit,
    onSetCustomAccentColor: (Long) -> Unit,
    onSetThemeMode: (String) -> Unit,
    onNavigateToServices: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEqDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // External Services Hub Banner
            item {
                Surface(
                    onClick = onNavigateToServices,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurpleLight.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("settings_services_item")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonPurpleLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Hub, null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Servicios y Plataformas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "YouTube, Spotify, TIDAL, Deezer y fuentes locales",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                fontSize = 12.sp
                            )
                        }

                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Appearance Section
            item {
                Text(
                    text = "APARIENCIA Y PERSONALIZACIÓN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tema de la Aplicación",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeOption(
                                title = "Claro",
                                icon = Icons.Default.LightMode,
                                isSelected = themeMode == "light",
                                onClick = { onSetThemeMode("light") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeOption(
                                title = "Oscuro",
                                icon = Icons.Default.DarkMode,
                                isSelected = themeMode == "dark",
                                onClick = { onSetThemeMode("dark") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeOption(
                                title = "Sistema",
                                icon = Icons.Default.Settings,
                                isSelected = themeMode == "system",
                                onClick = { onSetThemeMode("system") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Estilo de Visualizador (Ondas)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(180.dp) // Increased height for more styles
                        ) {
                            items(VisualizerStyle.entries.size) { index ->
                                val style = VisualizerStyle.entries[index]
                                val isSelected = visualizerStyle == style
                                Surface(
                                    onClick = { onSetVisualizerStyle(style) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = style.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Accent Color Section
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Color de Acento",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fija un color o usa el automático (carátula)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val colors = listOf(
                                0L to "Auto",
                                0xFF00E5FF to "Cyan",
                                0xFFFF007F to "Rosa",
                                0xFFBB86FC to "Violeta",
                                0xFF00FF00 to "Verde",
                                0xFFFFD700 to "Oro",
                                0xFFFF4500 to "Naranja"
                            )
                            
                            items(colors.size) { index ->
                                val (colorLong, name) = colors[index]
                                val isSelected = customAccentColor == colorLong
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { onSetCustomAccentColor(colorLong) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (colorLong == 0L) Color.Gray.copy(alpha = 0.3f) else Color(colorLong))
                                            .border(2.dp, if (isSelected) Color.White else Color.Transparent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (colorLong == 0L) Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(name, fontSize = 10.sp, color = if (isSelected) Color.White else TextTertiary)
                                }
                            }
                        }
                    }
                }
            }

            // Audio & Playback Section
            item {
                Text(
                    text = "AUDIO Y REPRODUCCIÓN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                SettingsOptionItem(
                    icon = Icons.Default.Equalizer,
                    title = "Perfil de Ecualizador",
                    subtitle = playerState.equalizerPreset.displayName,
                    onClick = { showEqDialog = true }
                )
            }

            item {
                SettingsOptionItem(
                    icon = Icons.Default.Timer,
                    title = "Temporizador de Apagado",
                    subtitle = if (playerState.sleepTimerMinutesLeft != null) "${playerState.sleepTimerMinutesLeft} minutos restantes" else "Desactivado",
                    onClick = { showTimerDialog = true }
                )
            }

            item {
                SettingsOptionItem(
                    icon = Icons.Default.Tune,
                    title = "Calidad de Salida",
                    subtitle = "${playerState.audioQuality.label} (${playerState.audioQuality.bitrate})",
                    onClick = {}
                )
            }

            // Audio Pro Section
            item {
                Text(
                    text = "AJUSTES DE AUDIO PRO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Omitir Silencios", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Recorta silencios al inicio/fin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = playerState.isSkipSilenceEnabled,
                                onCheckedChange = onToggleSkipSilence,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (!isMultiAudioSupported) Modifier.graphicsLayer { alpha = 0.5f } else Modifier)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reproducción Ininterrumpida",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isMultiAudioSupported) MaterialTheme.colorScheme.onSurface else TextTertiary
                                )
                                Text(
                                    text = if (isMultiAudioSupported) "La música no se detiene en llamadas" else "Dispositivo no compatible con multi-audio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isMultiAudioSupported) MaterialTheme.colorScheme.onSurfaceVariant else NeonPink.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = isIgnoreAudioFocusEnabled,
                                onCheckedChange = onToggleIgnoreAudioFocus,
                                enabled = isMultiAudioSupported,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonCyan,
                                    disabledCheckedTrackColor = Color.Gray,
                                    disabledUncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Iluminación Ambiental", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Efecto Aura reactivo a los bajos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isAmbientAuraEnabled,
                                onCheckedChange = onToggleAmbientAura,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonCyan
                                )
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(visible = isAmbientAuraEnabled) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Style Selection
                                Text("Estilo del Aura", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(com.example.core.model.AmbientAuraStyle.entries.size) { index ->
                                        val style = com.example.core.model.AmbientAuraStyle.entries[index]
                                        val isSelected = ambientAuraStyle == style
                                        Surface(
                                            onClick = { onSetAmbientAuraStyle(style) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) NeonCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else Color.Transparent)
                                        ) {
                                            Text(
                                                text = style.displayName,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Intensity Slider
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Intensidad", style = MaterialTheme.typography.labelMedium, color = TextTertiary, modifier = Modifier.width(80.dp))
                                    androidx.compose.material3.Slider(
                                        value = ambientAuraIntensity,
                                        onValueChange = onSetAmbientAuraIntensity,
                                        valueRange = 0.1f..0.8f,
                                        modifier = Modifier.weight(1f),
                                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                    )
                                }

                                // Weight Slider
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Grosor", style = MaterialTheme.typography.labelMedium, color = TextTertiary, modifier = Modifier.width(80.dp))
                                    androidx.compose.material3.Slider(
                                        value = ambientAuraWeight,
                                        onValueChange = onSetAmbientAuraWeight,
                                        valueRange = 0.2f..1.2f,
                                        modifier = Modifier.weight(1f),
                                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Media Scanning Section
            item {
                Text(
                    text = "BIBLIOTECA LOCAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Refresh, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reescanear Almacenamiento",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Actualiza canciones añadidas recientemente al teléfono",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = onRescan,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonPurpleLight,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isScanning) "Escaneando..." else "Escanear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Cloud Sync Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.theme.NeonPink.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Refresh, null, tint = com.example.ui.theme.NeonPink, modifier = Modifier.size(22.dp))
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sincronizar Nube (Firebase)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Busca nuevos archivos .mp3 subidos a tu bucket",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = onSyncCloud,
                                enabled = !isSyncingCloud,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = com.example.ui.theme.NeonPink,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isSyncingCloud) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Sincronizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Limpiar Librería",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Oculta notas de voz y audios de chats (WhatsApp, etc.)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = filterVoiceNotes,
                                onCheckedChange = onToggleFilterVoiceNotes,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonCyan
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Evitar Duplicados",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Filtra canciones con mismo nombre y duración",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = filterDuplicates,
                                onCheckedChange = onToggleFilterDuplicates,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonCyan
                                )
                            )
                        }
                    }
                }
            }

            // About Section
            item {
                Text(
                    text = "ACERCA DE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val packageInfo = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (e: Exception) {
                        null
                    }
                }
                val versionName = packageInfo?.versionName ?: "1.0"

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Author Item
                        Row(
                            modifier = Modifier
                                .clickable { /* Open portfolio or social if needed */ }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonPurpleLight.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = NeonPurpleLight, modifier = Modifier.size(24.dp))
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Autor",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fredy Alarcón Ordoñez",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Version Item
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Info, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Versión de la Aplicación",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "v$versionName",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEqDialog) {
        ProfessionalEqualizerDialog(
            playerState = playerState,
            accentColor = NeonCyan,
            onDismiss = { showEqDialog = false },
            onSetPreset = onSelectEqualizer,
            onSetBandLevel = onSetBandLevel,
            onSetBassBoost = onSetBassBoost,
            onSetVirtualizer = onSetVirtualizer
        )
    }

    if (showTimerDialog) {
        SleepTimerDialog(
            currentMinutes = playerState.sleepTimerMinutesLeft,
            onDismiss = { showTimerDialog = false },
            onSelect = {
                onSelectSleepTimer(it)
                showTimerDialog = false
            }
        )
    }
}

@Composable
fun ThemeModeOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NeonCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) NeonCyan else Color.Transparent
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    fontSize = 12.sp
                )
            }

            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}
