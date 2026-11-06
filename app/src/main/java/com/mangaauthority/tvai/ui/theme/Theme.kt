package com.mangaauthority.tvai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

// Night slate: deep blue-grey surfaces with a sky-blue primary. TV always uses this.
private val NightSlate = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    onPrimary = Color(0xFF00315B),
    primaryContainer = Color(0xFF1B4A7A),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD9BAF5),
    onTertiary = Color(0xFF3C2455),
    tertiaryContainer = Color(0xFF543B6D),
    onTertiaryContainer = Color(0xFFF1DBFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1419),
    onBackground = Color(0xFFE0E2E8),
    surface = Color(0xFF0F1419),
    onSurface = Color(0xFFE0E2E8),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E),
    surfaceContainerLowest = Color(0xFF0A0F14),
    surfaceContainerLow = Color(0xFF171C22),
    surfaceContainer = Color(0xFF1B2026),
    surfaceContainerHigh = Color(0xFF262A31),
    surfaceContainerHighest = Color(0xFF30353B),
)

/** Bigger type for 10-foot viewing on a TV; phones keep the standard scale. */
private fun typographyFor(isTv: Boolean): Typography {
    val base = Typography()
    if (!isTv) return base
    return base.copy(
        bodyLarge = base.bodyLarge.copy(fontSize = 24.sp, lineHeight = 34.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
        labelLarge = base.labelLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
        labelMedium = base.labelMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TvAiTheme(isTv: Boolean, content: @Composable () -> Unit) {
    val dark = isTv || isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        !isTv && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> NightSlate
        else -> expressiveLightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = typographyFor(isTv),
        content = content,
    )
}
