package com.nexo.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nexo.app.MainActivity
import com.nexo.app.R
import com.nexo.app.core.*
import kotlinx.coroutines.launch
import java.io.File

fun tituloSeccion(s: Seccion) = when (s) {
    Seccion.Ajustes -> R.string.settings
    Seccion.AjApariencia -> R.string.appearance
    Seccion.AjLector -> R.string.s_reader
    Seccion.AjReproductor -> R.string.s_player
    Seccion.AjDescargas -> R.string.tab_downloads
    Seccion.AjActualizaciones -> R.string.tab_updates
    Seccion.AjSeguridad -> R.string.s_security
    Seccion.AjAvanzado -> R.string.s_advanced
    Seccion.Acerca -> R.string.about
    Seccion.Historial -> R.string.history
    Seccion.Categorias -> R.string.categories
    Seccion.Estadisticas -> R.string.stats
    Seccion.Datos -> R.string.data_storage
}

private fun Context.activity(): Activity? {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

@Composable
fun SeccionScreen(s: Seccion, abrir: (Pantalla) -> Unit) {
    Pagina {
        when (s) {
            Seccion.Ajustes -> AjustesHub(abrir)
            Seccion.AjApariencia -> Desplazable { AparienciaUi() }
            Seccion.AjLector -> Desplazable { LectorUi() }
            Seccion.AjReproductor -> Desplazable { ReproductorUi() }
            Seccion.AjDescargas -> Desplazable { DescargasAjUi() }
            Seccion.AjActualizaciones -> Desplazable { ActualizacionesAjUi() }
            Seccion.AjSeguridad -> Desplazable { SeguridadUi() }
            Seccion.AjAvanzado -> Desplazable { AvanzadoUi() }
            Seccion.Acerca -> Desplazable { AcercaUi() }
            Seccion.Historial -> HistorialUi(abrir)
            Seccion.Categorias -> CategoriasUi()
            Seccion.Estadisticas -> EstadisticasUi()
            Seccion.Datos -> Desplazable { DatosUi() }
        }
    }
}

@Composable
private fun Desplazable(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        content(); Spacer(Modifier.height(24.dp))
    }

@Composable
private fun Titulo(r: Int) = Text(stringResource(r), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

@Composable
private fun Interruptor(r: Int, valor: Boolean, cambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(stringResource(r), Modifier.weight(1f)); Switch(valor, cambio)
    }
}

@Composable
private fun Radio(texto: String, sel: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(sel, null); Spacer(Modifier.width(12.dp)); Text(texto, Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun FilaMenu(icono: ImageVector, texto: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(20.dp))
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
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(valor, cambio)
    }
}

// ---------------- Ajustes ----------------
@Composable
private fun AjustesHub(abrir: (Pantalla) -> Unit) {
    fun ir(s: Seccion) = abrir(Pantalla.Sec(s))
    Desplazable {
        Tarjeta {
            ToggleRow(Icons.Filled.CloudOff, R.string.downloaded_only, R.string.downloaded_only_desc, Ajustes.soloDescargados) {
                Ajustes.soloDescargados = it; Ajustes.guardar()
            }
            ToggleRow(Icons.Filled.VisibilityOff, R.string.incognito, R.string.incognito_desc, Ajustes.incognito) {
                Ajustes.incognito = it; Ajustes.guardar()
            }
        }
        Tarjeta {
            FilaMenu(Icons.Filled.Palette, R.string.appearance) { ir(Seccion.AjApariencia) }
            FilaMenu(Icons.AutoMirrored.Filled.MenuBook, R.string.s_reader) { ir(Seccion.AjLector) }
            FilaMenu(Icons.Filled.PlayCircle, R.string.s_player) { ir(Seccion.AjReproductor) }
            FilaMenu(Icons.Filled.Download, R.string.tab_downloads) { ir(Seccion.AjDescargas) }
            FilaMenu(Icons.Filled.NewReleases, R.string.tab_updates) { ir(Seccion.AjActualizaciones) }
            FilaMenu(Icons.Filled.Lock, R.string.s_security) { ir(Seccion.AjSeguridad) }
        }
        Tarjeta {
            FilaMenu(Icons.Filled.History, R.string.history) { ir(Seccion.Historial) }
            FilaMenu(Icons.AutoMirrored.Filled.Label, R.string.categories) { ir(Seccion.Categorias) }
            FilaMenu(Icons.Filled.QueryStats, R.string.stats) { ir(Seccion.Estadisticas) }
            FilaMenu(Icons.Filled.Storage, R.string.data_storage) { ir(Seccion.Datos) }
        }
        Tarjeta {
            FilaMenu(Icons.Filled.Build, R.string.s_advanced) { ir(Seccion.AjAvanzado) }
            FilaMenu(Icons.Filled.Info, R.string.about) { ir(Seccion.Acerca) }
        }
    }
}

private val paleta = listOf(0xFF66FF4D, 0xFF4D9DFF, 0xFFFF5252, 0xFFB66DFF, 0xFFFFA040, 0xFFFF6EC7, 0xFF2EE6D6, 0xFFFFE14D)
private val idiomas = listOf(
    "" to null, "en" to "English", "es" to "Español", "pt" to "Português", "fr" to "Français",
    "de" to "Deutsch", "it" to "Italiano", "ru" to "Русский", "ja" to "日本語", "zh" to "中文", "ar" to "العربية",
    "ko" to "한국어", "id" to "Bahasa Indonesia", "vi" to "Tiếng Việt", "tr" to "Türkçe", "hi" to "हिन्दी", "pl" to "Polski",
)

@Composable
private fun ColumnScope.AparienciaUi() {
    var hue by remember { mutableFloatStateOf(120f) }
    val actual = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
    Titulo(R.string.theme)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0 to R.string.mode_system, 1 to R.string.mode_light, 2 to R.string.mode_dark).forEach { (m, r) ->
            FilterChip(Ajustes.modo == m, { Ajustes.modo = m; Ajustes.guardar() }, { Text(stringResource(r)) })
        }
    }
    Interruptor(R.string.amoled, Ajustes.amoled) { Ajustes.amoled = it; Ajustes.guardar() }
    Titulo(R.string.accent)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(paleta) { c ->
            val col = c.toInt()
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color(col))
                    .border(if (Ajustes.acento == col) 3.dp else 0.dp, Color.White, CircleShape)
                    .rebote { Ajustes.acento = col; Ajustes.guardar() }
            )
        }
    }
    Text(stringResource(R.string.custom_color))
    Slider(hue, {
        hue = it
        Ajustes.acento = android.graphics.Color.HSVToColor(floatArrayOf(it, 0.7f, 1f)); Ajustes.guardar()
    }, valueRange = 0f..360f)
    HorizontalDivider()
    Interruptor(R.string.animations, Ajustes.animaciones) { Ajustes.animaciones = it; Ajustes.guardar() }
    Interruptor(R.string.animated_bg, Ajustes.fondoAnimado) { Ajustes.fondoAnimado = it; Ajustes.guardar() }
    Interruptor(R.string.effects_glass, Ajustes.efectosCristal) { Ajustes.efectosCristal = it; Ajustes.guardar() }
    Text(stringResource(R.string.performance_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    HorizontalDivider()
    Titulo(R.string.language)
    idiomas.forEach { (code, nombre) ->
        Radio(nombre ?: stringResource(R.string.language_system), actual == code) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
        }
    }
}

@Composable
private fun ColumnScope.LectorUi() {
    Titulo(R.string.reader_mode)
    Radio(stringResource(R.string.reader_vertical), Ajustes.lectorModo == 0) { Ajustes.lectorModo = 0; Ajustes.guardar() }
    Radio(stringResource(R.string.reader_paged), Ajustes.lectorModo == 1) { Ajustes.lectorModo = 1; Ajustes.guardar() }
    Interruptor(R.string.keep_screen_on, Ajustes.pantallaEncendida) { Ajustes.pantallaEncendida = it; Ajustes.guardar() }
}

@Composable
private fun ColumnScope.ReproductorUi() {
    Titulo(R.string.playback_speed)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { v ->
            FilterChip(Ajustes.velocidad == v, { Ajustes.velocidad = v; Ajustes.guardar() }, { Text("${v}x") })
        }
    }
}

@Composable
private fun ColumnScope.DescargasAjUi() {
    Titulo(R.string.download_destination)
    listOf(0 to R.string.ask_every_time, 1 to R.string.save_in_app, 2 to R.string.save_gallery).forEach { (v, r) ->
        Radio(stringResource(r), Ajustes.destino == v) { Ajustes.destino = v; Ajustes.guardar() }
    }
    Interruptor(R.string.wifi_only, Ajustes.soloWifi) { Ajustes.soloWifi = it; Ajustes.guardar() }
}

@Composable
private fun ColumnScope.ActualizacionesAjUi() {
    val ctx = LocalContext.current
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        Ajustes.notificaciones = ok; Ajustes.guardar()
    }
    fun reprogramar() { if (Ajustes.autoUpdates) Actualizaciones.programar(ctx, true) }
    Interruptor(R.string.auto_updates, Ajustes.autoUpdates) {
        Ajustes.autoUpdates = it; Ajustes.guardar(); Actualizaciones.programar(ctx, it)
    }
    Titulo(R.string.check_interval)
    listOf(6, 12, 24).forEach { h ->
        Radio(stringResource(R.string.hours, h), Ajustes.intervaloH == h) { Ajustes.intervaloH = h; Ajustes.guardar(); reprogramar() }
    }
    Interruptor(R.string.wifi_only, Ajustes.soloWifi) { Ajustes.soloWifi = it; Ajustes.guardar(); reprogramar() }
    Interruptor(R.string.notifications, Ajustes.notificaciones) { on ->
        if (on && Build.VERSION.SDK_INT >= 33) permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        else { Ajustes.notificaciones = on; Ajustes.guardar() }
    }
}

@Composable
private fun ColumnScope.SeguridadUi() {
    val ctx = LocalContext.current
    val disponible = remember { Bloqueo.disponible(ctx) }
    Interruptor(R.string.biometric_lock, Ajustes.bloqueo) { on ->
        if (!on || disponible) { Ajustes.bloqueo = on; Ajustes.guardar() }
    }
    if (!disponible) Text(stringResource(R.string.biometric_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    if (Ajustes.bloqueo) {
        Titulo(R.string.lock_after)
        listOf(0 to R.string.lock_now, 1 to R.string.lock_1, 2 to R.string.lock_5).forEach { (v, r) ->
            Radio(stringResource(r), Ajustes.bloqueoMin == v) { Ajustes.bloqueoMin = v; Ajustes.guardar() }
        }
    }
    HorizontalDivider()
    Interruptor(R.string.secure_screen, Ajustes.pantallaSegura) {
        Ajustes.pantallaSegura = it; Ajustes.guardar(); (ctx.activity() as? MainActivity)?.aplicarSeguro()
    }
    Interruptor(R.string.hide_notif, Ajustes.ocultarNotif) { Ajustes.ocultarNotif = it; Ajustes.guardar() }
    Text(stringResource(R.string.hide_notif_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun ColumnScope.AvanzadoUi() {
    val ctx = LocalContext.current
    val portapapeles = LocalClipboardManager.current
    val archivo = remember { File(ctx.filesDir, "crash.txt") }
    var log by remember { mutableStateOf(if (archivo.exists()) archivo.readText() else "") }
    OutlinedTextField(Ajustes.repoUpdates, { Ajustes.repoUpdates = it; Ajustes.guardar() }, Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(stringResource(R.string.update_repo)) }, placeholder = { Text("usuario/repositorio") })
    Interruptor(R.string.auto_check_app, Ajustes.autoCheckApp) { Ajustes.autoCheckApp = it; Ajustes.guardar() }
    HorizontalDivider()
    Titulo(R.string.crash_log)
    if (log.isEmpty()) Text(stringResource(R.string.no_crash), style = MaterialTheme.typography.bodySmall)
    else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ portapapeles.setText(AnnotatedString(log)) }) { Text(stringResource(R.string.copy)) }
            OutlinedButton({ archivo.delete(); log = "" }) { Text(stringResource(R.string.clear)) }
        }
        Text(log.take(4000), fontSize = 10.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun ColumnScope.AcercaUi() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf<NuevaVersion?>(null) }
    Text(stringResource(R.string.created_by), style = MaterialTheme.typography.titleMedium)
    Text("${stringResource(R.string.version)} ${Actualizador.versionActual(ctx)}")
    Text(stringResource(R.string.about_text))
    Button({
        scope.launch {
            msg = try {
                when (val r = Actualizador.buscar(ctx)) {
                    ResUpd.SinConfig -> ctx.getString(R.string.update_not_configured)
                    ResUpd.AlDia -> ctx.getString(R.string.up_to_date)
                    is ResUpd.Nueva -> { nueva = r.v; "" }
                }
            } catch (e: Exception) { ctx.getString(R.string.error, e.message) }
        }
    }) { Text(stringResource(R.string.check_app_update)) }
    if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.primary)
    nueva?.let { DialogoActualizacion(it) { nueva = null } }
}

// ---------------- Datos, historial, categorías, estadísticas, descargas ----------------
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
private fun ColumnScope.DatosUi() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf("") }
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) scope.launch {
            msg = try { Backup.exportar(ctx, u); ctx.getString(R.string.backup_saved) }
            catch (e: Exception) { ctx.getString(R.string.error, e.message) }
        }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) scope.launch {
            msg = try {
                val r = Backup.importar(ctx, u) { msg = it }
                ctx.getString(R.string.done) + ": " + r
            } catch (e: Exception) { ctx.getString(R.string.error, e.message) }
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
fun DescargasScreen() {
    Column(Modifier.fillMaxSize().padding(top = LocalTopPad.current).padding(horizontal = 14.dp)) {
        TituloGrande(stringResource(R.string.tab_downloads))
        ColaUi()
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
