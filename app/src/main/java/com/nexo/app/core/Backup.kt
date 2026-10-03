@file:OptIn(ExperimentalSerializationApi::class)
package com.nexo.app.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
<<<<<<< Updated upstream
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.decodeFromByteArray
=======
>>>>>>> Stashed changes
import kotlinx.serialization.encodeToString
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import java.util.zip.GZIPInputStream

// Esquema mínimo del .tachibk de Mihon/Aniyomi (los campos desconocidos se ignoran).
@Serializable class MihonBackup(@ProtoNumber(1) val items: List<MihonItem> = emptyList())
@Serializable class MihonItem(@ProtoNumber(3) val title: String = "", @ProtoNumber(9) val thumbnailUrl: String? = null)

object Backup {
    private const val MAX = 64_000_000

    suspend fun exportar(ctx: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val texto = Engine.json.encodeToString(Store.datos)
        ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(texto.toByteArray()) } ?: error("No se pudo escribir el archivo")
    }

    /** Restaura un .nexo.json o importa un .tachibk. Todo error se devuelve como excepción normal (la UI lo muestra, no cierra la app). */
    suspend fun importar(ctx: Context, uri: Uri, progreso: (String) -> Unit): String {
        val bytes = withContext(Dispatchers.IO) {
            ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("No se pudo abrir el archivo")
        }
        if (bytes.size > MAX) error("Archivo demasiado grande")
        val gzip = bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
        return if (gzip) importarMihon(bytes, progreso) else importarNexo(bytes)
    }

    private suspend fun importarNexo(bytes: ByteArray): String {
        val d = withContext(Dispatchers.Default) { Engine.json.decodeFromString<Datos>(String(bytes)) }
        withContext(Dispatchers.Main) {
            val a = Store.datos
            Store.guardar(a.copy(
                fuentes = a.fuentes.filter { f -> d.fuentes.none { it.id == f.id } } + d.fuentes,
                biblioteca = a.biblioteca + d.biblioteca.filter { n -> a.biblioteca.none { it.fuenteId == n.fuenteId && it.item.id == n.item.id } },
                leidos = a.leidos + d.leidos,
                categorias = (a.categorias + d.categorias).distinct(),
                historial = (d.historial + a.historial).distinctBy { it.fuenteId + it.cap.id }.take(200),
                repos = (a.repos + d.repos).distinct(),
            ))
        }
        return "OK"
    }

    private suspend fun importarMihon(bytes: ByteArray, progreso: (String) -> Unit): String {
        val b = withContext(Dispatchers.Default) {
            ProtoBuf.decodeFromByteArray<MihonBackup>(GZIPInputStream(bytes.inputStream()).use { it.readBytes() })
        }
        val titulos = b.items.map { it.title.trim() }.filter { it.isNotEmpty() }.distinct()
        val fuentes = Store.fuentes
        val sem = Semaphore(4)
        var hechos = 0
        val hallados = coroutineScope {
            titulos.map { t ->
                async(Dispatchers.IO) {
                    sem.withPermit {
                        val hit = fuentes.firstNotNullOfOrNull { f ->
                            runCatching { f.buscar(t).firstOrNull { it.titulo.equals(t, true) } }.getOrNull()?.let { f.id to it }
                        }
                        withContext(Dispatchers.Main) { hechos++; progreso("$hechos/${titulos.size}") }
                        hit
                    }
                }
            }.awaitAll()
        }.filterNotNull()
        withContext(Dispatchers.Main) {
            var bib = Store.datos.biblioteca
            hallados.forEach { (fid, item) -> if (bib.none { it.fuenteId == fid && it.item.id == item.id }) bib = bib + Fav(fid, item) }
            Store.guardar(Store.datos.copy(biblioteca = bib))
        }
        return "${hallados.size}/${titulos.size}"
    }
}
