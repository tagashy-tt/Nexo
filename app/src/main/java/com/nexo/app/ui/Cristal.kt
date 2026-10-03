package com.nexo.app.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.res.painterResource
import com.nexo.app.R
import com.nexo.app.core.Ajustes

/** Capa con todo el contenido de la app: las barras de cristal la dibujan desenfocada. */
val LocalCapa = staticCompositionLocalOf<GraphicsLayer?> { null }
/** Espacio inferior que ocupa la barra flotante, para que las listas pasen por debajo. */
val LocalBarPad = compositionLocalOf { 0.dp }
/** Espacio superior que ocupa el encabezado de cristal. */
val LocalTopPad = compositionLocalOf { 0.dp }

fun Modifier.grabar(capa: GraphicsLayer): Modifier = drawWithContent {
    capa.record { this@drawWithContent.drawContent() }
    drawLayer(capa)
}

/** Cristal esmerilado: desenfoque real del fondo (Android 12+) + tinte; en versiones menores solo tinte. */
@Composable
fun Modifier.vidrio(
    forma: Shape,
    blur: Dp = 26.dp,
    tinte: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .55f),
): Modifier {
    val fondo = LocalCapa.current
    val propia = rememberGraphicsLayer()
    var origen by remember { mutableStateOf(Offset.Zero) }
    val px = with(LocalDensity.current) { blur.toPx() }
    val soporta = Build.VERSION.SDK_INT >= 31 && Ajustes.efectosCristal
    SideEffect { if (soporta) propia.renderEffect = BlurEffect(px, px, TileMode.Clamp) }
    return this
        .onGloballyPositioned { origen = it.positionInRoot() }
        .clip(forma)
        .drawBehind {
            if (soporta && fondo != null) {
                propia.record { translate(-origen.x, -origen.y) { drawLayer(fondo) } }
                drawLayer(propia)
            }
            drawRect(if (soporta) tinte else tinte.copy(alpha = .92f))
        }
        .border(1.dp, Color.White.copy(alpha = .14f), forma)
}

/** Fondo ambiental: dos resplandores suaves con el color de acento. */
@Composable
fun Fondo() {
    val a = Color(Ajustes.acento)
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(Ajustes.acento, it) }
    val b = Color(android.graphics.Color.HSVToColor(floatArrayOf((hsv[0] + 70f) % 360f, .6f, 1f)))
    val claro = MaterialTheme.colorScheme.background.luminance() > .5f
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).drawBehind {
            drawRect(Brush.radialGradient(
                listOf(a.copy(alpha = if (claro) .22f else .30f), Color.Transparent),
                center = Offset(size.width * .12f, size.height * .10f), radius = size.minDimension * 1.15f))
            drawRect(Brush.radialGradient(
                listOf(b.copy(alpha = if (claro) .16f else .20f), Color.Transparent),
                center = Offset(size.width * .95f, size.height * .85f), radius = size.minDimension * 1.25f))
        }
    ) {
        if (Ajustes.fondoAnimado && !claro) Estrellas(a)
    }
}

@Composable
fun Tarjeta(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = MaterialTheme.colorScheme.onSurface
    val forma = RoundedCornerShape(28.dp)
    val base = if (onClick != null) modifier.rebote(onClick = onClick) else modifier
    Column(
        base.fillMaxWidth().clip(forma).background(c.copy(alpha = .07f)).border(1.dp, c.copy(alpha = .10f), forma),
        content = content,
    )
}

@Composable
fun TituloGrande(texto: String) =
    Text(texto, Modifier.padding(start = 4.dp, top = 8.dp, bottom = 12.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold)

/** Encabezado de cristal: destello + logo Nexo a la izquierda, ajustes en el círculo de la derecha. */
@Composable
fun EncabezadoNexo(onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().vidrio(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.logo_nexo), "Nexo", Modifier.height(30.dp))
        Spacer(Modifier.weight(1f))
        IconButton(onSettings, Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = .10f))) {
            Icon(Icons.Filled.Settings, null)
        }
    }
}

@Composable
fun BarraNav(tab: Pestana, onTab: (Pestana) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.padding(horizontal = 14.dp).navigationBarsPadding().padding(bottom = 10.dp)
            .fillMaxWidth().height(70.dp).vidrio(RoundedCornerShape(35.dp)).padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        Pestana.entries.forEach { t ->
            val sel = t == tab
            val col = if (sel) cs.onPrimary else cs.onSurface.copy(alpha = .8f)
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(30.dp))
                    .background(animateColorAsState(if (sel) cs.primary else Color.Transparent, spring(), label = "nav").value).rebote { onTab(t) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                val esc by animateFloatAsState(if (sel) 1.18f else 1f, spring(dampingRatio = Spring.DampingRatioHighBouncy), label = "icono")
                Icon(t.icono, null, Modifier.graphicsLayer { scaleX = esc; scaleY = esc }, tint = col)
                Text(stringResource(t.label), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = col)
            }
        }
    }
}

@Composable
fun Cabecera(titulo: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)
            .fillMaxWidth().height(56.dp).vidrio(RoundedCornerShape(28.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BotonAtras(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.statusBarsPadding().padding(14.dp).size(48.dp).vidrio(CircleShape), Alignment.Center) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
    }
}

/** Contenedor de sub-pantallas: deja libre la cabecera de cristal. */
@Composable
fun Pagina(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(top = 76.dp).navigationBarsPadding().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content,
    )
}
