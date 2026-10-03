package com.nexo.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nexo.app.core.Ajustes
import kotlinx.coroutines.delay

/** Entrada con rebote: aparece subiendo y escalando con un resorte (efecto "bouncy"), escalonada por índice. */
fun Modifier.entrada(indice: Int = 0): Modifier = composed {
    if (!Ajustes.animaciones) return@composed Modifier
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(indice * 55L)
        a.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    Modifier.graphicsLayer {
        val v = a.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 70.dp.toPx()
        val s = .92f + .08f * v
        scaleX = s; scaleY = s
    }
}

/** Pulsación con rebote: se encoge al tocar y vuelve con resorte. */
fun Modifier.rebote(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val fuente = remember { MutableInteractionSource() }
    val presionado by fuente.collectIsPressedAsState()
    val escala by animateFloatAsState(
        if (presionado && Ajustes.animaciones) .93f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rebote",
    )
    Modifier.graphicsLayer { scaleX = escala; scaleY = escala }
        .clickable(interactionSource = fuente, indication = null, enabled = enabled, onClick = onClick)
}
