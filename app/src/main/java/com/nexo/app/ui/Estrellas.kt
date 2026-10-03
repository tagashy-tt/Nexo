package com.nexo.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private class Est(val x: Float, val y: Float, val r: Float, val periodo: Int, val fase: Float)
private val periodos = intArrayOf(2, 3, 4, 5, 6, 10, 12, 15, 20, 30) // divisores de 60: el bucle no da saltos

/** Estrellas parpadeantes + estrellas fugaces. Un solo Canvas, ~60 círculos: barato incluso en gamas bajas. */
@Composable
fun Estrellas(acento: Color) {
    val estrellas = remember {
        val rnd = java.util.Random(7)
        List(55) { Est(rnd.nextFloat(), rnd.nextFloat(), .5f + rnd.nextFloat() * 1.3f, periodos[rnd.nextInt(periodos.size)], rnd.nextFloat()) }
    }
    val t by rememberInfiniteTransition(label = "estrellas").animateFloat(
        initialValue = 0f, targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(Modifier.fillMaxSize()) {
        for (e in estrellas) {
            val a = .25f + .75f * abs(sin(2.0 * PI * (t / e.periodo + e.fase))).toFloat()
            drawCircle(Color.White.copy(alpha = a * .8f), e.r * density, Offset(e.x * size.width, e.y * size.height))
        }
        fugaz(t, 6, 0f, 0, acento)
        fugaz(t, 10, 3f, 1, Color.White)
        fugaz(t, 15, 8f, 2, acento)
    }
}

private fun DrawScope.fugaz(t: Float, periodo: Int, desfase: Float, k: Int, color: Color) {
    val x = t + desfase
    val u = (x % periodo) / periodo
    if (u > .16f) return
    val q = u / .16f
    val ciclo = (x / periodo).toInt()
    val inicio = Offset(
        size.width * (.35f + .65f * (((ciclo * 37 + k * 11) % 10) / 10f)),
        size.height * (.02f + .30f * (((ciclo * 13 + k * 7) % 10) / 10f)),
    )
    val dir = Offset(-.55f, .38f)
    val largo = size.minDimension * .75f
    val cabeza = Offset(inicio.x + dir.x * largo * q, inicio.y + dir.y * largo * q)
    val cola = Offset(cabeza.x - dir.x * largo * .22f, cabeza.y - dir.y * largo * .22f)
    val alpha = sin(PI * q).toFloat()
    drawLine(
        brush = Brush.linearGradient(listOf(Color.Transparent, color.copy(alpha = alpha)), start = cola, end = cabeza),
        start = cola, end = cabeza, strokeWidth = 2.2f * density, cap = StrokeCap.Round,
    )
    drawCircle(Color.White.copy(alpha = alpha), 2.4f * density, cabeza)
}
