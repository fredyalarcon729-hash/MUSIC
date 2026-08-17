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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.EqualizerPreset
import com.example.core.model.PlayerUiState
import com.example.ui.player.EqualizerDialog
import com.example.ui.player.SleepTimerDialog
import com.example.ui.theme.NeonCyan
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
    onRescan: () -> Unit,
    onSelectEqualizer: (EqualizerPreset) -> Unit,
    onSelectSleepTimer: (Int?) -> Unit,
    onNavigateToServices: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEqDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = TextPrimary,
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
                    color = ObsidianSurfaceVariant,
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
                                color = TextPrimary
                            )
                            Text(
                                text = "YouTube, Spotify, TIDAL, Deezer y fuentes locales",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                fontSize = 12.sp
                            )
                        }

                        Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
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
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
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
                                color = TextPrimary
                            )
                            Text(
                                text = "Actualiza canciones añadidas recientemente al teléfono",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
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
                }
            }

            // Android Auto & Background Playback Card
            item {
                Text(
                    text = "INTEGRACIÓN EN VEHÍCULOS",
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
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
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
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Android Auto & MediaSession",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Listo para reproducir en pantalla del auto, controles al volante y Bluetooth.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEqDialog) {
        EqualizerDialog(
            currentPreset = playerState.equalizerPreset,
            onDismiss = { showEqDialog = false },
            onSelect = {
                onSelectEqualizer(it)
                showEqDialog = false
            }
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
fun SettingsOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
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
                    .background(ObsidianCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    fontSize = 12.sp
                )
            }

            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
        }
    }
}
