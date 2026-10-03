package com.nexo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexo.app.R
import com.nexo.app.core.*

private class HeroDatos(val item: Item, val sub: String, val onClick: () -> Unit)

@Composable
fun HomeScreen(abrir: (Pantalla) -> Unit, buscar: (String) -> Unit, irUpdates: () -> Unit) {
    var tipo by remember { mutableStateOf(TipoTab.Home) }
    var q by remember { mutableStateOf("") }
    val d = Store.datos
    var tendencias by remember { mutableStateOf<List<Pair<String, Item>>>(emptyList()) }
    LaunchedEffect(tipo, d.fuentes) {
        tendencias = Store.fuentes.filter { tipo.cumple(it.tipo) }.take(3).flatMap { f ->
            runCatching { f.populares(1).take(10).map { f.id to it } }.getOrDefault(emptyList())
        }
    }
    val hist = d.historial.firstOrNull { tipo.cumple(Store.fuente(it.fuenteId)?.tipo) }
    val ups = d.actualizaciones.filter { tipo.cumple(Store.fuente(it.fuenteId)?.tipo) }
    val hero = when {
        hist != null -> HeroDatos(hist.item, hist.cap.nombre) { abrir(Pantalla.Leer(hist.fuenteId, hist.item, hist.cap)) }
        tendencias.isNotEmpty() -> tendencias.first().let { (fid, itm) ->
            HeroDatos(itm, Store.fuente(fid)?.nombre.orEmpty()) { abrir(Pantalla.Detalle(fid, itm)) }
        }
        else -> null
    }
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = LocalTopPad.current, bottom = LocalBarPad.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OutlinedTextField(
                q, { q = it }, Modifier.fillMaxWidth().entrada(0), singleLine = true,
                shape = RoundedCornerShape(26.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { buscar(q) }),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = cs.onSurface.copy(alpha = .12f), focusedBorderColor = cs.primary,
                    unfocusedContainerColor = cs.onSurface.copy(alpha = .06f), focusedContainerColor = cs.onSurface.copy(alpha = .08f),
                ),
            )
        }
        item { Box(Modifier.entrada(1)) { FiltroTipos(tipo, R.string.tab_home) { tipo = it } } }
        item {
            if (hero != null) Box(Modifier.entrada(2)) { Hero(hero) }
            else Tarjeta { Text(stringResource(R.string.no_sources), Modifier.padding(20.dp)) }
        }
        if (tendencias.isNotEmpty()) {
            item { CabeceraFila(R.string.trending) { buscar("") } }
            item {
                LazyRow(Modifier.entrada(3), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tendencias) { (fid, itm) ->
                        Column(Modifier.width(118.dp).rebote { abrir(Pantalla.Detalle(fid, itm)) }) {
                            AsyncImage(itm.portada, null,
                                Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(18.dp)).background(cs.surfaceVariant),
                                contentScale = ContentScale.Crop)
                            Text(itm.titulo, Modifier.padding(top = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(Store.fuente(fid)?.nombre.orEmpty(), fontSize = 11.sp, color = cs.primary, maxLines = 1)
                        }
                    }
                }
            }
        }
        item { CabeceraFila(R.string.new_episodes) { irUpdates() } }
        item {
            if (ups.isEmpty()) Text(stringResource(R.string.no_updates), color = cs.outline)
            else LazyRow(Modifier.entrada(4), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ups.take(15)) { x ->
                    Column(Modifier.width(210.dp).rebote { abrir(Pantalla.Leer(x.fuenteId, x.item, x.cap)) }) {
                        AsyncImage(x.item.portada, null,
                            Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(18.dp)).background(cs.surfaceVariant),
                            contentScale = ContentScale.Crop)
                        Text(x.item.titulo, Modifier.padding(top = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(x.cap.nombre, fontSize = 11.sp, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun CabeceraFila(titulo: Int, onClick: () -> Unit) {
    Row(Modifier.clickable(onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(titulo), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
    }
}

@Composable
private fun Hero(h: HeroDatos) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).rebote(onClick = h.onClick)
    ) {
        AsyncImage(h.item.portada, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .85f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp).fillMaxWidth(.75f)) {
            Text(h.item.titulo, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(h.sub, color = Color.White.copy(alpha = .8f), fontSize = 13.sp, maxLines = 1)
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp).size(48.dp).clip(CircleShape).background(Color.White), Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.Black)
        }
    }
}
