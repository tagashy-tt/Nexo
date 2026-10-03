package com.nexo.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.nexo.app.core.Ajustes

@Composable
fun NexoTheme(content: @Composable () -> Unit) {
    val oscuro = when (Ajustes.modo) { 0 -> isSystemInDarkTheme(); 1 -> false; else -> true }
    val a = Color(Ajustes.acento)
    val esquema = if (oscuro) {
        val bg = if (Ajustes.amoled) Color.Black else Color(0xFF121212)
        darkColorScheme(
            primary = a, onPrimary = Color.Black,
            secondaryContainer = a, onSecondaryContainer = Color.Black,
            primaryContainer = lerp(bg, a, .3f), onPrimaryContainer = Color.White,
            background = bg, surface = bg, onBackground = Color(0xFFEDEDED), onSurface = Color(0xFFEDEDED),
            surfaceVariant = Color(0xFF1E1E1E), surfaceContainer = Color(0xFF0D0D0D), outline = Color(0xFF8A8A8A),
        )
    } else lightColorScheme(
        primary = lerp(a, Color.Black, .35f), onPrimary = Color.White,
        secondaryContainer = lerp(a, Color.White, .5f), onSecondaryContainer = Color.Black,
    )
    val view = LocalView.current
    SideEffect {
        var c = view.context
        while (c is ContextWrapper && c !is Activity) c = c.baseContext
        (c as? Activity)?.window?.let {
            val ctl = WindowCompat.getInsetsController(it, view)
            ctl.isAppearanceLightStatusBars = !oscuro
            ctl.isAppearanceLightNavigationBars = !oscuro
        }
    }
    MaterialTheme(colorScheme = esquema) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
