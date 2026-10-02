package com.nexo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nexo.app.R
import com.nexo.app.core.*
import kotlinx.coroutines.launch

@Composable
fun ExplorarScreen(abrir: (Pantalla) -> Unit, consulta: String = "") {
    var sub by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(top = LocalTopPad.current)) {
        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(sub == 0, { sub = 0 }, { Text(stringResource(R.string.sources)) })
            FilterChip(sub == 1, { sub = 1 }, { Text(stringResource(R.string.extensions)) })
        }
        if (sub == 0) BuscarFuentes(abrir, consulta) else ExtensionesScreen()
    }
}

@Composable
private fun BuscarFuentes(abrir: (Pantalla) -> Unit, inicial: String) {
    val ctx = LocalContext.current
    val fuentes = Store.fuentes
    var sel by remember { mutableStateOf(fuentes.firstOrNull()?.id) }
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<Item>>(emptyList()) }
    var estado by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun buscar() {
        val f = Store.fuente(sel ?: return) ?: return
        estado = ctx.getString(R.string.searching)
        scope.launch {
            try { res = f.buscar(q); estado = if (res.isEmpty()) ctx.getString(R.string.no_results) else "" }
            catch (e: Exception) { estado = ctx.getString(R.string.error, e.message) }
        }
    }
    LaunchedEffect(inicial) { if (inicial.isNotBlank()) { q = inicial; buscar() } }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        if (fuentes.isEmpty()) {
            Text(stringResource(R.string.no_sources))
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fuentes) { f -> FilterChip(f.id == sel, { sel = f.id }, { Text(f.nombre) }) }
            }
            OutlinedTextField(
                q, { q = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true, label = { Text(stringResource(R.string.search)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { buscar() }),
            )
            if (estado.isNotEmpty()) Text(estado)
            LazyColumn(contentPadding = PaddingValues(bottom = LocalBarPad.current)) { items(res) { it -> FilaItem(it) { abrir(Pantalla.Detalle(sel!!, it)) } } }
        }
    }
}

@Composable
private fun ExtensionesScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val ext = remember { runCatching { Extensiones.detectar(ctx) }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentPadding = PaddingValues(bottom = LocalBarPad.current), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.manifest_url)) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({
                    scope.launch {
                        msg = try { val m = Engine.descargar(url.trim()); Store.agregar(m); m.nombre }
                        catch (e: Exception) { ctx.getString(R.string.error, e.message) }
                    }
                }) { Text(stringResource(R.string.add)) }
                OutlinedButton({
                    msg = try {
                        ctx.assets.list("ejemplos")!!.forEach {
                            Store.agregar(Engine.json.decodeFromString(ctx.assets.open("ejemplos/$it").bufferedReader().readText()))
                        }
                        ctx.getString(R.string.done)
                    } catch (e: Exception) { ctx.getString(R.string.error, e.message) }
                }) { Text(stringResource(R.string.load_examples)) }
            }
            if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.installed_sources), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
        }
        items(Store.datos.fuentes) { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${m.nombre} · ${m.tipo}", Modifier.weight(1f))
                IconButton({ Store.guardar(Store.datos.copy(fuentes = Store.datos.fuentes - m)) }) {
                    Icon(Icons.Filled.Delete, null)
                }
            }
        }
        item {
            Text(stringResource(R.string.detected_ext, ext.size), Modifier.padding(top = 8.dp))
            ext.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            if (ext.isNotEmpty()) Text(stringResource(R.string.detected_ext_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}
