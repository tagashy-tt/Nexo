package com.nexo.app.ui

import android.text.format.DateUtils
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexo.app.R
import com.nexo.app.core.Item

fun esVideo(t: String?) = t == "anime" || t == "pelicula" || t == "serie"

fun fechaRelativa(ms: Long): String =
    DateUtils.getRelativeTimeSpanString(ms, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

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
            val ancho by animateDpAsState(
                if (activo) 28.dp else 0.dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "linea")
            Column(Modifier.clickable { onSel(t) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(if (t == TipoTab.Home) todoLabel else t.label),
                    color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                    fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal,
                )
                Box(Modifier.padding(top = 4.dp).height(2.dp).width(ancho).background(MaterialTheme.colorScheme.primary))
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
