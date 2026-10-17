package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode(val displayName: String, val description: String) {
    SLATE("Clean Slate", "Čisté břidlicové téma s cyan akcenty"),
    OBSIDIAN("Obsidian Black", "Pravá OLED černá s kontrastním stříbrem"),
    CYBERPUNK("Cyberpunk Neon", "Půlnoční fialová s neonovou cyan & růžovou"),
    EMERALD("Forest Emerald", "Hluboká lesní zeleň s jadeitovým smaragdem")
}

// 1. Clean Slate Themes
private val SlateDarkColorScheme = darkColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = Slate100,
    secondary = Slate400,
    onSecondary = Slate950,
    secondaryContainer = Slate850,
    onSecondaryContainer = Slate200,
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = Slate800,
    error = RoseError,
    onError = Color.White
)

private val SlateLightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate900,
    secondary = Slate600,
    onSecondary = Color.White,
    secondaryContainer = Slate200,
    onSecondaryContainer = Slate800,
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    outlineVariant = Slate200,
    error = RoseError,
    onError = Color.White
)

// 2. Obsidian Black (True OLED Dark)
private val ObsidianDarkColorScheme = darkColorScheme(
    primary = ObsidianSilver,
    onPrimary = ObsidianBlack,
    primaryContainer = ObsidianCard,
    onPrimaryContainer = Color.White,
    secondary = ObsidianMuted,
    onSecondary = ObsidianBlack,
    secondaryContainer = ObsidianSurface,
    onSecondaryContainer = ObsidianSilver,
    tertiary = CyanPrimaryDark,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = Color.White,
    surface = ObsidianSurface,
    onSurface = Color.White,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = ObsidianSilver,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorder,
    error = RoseError,
    onError = Color.White
)

private val ObsidianLightColorScheme = lightColorScheme(
    primary = ObsidianBlack,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEEEEE),
    onPrimaryContainer = ObsidianBlack,
    secondary = Color(0xFF555555),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = ObsidianBlack,
    tertiary = CyanPrimary,
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = ObsidianBlack,
    surface = Color.White,
    onSurface = ObsidianBlack,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF333333),
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),
    error = RoseError,
    onError = Color.White
)

// 3. Cyberpunk Neon Themes
private val CyberpunkDarkColorScheme = darkColorScheme(
    primary = CyberpunkNeonCyan,
    onPrimary = CyberpunkBackground,
    primaryContainer = CyberpunkCard,
    onPrimaryContainer = CyberpunkNeonCyan,
    secondary = CyberpunkNeonPink,
    onSecondary = Color.White,
    secondaryContainer = CyberpunkSurface,
    onSecondaryContainer = CyberpunkNeonPink,
    tertiary = CyberpunkNeonYellow,
    onTertiary = CyberpunkBackground,
    background = CyberpunkBackground,
    onBackground = Color(0xFFF3E8FF),
    surface = CyberpunkSurface,
    onSurface = Color(0xFFF3E8FF),
    surfaceVariant = CyberpunkCard,
    onSurfaceVariant = Color(0xFFD8B4FE),
    outline = CyberpunkBorder,
    outlineVariant = CyberpunkBorder,
    error = CyberpunkNeonPink,
    onError = Color.White
)

private val CyberpunkLightColorScheme = lightColorScheme(
    primary = Color(0xFF7000FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFFDB2777),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF831843),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = Color(0xFFFAF5FF),
    onBackground = Color(0xFF1E1B4B),
    surface = Color.White,
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFF3E8FF),
    onSurfaceVariant = Color(0xFF581C87),
    outline = Color(0xFFD8B4FE),
    outlineVariant = Color(0xFFE9D5FF),
    error = RoseError,
    onError = Color.White
)

// 4. Forest Emerald Themes
private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldBackground,
    primaryContainer = EmeraldCard,
    onPrimaryContainer = EmeraldMint,
    secondary = EmeraldMint,
    onSecondary = EmeraldBackground,
    secondaryContainer = EmeraldSurface,
    onSecondaryContainer = EmeraldMint,
    tertiary = AmberWarning,
    onTertiary = EmeraldBackground,
    background = EmeraldBackground,
    onBackground = Color(0xFFECFDF5),
    surface = EmeraldSurface,
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = EmeraldCard,
    onSurfaceVariant = Color(0xFFA7F3D0),
    outline = EmeraldBorder,
    outlineVariant = EmeraldBorder,
    error = RoseError,
    onError = Color.White
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFDF5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = AmberWarning,
    onTertiary = Color.White,
    background = Color(0xFFF0FDF4),
    onBackground = Color(0xFF064E3B),
    surface = Color.White,
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFE6F4EA),
    onSurfaceVariant = Color(0xFF047857),
    outline = Color(0xFFA7F3D0),
    outlineVariant = Color(0xFFD1FAE5),
    error = RoseError,
    onError = Color.White
)

@Composable
fun VertexAiTheme(
    appTheme: String = "SLATE",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme.uppercase()) {
        "OBSIDIAN" -> if (darkTheme) ObsidianDarkColorScheme else ObsidianLightColorScheme
        "CYBERPUNK" -> if (darkTheme) CyberpunkDarkColorScheme else CyberpunkLightColorScheme
        "EMERALD" -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
        else -> if (darkTheme) SlateDarkColorScheme else SlateLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
