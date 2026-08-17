package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class FusionNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("home", "Inicio", Icons.Default.Home, "nav_home"),
    LIBRARY("library", "Biblioteca", Icons.Default.LibraryMusic, "nav_library"),
    SEARCH("search", "Buscar", Icons.Default.Search, "nav_search"),
    PLAYLISTS("playlists", "Playlists", Icons.Default.QueueMusic, "nav_playlists"),
    SETTINGS("settings", "Ajustes", Icons.Default.Settings, "nav_settings")
}

@Composable
fun FusionBottomNav(
    currentDestination: FusionNavDestination,
    onNavigateTo: (FusionNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = ObsidianDark,
        tonalElevation = 8.dp,
        modifier = modifier.height(68.dp)
    ) {
        FusionNavDestination.values().forEach { destination ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateTo(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = NeonCyan,
                    indicatorColor = NeonCyan,
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary
                ),
                modifier = Modifier.testTag(destination.testTag)
            )
        }
    }
}
