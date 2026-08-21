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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.auth.AuthManager
import com.example.core.model.MusicSource
import com.example.core.source.ExternalServiceDescriptor
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
fun ServicesScreen(
    services: List<ExternalServiceDescriptor>,
    youtubeApiKey: String,
    googleClientId: String,
    userSession: AuthManager.UserSession?,
    onUpdateYouTubeApiKey: (String) -> Unit,
    onUpdateGoogleClientId: (String) -> Unit,
    onToggleService: (MusicSource, Boolean) -> Unit,
    onSignIn: (android.content.Context, String?) -> Unit,
    onSignOut: () -> Unit,
    onGetDiagnosticInfo: () -> AuthManager.DiagnosticInfo,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showApiKeyDialog) {
        YouTubeApiKeyDialog(
            currentKey = youtubeApiKey,
            currentClientId = googleClientId,
            userSession = userSession,
            onDismiss = { showApiKeyDialog = false },
            onSave = { key, clientId ->
                onUpdateYouTubeApiKey(key)
                onUpdateGoogleClientId(clientId)
                showApiKeyDialog = false
            },
            onSignIn = { clientId -> onSignIn(context, clientId) },
            onSignOut = onSignOut
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("services_back_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column {
                Text(
                    text = "Servicios y Plataformas",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showDiagnosticDialog = true }
                ) {
                    Text(
                        text = "Arquitectura modular",
                        style = MaterialTheme.typography.bodyMedium.copy(color = NeonCyan),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, null, tint = NeonCyan.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User Session Header Block
            item(key = "user_profile_header") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (userSession != null) NeonCyan.copy(alpha = 0.05f) else ObsidianSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (userSession != null) NeonCyan.copy(alpha = 0.3f) else ObsidianBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (userSession != null) {
                            AsyncImage(
                                model = userSession.photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianCard),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userSession.displayName ?: "Usuario Conectado",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = userSession.email ?: "Google Auth Activo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan
                                )
                            }
                            IconButton(onClick = onSignOut) {
                                Icon(Icons.Default.Lock, "Cerrar Sesión", tint = Color(0xFFFF4D4D))
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, null, tint = TextTertiary, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sin cuenta vinculada",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Conecta Google para mejorar YouTube",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            TextButton(onClick = { showApiKeyDialog = true }) {
                                Text("CONECTAR", color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Architecture Info Box
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurpleLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Fusion Music utiliza una arquitectura de proveedores aislados (MusicSourceProvider). Cada plataforma se integra mediante sus SDKs oficiales autorizados (OAuth2, App Remote y APIs REST oficiales).",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                lineHeight = 18.sp,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            items(services, key = { it.source.name }) { service ->
                val badgeColor = Color(service.badgeColorHex)
                val isYouTube = service.source == MusicSource.YOUTUBE

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (service.isConnected || (isYouTube && userSession != null)) badgeColor.copy(alpha = 0.5f) else ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = service.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = TextPrimary
                                    )
                                    if (isYouTube && userSession != null) {
                                        Text(
                                            text = "Sesión activa: ${userSession.email}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeonCyan
                                        )
                                    }
                                }
                            }

                        if (service.source == MusicSource.LOCAL) {
                            Surface(
                                color = NeonCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Activo por Defecto",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isYouTube) {
                                    IconButton(
                                        onClick = { showApiKeyDialog = true },
                                        modifier = Modifier.size(32.dp).padding(end = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Configurar API Key",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Switch(
                                    checked = service.isEnabled,
                                    onCheckedChange = { onToggleService(service.source, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = badgeColor,
                                        uncheckedThumbColor = TextTertiary,
                                        uncheckedTrackColor = ObsidianBorder
                                    )
                                )
                            }
                        }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = service.description,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Mecanismo oficial: ${service.sdkType}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = service.officialRequirements,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextTertiary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showApiKeyDialog) {
        YouTubeApiKeyDialog(
            currentKey = youtubeApiKey,
            currentClientId = googleClientId,
            userSession = userSession,
            onDismiss = { showApiKeyDialog = false },
            onSave = { key, clientId ->
                onUpdateYouTubeApiKey(key)
                onUpdateGoogleClientId(clientId)
                showApiKeyDialog = false
            },
            onSignIn = { clientId -> onSignIn(context, clientId) },
            onSignOut = onSignOut
        )
    }

    if (showDiagnosticDialog) {
        val info = onGetDiagnosticInfo()
        AlertDialog(
            onDismissRequest = { showDiagnosticDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Datos de Diagnóstico", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Nombre del Paquete:", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                    Text(info.packageName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Huella Digital SHA-1:", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                    Text(info.sha1, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Copia el SHA-1 y asegúrate de que sea idéntico al que figura en el ID de Android de tu consola Google Cloud.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticDialog = false }) {
                    Text("CERRAR", color = NeonCyan)
                }
            }
        )
    }
}

@Composable
fun YouTubeApiKeyDialog(
    currentKey: String,
    currentClientId: String,
    userSession: AuthManager.UserSession?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onSignIn: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var key by remember { mutableStateOf(currentKey) }
    var clientId by remember { mutableStateOf(currentClientId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                "YouTube & Google Auth",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column {
                if (userSession != null) {
                    // Profile info inside dialog (secondary verification)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianCard)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = userSession.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = userSession.email ?: "Autenticado",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onSignOut) {
                            Text("SALIR", color = Color(0xFFFF4D4D), fontSize = 10.sp)
                        }
                    }
                } else {
                    Surface(
                        onClick = { onSignIn(clientId) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CONECTAR CON GOOGLE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Configuración técnica de API",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("YouTube API Key (v3)") },
                    placeholder = { Text("AIza...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("OAuth Client ID (Web)") },
                    placeholder = { Text("...apps.googleusercontent.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "El Client ID es obligatorio para el inicio de sesión con Google.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(key, clientId) }) {
                Text("GUARDAR", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextTertiary)
            }
        }
    )
}

