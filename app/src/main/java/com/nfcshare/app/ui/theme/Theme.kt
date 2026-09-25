package com.nfcshare.app.ui.theme

import android.os.Build
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nfcshare.app.data.Settings

private val Light = lightColorScheme(primary = Color(0xFFAD4600), onPrimary = Color.White, primaryContainer = Color(0xFFFFE2CC), onPrimaryContainer = Color(0xFF633000), secondary = Color(0xFF5E625E), secondaryContainer = Color(0xFFFFE2CC), onSecondaryContainer = Color(0xFF633000), background = Color(0xFFFAF9F6), surface = Color(0xFFFAF9F6), surfaceContainer = Color(0xFFF0EFEB), surfaceContainerHigh = Color(0xFFEAE9E4), onSurface = Color(0xFF202321), outline = Color(0xFF797B76))
private val Dark = darkColorScheme(primary = Color(0xFFFFAC73), onPrimary = Color(0xFF542400), primaryContainer = Color(0xFF65310E), onPrimaryContainer = Color(0xFFFFDEC7), secondary = Color(0xFFBEC7BD), secondaryContainer = Color(0xFF65310E), onSecondaryContainer = Color(0xFFFFDEC7), background = Color(0xFF111411), surface = Color(0xFF111411), surfaceContainer = Color(0xFF1D211D), surfaceContainerHigh = Color(0xFF272C27), onSurface = Color(0xFFE4E8E0), outline = Color(0xFF8B9488))
@Composable fun NfcShareTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = settings.theme == "Oscuro" || (settings.theme == "Sistema" && isSystemInDarkTheme())
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
        }
    }
    val colors = if (settings.dynamicColor && Build.VERSION.SDK_INT >= 31) { if(dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current) } else if(dark) Dark else Light
    MaterialTheme(colorScheme = colors, typography = Typography(
        headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
    ), content = content)
}
