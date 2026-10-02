@file:OptIn(ExperimentalSerializationApi::class)
package com.nexo.app.core

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import java.util.zip.GZIPInputStream

// Esquema mínimo del .tachibk de Mihon/Aniyomi (campos desconocidos se ignoran).
@Serializable class MihonBackup(@ProtoNumber(1) val items: List<MihonItem> = emptyList())
@Serializable class MihonItem(@ProtoNumber(3) val title: String = "", @ProtoNumber(9) val thumbnailUrl: String? = null)

object Backup {
    fun exportar(ctx: Context, uri: Uri) {
        ctx.contentResolver.openOutputStream(uri)!!.use {
            it.write(Engine.json.encodeToString(Store.datos).toByteArray())
        }
    }

    suspend fun importar(ctx: Context, uri: Uri, progreso: (String) -> Unit): String = withContext(Dispatchers.IO) {
        val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        if (bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            val b = ProtoBuf.decodeFromByteArray<MihonBackup>(GZIPInputStream(bytes.inputStream()).readBytes())
            var bib = Store.datos.biblioteca
            var ok = 0
            b.items.forEachIndexed { i, mg ->
                progreso("${i + 1}/${b.items.size}")
                val hit = Store.fuentes.firstNotNullOfOrNull { f ->
                    runCatching { f.buscar(mg.title).firstOrNull { it.titulo.equals(mg.title, true) } }
                        .getOrNull()?.let { f.id to it }
                }
                if (hit != null && bib.none { it.fuenteId == hit.first && it.item.id == hit.second.id }) {
                    bib = bib + Fav(hit.first, hit.second); ok++
                }
            }
            Store.guardar(Store.datos.copy(biblioteca = bib))
            "Importados $ok de ${b.items.size}. El resto no existe en tus fuentes activas."
        } else {
            val d = Engine.json.decodeFromString<Datos>(String(bytes))
            val a = Store.datos
            Store.guardar(a.copy(
                fuentes = a.fuentes.filter { f -> d.fuentes.none { it.id == f.id } } + d.fuentes,
                biblioteca = a.biblioteca + d.biblioteca.filter { n -> a.biblioteca.none { it.fuenteId == n.fuenteId && it.item.id == n.item.id } },
                leidos = a.leidos + d.leidos,
                categorias = (a.categorias + d.categorias).distinct(),
                historial = (d.historial + a.historial).distinctBy { it.fuenteId + it.cap.id }.take(200),
            ))
            "Backup restaurado."
        }
    }
}

object Extensiones {
    /** Detecta extensiones de Mihon/Aniyomi instaladas (aún no se ejecutan). */
    @Suppress("DEPRECATION")
    fun detectar(ctx: Context): List<String> {
        val pm = ctx.packageManager
        return pm.getInstalledPackages(PackageManager.GET_CONFIGURATIONS)
            .filter { p -> p.reqFeatures?.any { it.name == "tachiyomi.extension" || it.name == "tachiyomi.animeextension" } == true }
            .map { p -> p.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: p.packageName }
    }
}
