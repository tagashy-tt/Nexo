package com.nexo.app.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.nexo.app.R
import com.nexo.app.core.*

@Composable
fun DetalleScreen(p: Pantalla.Detalle, abrir: (Pantalla) -> Unit) {
    val ctx = LocalContext.current
    var caps by remember { mutableStateOf<List<Capitulo>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var pendiente by remember { mutableStateOf<Capitulo?>(null) }
    LaunchedEffect(p) {
        try { caps = Store.fuente(p.fuenteId)!!.capitulos(p.item.id) }
        catch (e: Exception) { err = e.message; caps = emptyList() }
    }
    fun lanzar(c: Capitulo, destino: String) {
        Descargas.encolar(ctx, p.fuenteId, c, p.item.titulo, destino)
        Toast.makeText(ctx, ctx.getString(R.string.download_queued), Toast.LENGTH_SHORT).show()
    }
    val d = Store.datos
    val fav = d.biblioteca.firstOrNull { it.fuenteId == p.fuenteId && it.item.id == p.item.id }

    pendiente?.let { c ->
        AlertDialog(
            onDismissRequest = { pendiente = null },
            title = { Text(stringResource(R.string.download_where)) },
            text = { Text(c.nombre) },
            confirmButton = {
                Column {
                    TextButton({ lanzar(c, "app"); pendiente = null }) { Text(stringResource(R.string.save_in_app)) }
                    TextButton({ lanzar(c, "galeria"); pendiente = null }) { Text(stringResource(R.string.save_gallery)) }
                }
            },
            dismissButton = { TextButton({ pendiente = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp, end = 14.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp),
    ) {
        item {
            Row(Modifier.entrada(0), verticalAlignment = Alignment.CenterVertically) {
                Portada(p.item.portada); Spacer(Modifier.width(12.dp))
                Text(p.item.titulo, style = MaterialTheme.typography.titleLarge)
            }
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val c = caps
                    val key = "${p.fuenteId}:${p.item.id}"
                    Store.guardar(
                        if (fav != null) d.copy(biblioteca = d.biblioteca - fav)
                        else d.copy(
                            biblioteca = d.biblioteca + Fav(p.fuenteId, p.item),
                            conocidos = if (c != null) d.conocidos + (key to c.map { it.id }) else d.conocidos,
                        )
                    )
                }) { Text(stringResource(if (fav != null) R.string.unfollow else R.string.follow)) }
                if (fav != null) OutlinedButton({ abrir(Pantalla.Migrar(fav)) }) { Text(stringResource(R.string.migrate)) }
            }
            if (fav != null && d.categorias.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(d.categorias) { c ->
                    FilterChip(fav.categoria == c, {
                        Store.guardar(d.copy(biblioteca = d.biblioteca.map {
                            if (it == fav) it.copy(categoria = if (it.categoria == c) null else c) else it
                        }))
                    }, { Text(c) })
                }
            }
            err?.let { Text(stringResource(R.string.error, it)) }
            if (caps == null) CircularProgressIndicator()
        }
        items(caps.orEmpty()) { c ->
            val leido = "${p.fuenteId}:${c.id}" in d.leidos
            Tarjeta(Modifier.padding(vertical = 4.dp), { abrir(Pantalla.Leer(p.fuenteId, p.item, c)) }) {
                Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.nombre, Modifier.weight(1f).padding(vertical = 12.dp),
                        color = if (leido) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = {
                        when (Ajustes.destino) {
                            1 -> lanzar(c, "app")
                            2 -> lanzar(c, "galeria")
                            else -> pendiente = c
                        }
                    }) { Icon(Icons.Filled.Download, null) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VisorScreen(p: Pantalla.Leer) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val fuente = Store.fuente(p.fuenteId)
    var urls by remember { mutableStateOf<List<String>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        val antes = view.keepScreenOn
        if (Ajustes.pantallaEncendida) view.keepScreenOn = true
        onDispose { view.keepScreenOn = antes }
    }
    LaunchedEffect(p) {
        val locales = Descargas.archivos(ctx, p.cap.id)
        try {
            urls = if (locales.isNotEmpty()) locales.map { it.toURI().toString() }
                   else fuente!!.contenido(p.cap.id)
            if (!Ajustes.incognito) {
                val d = Store.datos
                val h = Hist(p.fuenteId, p.item, p.cap, System.currentTimeMillis())
                Store.guardar(d.copy(
                    leidos = d.leidos + "${p.fuenteId}:${p.cap.id}",
                    historial = (listOf(h) + d.historial.filterNot { it.fuenteId == p.fuenteId && it.cap.id == p.cap.id }).take(200),
                ))
            }
        } catch (e: Exception) { err = e.message; urls = emptyList() }
    }
    val lista = urls
    when {
        lista == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        lista.isEmpty() -> Text(err?.let { stringResource(R.string.error, it) } ?: stringResource(R.string.no_content), Modifier.padding(16.dp))
        esVideo(fuente?.tipo) -> Reproductor(lista.first())
        Ajustes.lectorModo == 1 -> {
            val estado = rememberPagerState { lista.size }
            HorizontalPager(estado, Modifier.fillMaxSize()) { i ->
                AsyncImage(lista[i], null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(lista) { u -> AsyncImage(u, null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
        }
    }
}

@Composable
fun Reproductor(url: String) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(url)); prepare(); setPlaybackSpeed(Ajustes.velocidad); playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; player.release() }
    }
    AndroidView({ PlayerView(it).apply { this.player = player } }, Modifier.fillMaxSize())
}
