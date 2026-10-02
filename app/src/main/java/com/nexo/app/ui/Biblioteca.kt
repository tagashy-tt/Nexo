package com.nexo.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.nexo.app.R
import com.nexo.app.core.*
import kotlinx.coroutines.launch

fun esVideo(t: String?) = t == "anime" || t == "pelicula" || t == "serie"

enum class TipoTab(val label: Int, val tipos: Set<String>?) {
    Home(R.string.tab_home, null),
    Anime(R.string.tab_anime, setOf("anime")),
    Movies(R.string.tab_movies, setOf("pelicula")),
    Series(R.string.tab_series, setOf("serie")),
    Manga(R.string.tab_manga, setOf("manga", "libro"));

    fun cumple(t: String?) = tipos == null || (t ?: "manga") in tipos
}

@Composable
fun FiltroTipos(sel: TipoTab, todoLabel: Int, onSel: (TipoTab) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        TipoTab.entries.forEach { t ->
            val activo = t == sel
            Column(Modifier.clickable { onSel(t) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(if (t == TipoTab.Home) todoLabel else t.label),
                    color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                    fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal,
                )
                Box(Modifier.padding(top = 4.dp).height(2.dp).width(if (activo) 28.dp else 0.dp).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
fun Portada(url: String?) =
    AsyncImage(url, null, Modifier.size(56.dp, 80.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)

@Composable
fun FilaItem(item: Item, sub: String? = null, onClick: () -> Unit) {
    Tarjeta(Modifier.padding(vertical = 4.dp), onClick) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Portada(item.portada); Spacer(Modifier.width(12.dp))
            Column {
                Text(item.titulo, maxLines = 2)
                if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun BibliotecaScreen(abrir: (Pantalla) -> Unit) {
    val ctx = LocalContext.current
    val d = Store.datos
    var tipo by remember { mutableStateOf(TipoTab.Home) }
    var cat by remember { mutableStateOf<String?>(null) }
    val descargados = remember { Descargas.todas(ctx).map { it.second } }
    val lista = d.biblioteca
        .filter { tipo.cumple(Store.fuente(it.fuenteId)?.tipo) }
        .filter { cat == null || it.categoria == cat }
        .filter { f -> !Ajustes.soloDescargados || descargados.any { it.startsWith(f.item.titulo) } }
    val completo: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    LazyVerticalGrid(
        GridCells.Adaptive(112.dp), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = LocalTopPad.current, bottom = LocalBarPad.current),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = completo) { TituloGrande(stringResource(R.string.tab_library)) }
        item(span = completo) { FiltroTipos(tipo, R.string.all) { tipo = it } }
        if (d.categorias.isNotEmpty()) item(span = completo) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(cat == null, { cat = null }, { Text(stringResource(R.string.all)) }) }
                items(d.categorias) { c -> FilterChip(cat == c, { cat = c }, { Text(c) }) }
            }
        }
        if (lista.isEmpty()) item(span = completo) { Text(stringResource(R.string.library_empty), Modifier.padding(8.dp)) }
        items(lista) { fav ->
            Box(
                Modifier.aspectRatio(2f / 3f).clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { abrir(Pantalla.Detalle(fav.fuenteId, fav.item)) }
            ) {
                AsyncImage(fav.item.portada, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(84.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .8f)))))
                Text(fav.item.titulo, Modifier.align(Alignment.BottomStart).padding(10.dp), color = Color.White,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
        }
    }
}

/** Contenido de la pantalla "Actualizaciones" (se muestra dentro de Pagina). */
@Composable
fun ActualizacionesScreen(abrir: (Pantalla) -> Unit) {
    val scope = rememberCoroutineScope()
    var ocupado by remember { mutableStateOf<String?>(null) }
    val ups = Store.datos.actualizaciones
    Button({ scope.launch { Actualizaciones.refrescar { ocupado = it }; ocupado = null } }, enabled = ocupado == null) {
        Text(stringResource(R.string.check_updates))
    }
    ocupado?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (ups.isEmpty()) Text(stringResource(R.string.no_updates), Modifier.padding(vertical = 8.dp))
    LazyColumn {
        items(ups) { x -> FilaItem(x.item, x.cap.nombre) { abrir(Pantalla.Leer(x.fuenteId, x.item, x.cap)) } }
    }
}

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Portada(p.item.portada); Spacer(Modifier.width(12.dp))
                Text(p.item.titulo, style = MaterialTheme.typography.titleLarge)
            }
            Button(
                onClick = {
                    val c = caps
                    val key = "${p.fuenteId}:${p.item.id}"
                    Store.guardar(
                        if (fav != null) d.copy(biblioteca = d.biblioteca - fav)
                        else d.copy(
                            biblioteca = d.biblioteca + Fav(p.fuenteId, p.item),
                            conocidos = if (c != null) d.conocidos + (key to c.map { it.id }) else d.conocidos,
                        )
                    )
                },
                modifier = Modifier.padding(vertical = 8.dp),
            ) { Text(stringResource(if (fav != null) R.string.remove_from_library else R.string.add_to_library)) }
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

@Composable
fun VisorScreen(p: Pantalla.Leer) {
    val ctx = LocalContext.current
    val fuente = Store.fuente(p.fuenteId)
    var urls by remember { mutableStateOf<List<String>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
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
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(lista) { u -> AsyncImage(u, null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
        }
    }
}

@Composable
fun Reproductor(url: String) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }
    AndroidView({ PlayerView(it).apply { this.player = player } }, Modifier.fillMaxSize())
}
