package com.example.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.model.EqualizerPreset
import com.example.core.model.PlayerUiState
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfessionalEqualizerDialog(
    playerState: PlayerUiState,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSetPreset: (EqualizerPreset) -> Unit,
    onSetBandLevel: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "ECUALIZADOR",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                            color = TextPrimary
                        )
                        Text(
                            "Ajuste de audio profesional",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.GraphicEq, null, tint = accentColor)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Presets Quick Switch
                Text("Preajustes Rápidos", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EqualizerPreset.values()) { preset ->
                        FilterChip(
                            selected = playerState.equalizerPreset == preset,
                            onClick = { onSetPreset(preset) },
                            label = { Text(preset.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bands Visualization Curve
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    FrequencyCurve(playerState.bandLevels, accentColor)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Frequency Bands Sliders
                Text("Bandas de Frecuencia (mB)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val bands = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
                    bands.forEachIndexed { index, label ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            val level = playerState.bandLevels[index] ?: 0
                            VerticalSlider(
                                value = (level + 1500) / 3000f,
                                onValueChange = { onSetBandLevel(index, (it * 3000 - 1500).toInt()) },
                                accentColor = accentColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(label, fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Additional Effects
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EffectCard(
                        title = "Bass Boost",
                        value = playerState.bassBoostStrength / 1000f,
                        onValueChange = { onSetBassBoost((it * 1000).toInt()) },
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    EffectCard(
                        title = "Virtualizer",
                        value = playerState.virtualizerStrength / 1000f,
                        onValueChange = { onSetVirtualizer((it * 1000).toInt()) },
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("APLICAR", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .height(160.dp)
            .width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.4f))))
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = -90f
                    translationY = with(density) { -80.dp.toPx() }
                }
                .width(160.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}

@Composable
fun EffectCard(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor
            )
        )
        Text(
            "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = accentColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun FrequencyCurve(bandLevels: Map<Int, Int>, accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val width = size.width
        val height = size.height
        val midY = height / 2

        val points = (0..4).map { i ->
            val level = bandLevels[i] ?: 0
            val x = i * (width / 4)
            val y = midY - (level / 1500f) * (height / 2)
            x to y
        }

        path.moveTo(0f, points[0].second)
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i+1]
            path.cubicTo(
                p1.first + (p2.first - p1.first) / 2, p1.second,
                p1.first + (p2.first - p1.first) / 2, p2.second,
                p2.first, p2.second
            )
        }

        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
