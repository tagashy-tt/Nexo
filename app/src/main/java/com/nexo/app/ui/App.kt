package com.nexo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexo.app.R
import com.nexo.app.core.Capitulo
import com.nexo.app.core.Item

enum class Pestana(val label: Int, val icono: ImageVector) {
    Home(R.string.tab_home, Icons.Filled.Home),
    Explore(R.string.tab_browse, Icons.Filled.Search),
    Downloads(R.string.tab_downloads, Icons.Filled.Download),
    Library(R.string.tab_library, Icons.Filled.CollectionsBookmark),
}

enum class Seccion { Ajustes, Actualizaciones, Historial, Categorias, Estadisticas, Datos }

sealed interface Pantalla {
    data class Detalle(val fuenteId: String, val item: Item) : Pantalla
    data class Leer(val fuenteId: String, val item: Item, val cap: Capitulo) : Pantalla
    data class Sec(val s: Seccion) : Pantalla
}

@Composable
fun NexoApp() {
    var tab by remember { mutableStateOf(Pestana.Home) }
    var consulta by remember { mutableStateOf("") }
    val pila = remember { mutableStateListOf<Pantalla>() }
    val volver = { pila.removeAt(pila.lastIndex); Unit }
    BackHandler(pila.isNotEmpty()) { volver() }
    val abrir: (Pantalla) -> Unit = { pila.add(it) }
    val capa = rememberGraphicsLayer()
    val top = pila.lastOrNull()
    val barPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp
    val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 70.dp

    CompositionLocalProvider(LocalCapa provides capa, LocalBarPad provides barPad, LocalTopPad provides topPad) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().grabar(capa)) {
                Fondo()
                when (top) {
                    is Pantalla.Detalle -> DetalleScreen(top, abrir)
                    is Pantalla.Leer -> VisorScreen(top)
                    is Pantalla.Sec -> SeccionScreen(top.s, abrir)
                    null -> when (tab) {
                        Pestana.Home -> HomeScreen(abrir) { consulta = it; tab = Pestana.Explore }
                        Pestana.Explore -> ExplorarScreen(abrir, consulta)
                        Pestana.Downloads -> DescargasScreen()
                        Pestana.Library -> BibliotecaScreen(abrir)
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
                is Pantalla.Detalle -> BotonAtras(volver, Modifier.align(Alignment.TopStart))
                is Pantalla.Leer -> {}
            }
        }
    }
}
