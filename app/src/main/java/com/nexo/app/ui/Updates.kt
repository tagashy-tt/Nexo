package com.nexo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexo.app.R
import com.nexo.app.core.*
import kotlinx.coroutines.launch

/** Pestaña "Actualizaciones": capítulos y episodios nuevos de los títulos que sigues. */
@Composable
fun UpdatesScreen(abrir: (Pantalla) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ocupado by remember { mutableStateOf<String?>(null) }
    var tipo by remember { mutableStateOf(TipoTab.Home) }
    val d = Store.datos
    val descargados = remember { Descargas.todas(ctx).map { it.second } }
    val lista = d.actualizaciones
        .filter { tipo.cumple(Store.fuente(it.fuenteId)?.tipo) }
        .filter { x -> !Ajustes.soloDescargados || descargados.any { it.startsWith(x.item.titulo) } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = LocalTopPad.current, bottom = LocalBarPad.current),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { TituloGrande(stringResource(R.string.tab_updates)) }
                IconButton(enabled = ocupado == null, onClick = {
                    scope.launch { runCatching { Actualizaciones.refrescar { ocupado = it } }; ocupado = null }
                }) { Icon(Icons.Filled.Refresh, stringResource(R.string.check_updates)) }
            }
        }
        item { FiltroTipos(tipo, R.string.all) { tipo = it } }
        ocupado?.let { item { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp)) } }
        if (d.biblioteca.isEmpty()) item { Text(stringResource(R.string.follow_hint), Modifier.padding(vertical = 16.dp)) }
        else if (lista.isEmpty()) item { Text(stringResource(R.string.no_updates), Modifier.padding(vertical = 16.dp)) }
        itemsIndexed(lista) { i, x ->
            Box(if (i < 6) Modifier.entrada(i) else Modifier) {
                FilaItem(x.item, "${x.cap.nombre} · ${fechaRelativa(x.fecha)}") { abrir(Pantalla.Leer(x.fuenteId, x.item, x.cap)) }
            }
        }
    }
}
