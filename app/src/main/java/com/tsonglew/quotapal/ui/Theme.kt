package com.tsonglew.quotapal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import android.app.Activity

val QuotaBlue = Color(0xFF3989EF)
val QuotaAmber = Color(0xFFD38A2F)

@Composable
fun QuotaTheme(mode: String = "system", content: @Composable () -> Unit) {
    val dark = mode == "dark" || mode == "system" && isSystemInDarkTheme()
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFF79B4FF), background = Color(0xFF101113), surface = Color(0xFF1A1C20),
        surfaceVariant = Color(0xFF24272C), onBackground = Color(0xFFF3F4F6), onSurface = Color(0xFFF3F4F6),
        onSurfaceVariant = Color(0xFFADB2BC), outlineVariant = Color(0xFF30343A),
    ) else lightColorScheme(
        primary = QuotaBlue, background = Color(0xFFF5F6F8), surface = Color.White,
        surfaceVariant = Color(0xFFEBEDF1), onBackground = Color(0xFF17191D), onSurface = Color(0xFF17191D),
        onSurfaceVariant = Color(0xFF6B727E), outlineVariant = Color(0xFFE7E9ED),
    )
    MaterialTheme(colorScheme = colors, content = content)
}
