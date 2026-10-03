package com.nexo.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexo.app.R
import com.nexo.app.core.*
import kotlinx.coroutines.launch

enum class Pestana(val label: Int, val icono: ImageVector) {
    Home(R.string.tab_home, Icons.Filled.Home),
    Explore(R.string.tab_browse, Icons.Filled.Search),
    Downloads(R.string.tab_downloads, Icons.Filled.Download),
    Updates(R.string.tab_updates, Icons.Filled.NewReleases),
}

enum class Seccion {
    Ajustes, AjApariencia, AjLector, AjReproductor, AjDescargas, AjActualizaciones, AjSeguridad, AjAvanzado, Acerca,
    Historial, Categorias, Estadisticas, Datos,
}

sealed interface Pantalla {
    data class Detalle(val fuenteId: String, val item: Item) : Pantalla
    data class Leer(val fuenteId: String, val item: Item, val cap: Capitulo) : Pantalla
    data class Sec(val s: Seccion) : Pantalla
    data class Migrar(val fav: Fav) : Pantalla
}

@Composable
fun NexoApp() {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(Pestana.Home) }
    var consulta by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf<NuevaVersion?>(null) }
    val pila = remember { mutableStateListOf<Pantalla>() }
    val volver = { pila.removeAt(pila.lastIndex); Unit }
    BackHandler(pila.isNotEmpty()) { volver() }
    val abrir: (Pantalla) -> Unit = { pila.add(it) }
    val capa = rememberGraphicsLayer()
    val top = pila.lastOrNull()
    val barPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp
    val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 70.dp

    // Aviso de nueva versión (como máximo cada 6 h)
    LaunchedEffect(Unit) {
        val ahora = System.currentTimeMillis()
        if (Ajustes.autoCheckApp && ahora - Ajustes.ultimaComprobacion > 6 * 3600_000L) {
            Ajustes.ultimaComprobacion = ahora; Ajustes.guardar()
            val r = runCatching { Actualizador.buscar(ctx) }.getOrNull()
            if (r is ResUpd.Nueva && r.v.version != Ajustes.versionOmitida) nueva = r.v
        }
    }
    nueva?.let { DialogoActualizacion(it) { nueva = null } }

    CompositionLocalProvider(LocalCapa provides capa, LocalBarPad provides barPad, LocalTopPad provides topPad) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().grabar(capa)) {
                Fondo()
                AnimatedContent(
                    targetState = top ?: tab,
                    transitionSpec = {
                        if (Ajustes.animaciones)
                            (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                scaleIn(spring(dampingRatio = .6f, stiffness = Spring.StiffnessLow), initialScale = .94f)) togetherWith
                                fadeOut(tween(120))
                        else EnterTransition.None togetherWith ExitTransition.None
                    },
                    label = "pantallas",
                ) { destino ->
                    when (destino) {
                        is Pantalla.Detalle -> DetalleScreen(destino, abrir)
                        is Pantalla.Leer -> VisorScreen(destino)
                        is Pantalla.Sec -> SeccionScreen(destino.s, abrir)
                        is Pantalla.Migrar -> MigrarScreen(destino, volver)
                        Pestana.Home -> HomeScreen(abrir, { consulta = it; tab = Pestana.Explore }, { tab = Pestana.Updates })
                        Pestana.Explore -> ExplorarScreen(abrir, consulta)
                        Pestana.Downloads -> DescargasScreen()
                        Pestana.Updates -> UpdatesScreen(abrir)
                        else -> {}
                    }
                }
            }
            // Barras de cristal: fuera de la capa grabada, dibujan el contenido desenfocado.
            when (top) {
                null -> {
                    EncabezadoNexo({ abrir(Pantalla.Sec(Seccion.Ajustes)) }, Modifier.align(Alignment.TopCenter))
                    BarraNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
                }
                is Pantalla.Sec -> Cabecera(stringResource(tituloSeccion(top.s)), volver, Modifier.align(Alignment.TopCenter))
                is Pantalla.Detalle, is Pantalla.Migrar -> BotonAtras(volver, Modifier.align(Alignment.TopStart))
                is Pantalla.Leer -> {}
            }
        }
    }
}

@Composable
fun DialogoActualizacion(n: NuevaVersion, cerrar: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ocupado by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!ocupado) cerrar() },
        title = { Text(stringResource(R.string.update_available, n.version)) },
        text = { Text(if (ocupado) stringResource(R.string.downloading) else n.notas) },
        confirmButton = {
            TextButton(enabled = !ocupado, onClick = {
                scope.launch {
                    ocupado = true
                    val r = runCatching { Actualizador.descargarEInstalar(ctx, n) }
                    ocupado = false
                    r.onFailure { Toast.makeText(ctx, ctx.getString(R.string.error, it.message), Toast.LENGTH_LONG).show() }
                    r.onSuccess { ok ->
                        if (ok) cerrar() else Toast.makeText(ctx, ctx.getString(R.string.allow_install), Toast.LENGTH_LONG).show()
                    }
                }
            }) { Text(stringResource(R.string.update_now)) }
        },
        dismissButton = {
            Row {
                TextButton(enabled = !ocupado, onClick = { Ajustes.versionOmitida = n.version; Ajustes.guardar(); cerrar() }) {
                    Text(stringResource(R.string.skip_version))
                }
                TextButton(enabled = !ocupado, onClick = cerrar) { Text(stringResource(R.string.later)) }
            }
        },
    )
}

@Composable
fun PantallaBloqueo(pedir: () -> Unit) {
    LaunchedEffect(Unit) { pedir() }
    Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.logo_nexo), "Nexo", Modifier.width(220.dp))
            Spacer(Modifier.height(32.dp))
            Button(pedir) {
                Icon(Icons.Filled.Fingerprint, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.unlock))
            }
        }
    }
}
