package com.nexo.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexo.app.R
import com.nexo.app.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Explorar como en Aniyomi: Anime / Manga separados, y dentro Fuentes (búsqueda global), Extensiones y Migrar. */
@Composable
fun ExplorarScreen(abrir: (Pantalla) -> Unit, consulta: String = "") {
    var anime by remember { mutableStateOf(true) }
    var sub by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(top = LocalTopPad.current)) {
        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(anime, { anime = true }, { Text(stringResource(R.string.mode_anime)) })
            FilterChip(!anime, { anime = false }, { Text(stringResource(R.string.mode_manga)) })
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(sub == 0, { sub = 0 }, { Text(stringResource(R.string.sources)) })
            FilterChip(sub == 1, { sub = 1 }, { Text(stringResource(R.string.extensions)) })
            FilterChip(sub == 2, { sub = 2 }, { Text(stringResource(R.string.migrate)) })
        }
        when (sub) {
            0 -> BuscarFuentes(abrir, anime, consulta)
            1 -> ExtensionesTab(anime)
            else -> SiguiendoTab(abrir, anime)
        }
    }
}

@Composable
private fun BuscarFuentes(abrir: (Pantalla) -> Unit, anime: Boolean, inicial: String) {
    val ctx = LocalContext.current
    val fuentes = Store.fuentes.filter { esVideo(it.tipo) == anime }
    var sel by remember(anime) { mutableStateOf<String?>(null) } // null = todas las fuentes
    var q by remember { mutableStateOf("") }
    var global by remember(anime) { mutableStateOf<List<Pair<Fuente, List<Item>>>>(emptyList()) }
    var uno by remember(anime) { mutableStateOf<List<Item>>(emptyList()) }
    var estado by remember(anime) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun buscar() {
        val s = sel
        estado = ctx.getString(R.string.searching)
        scope.launch {
            try {
                if (s == null) {
                    global = coroutineScope {
                        fuentes.map { f ->
                            async { f to runCatching { if (q.isBlank()) f.populares(1) else f.buscar(q) }.getOrDefault(emptyList()) }
                        }.awaitAll()
                    }.filter { it.second.isNotEmpty() }
                    uno = emptyList()
                    estado = if (global.isEmpty()) ctx.getString(R.string.no_results) else ""
                } else {
                    val f = Store.fuente(s) ?: return@launch
                    uno = if (q.isBlank()) f.populares(1) else f.buscar(q)
                    global = emptyList()
                    estado = if (uno.isEmpty()) ctx.getString(R.string.no_results) else ""
                }
            } catch (e: Exception) { estado = ctx.getString(R.string.error, e.message) }
        }
    }
    LaunchedEffect(inicial, anime) { if (inicial.isNotBlank()) { q = inicial; buscar() } }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        if (fuentes.isEmpty()) {
            Text(stringResource(R.string.no_sources), Modifier.padding(top = 12.dp))
        } else {
            LazyRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(sel == null, { sel = null }, { Text(stringResource(R.string.all_sources)) }) }
                items(fuentes) { f -> FilterChip(f.id == sel, { sel = f.id }, { Text(f.nombre) }) }
            }
            OutlinedTextField(
                q, { q = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp), singleLine = true,
                label = { Text(stringResource(if (sel == null) R.string.global_search else R.string.search)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { buscar() }),
            )
            if (estado.isNotEmpty()) Text(estado)
            LazyColumn(contentPadding = PaddingValues(bottom = LocalBarPad.current)) {
                global.forEach { (f, lista) ->
                    item {
                        Text(f.nombre, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(lista) { itm ->
                                Column(Modifier.width(110.dp).rebote { abrir(Pantalla.Detalle(f.id, itm)) }) {
                                    AsyncImage(itm.portada, null,
                                        Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop)
                                    Text(itm.titulo, Modifier.padding(top = 4.dp), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                items(uno) { itm -> FilaItem(itm) { abrir(Pantalla.Detalle(sel!!, itm)) } }
            }
        }
    }
}

@Composable
private fun ExtensionesTab(anime: Boolean) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var disponibles by remember { mutableStateOf<List<ExtDisponible>>(emptyList()) }
    var instaladas by remember { mutableStateOf(runCatching { Repos.detectar(ctx) }.getOrDefault(emptyList())) }
    val d = Store.datos

    LaunchedEffect(d.repos) {
        cargando = true
        disponibles = d.repos.flatMap { r -> runCatching { Repos.indice(r) }.getOrDefault(emptyList()) }
        cargando = false
    }
    fun manejar(r: Importado): String = when (r) {
        is Importado.Fuentes -> { r.lista.forEach { Store.agregar(it) }; ctx.getString(R.string.added) + ": " + r.lista.joinToString { it.nombre } }
        is Importado.IndiceExt -> { Store.guardar(Store.datos.copy(repos = (Store.datos.repos + r.url).distinct())); ctx.getString(R.string.repo_added) }
        is Importado.Apk -> if (Repos.instalar(ctx, r.archivo)) ctx.getString(R.string.done) else ctx.getString(R.string.allow_install)
    }
    val archivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) scope.launch {
            msg = try { manejar(Importador.desdeArchivo(ctx, u)) } catch (e: Exception) { ctx.getString(R.string.error, e.message) }
        }
    }

    val f = filtro.trim()
    val fuentesJson = d.fuentes.filter { esVideo(it.tipo) == anime && (f.isEmpty() || it.nombre.contains(f, true)) }
    val apkInst = instaladas.filter { it.anime == anime && (f.isEmpty() || it.nombre.contains(f, true)) }
    val disp = disponibles.filter { x ->
        x.ext.esAnime == anime && instaladas.none { it.pkg == x.ext.pkg } &&
            (f.isEmpty() || x.ext.titulo.contains(f, true) || x.ext.sources.any { s -> s.name.contains(f, true) })
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 14.dp),
        contentPadding = PaddingValues(bottom = LocalBarPad.current), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(filtro, { filtro = it }, Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = true,
                label = { Text(stringResource(R.string.search_extensions)) })
        }
        item {
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.add_repo_hint)) })
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({
                    scope.launch {
                        msg = try { manejar(Importador.desdeUrl(ctx, url)) } catch (e: Exception) { ctx.getString(R.string.error, e.message) }
                    }
                }) { Text(stringResource(R.string.add)) }
                OutlinedButton({ archivo.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Filled.UploadFile, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.import_file))
                }
                OutlinedButton({
                    msg = try {
                        ctx.assets.list("ejemplos")!!.forEach {
                            Store.agregar(Engine.json.decodeFromString(ctx.assets.open("ejemplos/$it").bufferedReader().readText()))
                        }
                        ctx.getString(R.string.done)
                    } catch (e: Exception) { ctx.getString(R.string.error, e.message) }
                }) { Text(stringResource(R.string.load_examples)) }
            }
            if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.installed), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton({ instaladas = runCatching { Repos.detectar(ctx) }.getOrDefault(emptyList()) }) { Icon(Icons.Filled.Refresh, null) }
            }
        }
        items(fuentesJson) { m ->
            FilaExt("${m.nombre}", m.tipo) {
                IconButton({ Store.guardar(Store.datos.copy(fuentes = Store.datos.fuentes - m)) }) { Icon(Icons.Filled.Delete, null) }
            }
        }
        items(apkInst) { x ->
            FilaExt(x.nombre, "APK · ${x.version}") {
                TextButton({ Repos.desinstalar(ctx, x.pkg) }) { Text(stringResource(R.string.uninstall)) }
            }
        }
        if (apkInst.isNotEmpty()) item { Text(stringResource(R.string.ext_run_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.available), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                if (cargando) { Spacer(Modifier.width(8.dp)); CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) }
            }
        }
        items(disp) { x ->
            FilaExt(x.ext.titulo, "${x.ext.lang} · ${x.ext.version} · ${x.ext.sources.size}") {
                TextButton({
                    scope.launch {
                        msg = try {
                            if (Repos.instalar(ctx, Repos.descargarApk(ctx, x.apkUrl))) ctx.getString(R.string.done) else ctx.getString(R.string.allow_install)
                        } catch (e: Exception) { ctx.getString(R.string.error, e.message) }
                    }
                }) { Text(stringResource(R.string.install)) }
            }
        }
        item { Text(stringResource(R.string.repos), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        items(d.repos) { r ->
            FilaExt(r.substringAfter("//").take(48), "") {
                IconButton({ Store.guardar(Store.datos.copy(repos = Store.datos.repos - r)) }) { Icon(Icons.Filled.Delete, null) }
            }
        }
    }
}

@Composable
private fun FilaExt(titulo: String, sub: String, accion: @Composable () -> Unit) {
    Tarjeta {
        Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            accion()
        }
    }
}

@Composable
private fun SiguiendoTab(abrir: (Pantalla) -> Unit, anime: Boolean) {
    val lista = Store.datos.biblioteca.filter { esVideo(Store.fuente(it.fuenteId)?.tipo) == anime }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentPadding = PaddingValues(top = 6.dp, bottom = LocalBarPad.current)) {
        if (lista.isEmpty()) item { Text(stringResource(R.string.follow_hint), Modifier.padding(vertical = 16.dp)) }
        items(lista) { fav ->
            Tarjeta(Modifier.padding(vertical = 4.dp), { abrir(Pantalla.Detalle(fav.fuenteId, fav.item)) }) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Portada(fav.item.portada); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(fav.item.titulo, maxLines = 2)
                        Text(Store.fuente(fav.fuenteId)?.nombre.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton({ abrir(Pantalla.Migrar(fav)) }) { Icon(Icons.Filled.SwapHoriz, stringResource(R.string.migrate)) }
                    IconButton({ Store.guardar(Store.datos.copy(biblioteca = Store.datos.biblioteca - fav)) }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.unfollow))
                    }
                }
            }
        }
    }
}

@Composable
fun MigrarScreen(p: Pantalla.Migrar, volver: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val origenVideo = esVideo(Store.fuente(p.fav.fuenteId)?.tipo)
    val destinos = Store.fuentes.filter { it.id != p.fav.fuenteId && esVideo(it.tipo) == origenVideo }
    var sel by remember { mutableStateOf(destinos.firstOrNull()?.id) }
    var resultados by remember { mutableStateOf<List<Item>>(emptyList()) }
    var estado by remember { mutableStateOf("") }
    var elegido by remember { mutableStateOf<Item?>(null) }
    var conservar by remember { mutableStateOf(true) }

    LaunchedEffect(sel) {
        val f = sel?.let { Store.fuente(it) } ?: return@LaunchedEffect
        estado = ctx.getString(R.string.searching); resultados = emptyList()
        try {
            resultados = f.buscar(p.fav.item.titulo)
            estado = if (resultados.isEmpty()) ctx.getString(R.string.no_matches) else ""
        } catch (e: Exception) { estado = ctx.getString(R.string.error, e.message) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 72.dp, start = 14.dp, end = 14.dp)) {
        Text("${stringResource(R.string.migrate_to)}: ${p.fav.item.titulo}", style = MaterialTheme.typography.titleMedium, maxLines = 2)
        if (destinos.isEmpty()) Text(stringResource(R.string.no_sources), Modifier.padding(top = 12.dp))
        LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(destinos) { f -> FilterChip(f.id == sel, { sel = f.id }, { Text(f.nombre) }) }
        }
        if (estado.isNotEmpty()) Text(estado)
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(resultados) { itm -> FilaItem(itm) { elegido = itm } }
        }
    }

    val destino = sel?.let { Store.fuente(it) }
    val nuevo = elegido
    if (nuevo != null && destino != null) {
        AlertDialog(
            onDismissRequest = { elegido = null },
            title = { Text(stringResource(R.string.confirm)) },
            text = {
                Column {
                    Text(stringResource(R.string.migrate_confirm, p.fav.item.titulo, destino.nombre))
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(conservar, { conservar = it })
                        Text(stringResource(R.string.migrate_keep_read))
                    }
                }
            },
            confirmButton = {
                TextButton({
                    scope.launch {
                        runCatching { Migracion.migrar(p.fav, destino, nuevo, conservar) }
                            .onSuccess { Toast.makeText(ctx, ctx.getString(R.string.migrated), Toast.LENGTH_SHORT).show(); volver() }
                            .onFailure { Toast.makeText(ctx, ctx.getString(R.string.error, it.message), Toast.LENGTH_LONG).show() }
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton({ elegido = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
