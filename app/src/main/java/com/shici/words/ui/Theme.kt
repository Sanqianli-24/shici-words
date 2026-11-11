package com.shici.words.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shici.words.data.AppState

private val Primary = Color(0xFF4C6FFF)
private val PrimaryDark = Color(0xFF8CA0FF)
private val BgLight = Color(0xFFF5F6FA)
private val BgDark = Color(0xFF12141C)

private val LightScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E9FF),
    onPrimaryContainer = Color(0xFF16306E),
    secondary = Color(0xFF22B573),
    background = BgLight,
    onBackground = Color(0xFF1C1E26),
    surface = Color.White,
    onSurface = Color(0xFF1C1E26),
    surfaceVariant = Color(0xFFEEF0F7),
    onSurfaceVariant = Color(0xFF8A8F9E),
    outline = Color(0xFFE3E6EF),
    error = Color(0xFFE5484D)
)

private val DarkScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF10205C),
    primaryContainer = Color(0xFF2A3A78),
    onPrimaryContainer = Color(0xFFDDE5FF),
    secondary = Color(0xFF4CD49A),
    background = BgDark,
    onBackground = Color(0xFFE7E9F2),
    surface = Color(0xFF1C1F2A),
    onSurface = Color(0xFFE7E9F2),
    surfaceVariant = Color(0xFF262A38),
    onSurfaceVariant = Color(0xFF9AA0B2),
    outline = Color(0xFF323748),
    error = Color(0xFFFF6B6E)
)

/** 词性/状态用色 */
object AppColors {
    val Green = Color(0xFF22B573)
    val Orange = Color(0xFFF59E0B)
    val Red = Color(0xFFE5484D)
    val Gold = Color(0xFFF5A623)
    val Gray = Color(0xFF9AA0B2)
    fun green(dark: Boolean) = if (dark) Color(0xFF4CD49A) else Green
    fun orange(dark: Boolean) = if (dark) Color(0xFFFFB84D) else Orange
    fun red(dark: Boolean) = if (dark) Color(0xFFFF8083) else Red
    fun gold(dark: Boolean) = if (dark) Color(0xFFFFC94D) else Gold
    fun gray(dark: Boolean) = if (dark) Color(0xFF7C8296) else Gray
}

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = when (AppState.settings.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}
