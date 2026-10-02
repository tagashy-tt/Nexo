package com.nexo.app.ui

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nexo.app.R
import com.nexo.app.core.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
private fun FilaMenu(icono: ImageVector, texto: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(24.dp))
        Text(stringResource(texto), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ToggleRow(icono: ImageVector, titulo: Int, desc: Int, valor: Boolean, cambio: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { cambio(!valor) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(valor, cambio)
    }
}

fun tituloSeccion(s: Seccion) = when (s) {
    Seccion.Ajustes -> R.string.settings
    Seccion.Actualizaciones -> R.string.tab_updates
    Seccion.Historial -> R.string.history
    Seccion.Categorias -> R.string.categories
    Seccion.Estadisticas -> R.string.stats
    Seccion.Datos -> R.string.data_storage
}

@Composable
fun SeccionScreen(s: Seccion, abrir: (Pantalla) -> Unit) {
    Pagina {
        when (s) {
            Seccion.Ajustes -> AjustesUi(abrir)
            Seccion.Actualizaciones -> ActualizacionesScreen(abrir)
            Seccion.Historial -> HistorialUi(abrir)
            Seccion.Categorias -> CategoriasUi()
            Seccion.Estadisticas -> EstadisticasUi()
            Seccion.Datos -> DatosUi()
        }
    }
}

@Composable
fun DescargasScreen() {
    Column(Modifier.fillMaxSize().padding(top = LocalTopPad.current).padding(horizontal = 14.dp)) {
        TituloGrande(stringResource(R.string.tab_downloads))
        ColaUi()
    }
}

@Composable
private fun HistorialUi(abrir: (Pantalla) -> Unit) {
    val h = Store.datos.historial
    if (h.isEmpty()) { Text(stringResource(R.string.history_empty), Modifier.padding(16.dp)); return }
    TextButton({ Store.guardar(Store.datos.copy(historial = emptyList())) }) { Text(stringResource(R.string.clear)) }
    LazyColumn {
        items(h) { x -> FilaItem(x.item, x.cap.nombre) { abrir(Pantalla.Leer(x.fuenteId, x.item, x.cap)) } }
    }
}

@Composable
fun ColaUi() {
    val ctx = LocalContext.current
    val infos by WorkManager.getInstance(ctx).getWorkInfosByTagFlow("descarga").collectAsState(emptyList())
    var n by remember { mutableIntStateOf(0) }
    val hechas = remember(n, infos) { Descargas.todas(ctx) }
    val activas = infos.filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
    val galeria = infos.filter { it.state == WorkInfo.State.SUCCEEDED && "d:galeria" in it.tags }
    fun nombre(w: WorkInfo) = w.tags.firstOrNull { it.startsWith("t:") }?.drop(2).orEmpty()
    LazyColumn(contentPadding = PaddingValues(bottom = LocalBarPad.current), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text(stringResource(R.string.in_queue), style = MaterialTheme.typography.titleMedium) }
        items(activas) { w -> Text("${nombre(w)} · ${w.state}", style = MaterialTheme.typography.bodySmall) }
        item { Text(stringResource(R.string.downloaded), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        items(hechas) { (dir, titulo) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, Modifier.weight(1f))
                IconButton({ dir.deleteRecursively(); n++ }) { Icon(Icons.Filled.Delete, null) }
            }
        }
        if (galeria.isNotEmpty()) {
            item { Text(stringResource(R.string.saved_gallery), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items(galeria) { w -> Text(nombre(w), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun CategoriasUi() {
    var nueva by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(nueva, { nueva = it }, Modifier.weight(1f), singleLine = true,
            label = { Text(stringResource(R.string.new_category)) })
        IconButton({
            val c = nueva.trim()
            if (c.isNotEmpty() && c !in Store.datos.categorias) Store.guardar(Store.datos.copy(categorias = Store.datos.categorias + c))
            nueva = ""
        }) { Icon(Icons.Filled.Add, null) }
    }
    Store.datos.categorias.forEach { c ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c, Modifier.weight(1f))
            IconButton({
                val d = Store.datos
                Store.guardar(d.copy(
                    categorias = d.categorias - c,
                    biblioteca = d.biblioteca.map { if (it.categoria == c) it.copy(categoria = null) else it },
                ))
            }) { Icon(Icons.Filled.Delete, null) }
        }
    }
}

@Composable
private fun EstadisticasUi() {
    val ctx = LocalContext.current
    val d = Store.datos
    val anime = d.biblioteca.count { esVideo(Store.fuente(it.fuenteId)?.tipo) }
    val bytes = File(ctx.filesDir, "descargas").walkTopDown().sumOf { if (it.isFile) it.length() else 0L }
    listOf(
        R.string.stat_anime to "$anime",
        R.string.stat_manga to "${d.biblioteca.size - anime}",
        R.string.stat_read to "${d.leidos.size}",
        R.string.stat_size to Formatter.formatFileSize(ctx, bytes),
    ).forEach { (r, v) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), Arrangement.SpaceBetween) {
            Text(stringResource(r)); Text(v, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DatosUi() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf("") }
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) msg = try { Backup.exportar(ctx, u); ctx.getString(R.string.backup_saved) }
        catch (e: Exception) { ctx.getString(R.string.error, e.message) }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) scope.launch {
            msg = try { Backup.importar(ctx, u) { msg = it } } catch (e: Exception) { ctx.getString(R.string.error, e.message) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ exportar.launch("nexo-backup.nexo.json") }) { Text(stringResource(R.string.backup_create)) }
        OutlinedButton({ importar.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.backup_restore)) }
    }
    Text(stringResource(R.string.backup_note), style = MaterialTheme.typography.bodySmall)
    if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.primary)
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    OutlinedButton({ ctx.cacheDir.deleteRecursively(); msg = ctx.getString(R.string.done) }) { Text(stringResource(R.string.clear_cache)) }
    OutlinedButton({ File(ctx.filesDir, "descargas").deleteRecursively(); msg = ctx.getString(R.string.done) }) {
        Text(stringResource(R.string.delete_downloads))
    }
}

private val paleta = listOf(0xFF66FF4D, 0xFF4D9DFF, 0xFFFF5252, 0xFFB66DFF, 0xFFFFA040, 0xFFFF6EC7, 0xFF2EE6D6, 0xFFFFE14D)
private val idiomas = listOf("" to null, "en" to "English", "es" to "Español")

@Composable
private fun AjustesUi(abrir: (Pantalla) -> Unit) {
    val ctx = LocalContext.current
    var hue by remember { mutableFloatStateOf(120f) }
    val actual = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Tarjeta {
            ToggleRow(Icons.Filled.CloudOff, R.string.downloaded_only, R.string.downloaded_only_desc, Ajustes.soloDescargados) {
                Ajustes.soloDescargados = it; Ajustes.guardar()
            }
            ToggleRow(Icons.Filled.VisibilityOff, R.string.incognito, R.string.incognito_desc, Ajustes.incognito) {
                Ajustes.incognito = it; Ajustes.guardar()
            }
        }
        Tarjeta {
            FilaMenu(Icons.Filled.NewReleases, R.string.tab_updates) { abrir(Pantalla.Sec(Seccion.Actualizaciones)) }
            FilaMenu(Icons.Filled.History, R.string.history) { abrir(Pantalla.Sec(Seccion.Historial)) }
            FilaMenu(Icons.AutoMirrored.Filled.Label, R.string.categories) { abrir(Pantalla.Sec(Seccion.Categorias)) }
            FilaMenu(Icons.Filled.QueryStats, R.string.stats) { abrir(Pantalla.Sec(Seccion.Estadisticas)) }
            FilaMenu(Icons.Filled.Storage, R.string.data_storage) { abrir(Pantalla.Sec(Seccion.Datos)) }
        }
        Text(stringResource(R.string.download_destination), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        listOf(0 to R.string.ask_every_time, 1 to R.string.save_in_app, 2 to R.string.save_gallery).forEach { (v, r) ->
            Row(Modifier.fillMaxWidth().clickable { Ajustes.destino = v; Ajustes.guardar() }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(Ajustes.destino == v, null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(r), Modifier.padding(vertical = 8.dp))
            }
        }
        HorizontalDivider()
        Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.theme))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to R.string.mode_system, 1 to R.string.mode_light, 2 to R.string.mode_dark).forEach { (m, r) ->
                FilterChip(Ajustes.modo == m, { Ajustes.modo = m; Ajustes.guardar() }, { Text(stringResource(r)) })
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.amoled), Modifier.weight(1f))
            Switch(Ajustes.amoled, { Ajustes.amoled = it; Ajustes.guardar() })
        }
        Text(stringResource(R.string.accent))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(paleta) { c ->
                val col = c.toInt()
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(Color(col))
                        .border(if (Ajustes.acento == col) 3.dp else 0.dp, Color.White, CircleShape)
                        .clickable { Ajustes.acento = col; Ajustes.guardar() }
                )
            }
        }
        Text(stringResource(R.string.custom_color))
        Slider(hue, {
            hue = it
            Ajustes.acento = android.graphics.Color.HSVToColor(floatArrayOf(it, 0.7f, 1f)); Ajustes.guardar()
        }, valueRange = 0f..360f)

        HorizontalDivider()
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        idiomas.forEach { (code, nombre) ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(actual == code, null)
                Spacer(Modifier.width(12.dp))
                Text(nombre ?: stringResource(R.string.language_system), Modifier.padding(vertical = 8.dp))
            }
        }

        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.auto_updates), Modifier.weight(1f))
            Switch(Ajustes.autoUpdates, {
                Ajustes.autoUpdates = it; Ajustes.guardar(); Actualizaciones.programar(ctx, it)
            })
        }
        HorizontalDivider()
        Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.created_by))
        Text("Nexo v0.3.0")
        Text(stringResource(R.string.about_text))
    }
}
