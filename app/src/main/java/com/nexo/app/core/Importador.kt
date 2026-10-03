package com.nexo.app.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

sealed interface Importado {
    data class Fuentes(val lista: List<Manifiesto>) : Importado
    data class IndiceExt(val url: String) : Importado
    data class Apk(val archivo: File) : Importado
}

/** Acepta manifiestos Nexo (uno o varios), índices de extensiones Mihon/Aniyomi y archivos APK, sin cargar nada enorme en memoria. */
object Importador {
    private const val MAX = 12_000_000

    fun leer(i: InputStream): String {
        val out = ByteArrayOutputStream(); val buf = ByteArray(16384); var total = 0
        while (true) {
            val n = i.read(buf); if (n < 0) break
            total += n; if (total > MAX) error("Archivo demasiado grande")
            out.write(buf, 0, n)
        }
        return out.toString("UTF-8")
    }

    fun interpretar(texto: String, origen: String): Importado {
        val el = Engine.json.parseToJsonElement(texto)
        fun manifiesto(e: JsonElement) = Engine.json.decodeFromJsonElement<Manifiesto>(e)
        return when {
            el is JsonObject && "buscar" in el -> Importado.Fuentes(listOf(manifiesto(el)))
            el is JsonObject && el["fuentes"] is JsonArray -> Importado.Fuentes((el["fuentes"] as JsonArray).map { manifiesto(it) })
            el is JsonArray && el.isNotEmpty() && (el[0] as? JsonObject)?.containsKey("buscar") == true ->
                Importado.Fuentes(el.map { manifiesto(it) })
            el is JsonArray && (el.isEmpty() || (el[0] as? JsonObject)?.containsKey("pkg") == true) -> {
                if (!origen.startsWith("http")) error("El índice de extensiones debe agregarse con su URL")
                Engine.json.decodeFromJsonElement<List<ExtRepo>>(el)
                Importado.IndiceExt(origen)
            }
            else -> error("Formato no reconocido")
        }
    }

    suspend fun desdeUrl(ctx: Context, url: String): Importado = withContext(Dispatchers.IO) {
        val base = url.trim()
        require(base.startsWith("http")) { "URL no válida" }
        val limpia = base.substringBefore('?')
        val candidatos = if (limpia.endsWith(".json", true) || limpia.endsWith(".apk", true)) listOf(base)
                         else listOf(base, base.trimEnd('/') + "/index.min.json")
        var ultimo: Throwable? = null
        for (u in candidatos) {
            try {
                return@withContext Repos.http.newCall(Request.Builder().url(u).build()).execute().use { r ->
                    if (!r.isSuccessful) error("HTTP ${r.code}")
                    val esApk = u.substringBefore('?').endsWith(".apk", true) ||
                        r.header("Content-Type").orEmpty().contains("android.package-archive")
                    if (esApk) {
                        val f = Repos.archivoApk(ctx, u.substringBefore('?').substringAfterLast('/'))
                        r.body!!.byteStream().use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                        Importado.Apk(f)
                    } else interpretar(leer(r.body!!.byteStream()), u)
                }
            } catch (e: Exception) { ultimo = e }
        }
        throw ultimo ?: IllegalStateException("Sin respuesta")
    }

    suspend fun desdeArchivo(ctx: Context, uri: Uri): Importado = withContext(Dispatchers.IO) {
        val flujo = BufferedInputStream(ctx.contentResolver.openInputStream(uri) ?: error("No se pudo abrir el archivo"))
        flujo.use { i ->
            i.mark(4); val a = i.read(); val b = i.read(); i.reset()
            if (a == 0x50 && b == 0x4B) { // "PK": un APK
                val f = Repos.archivoApk(ctx, "local-${System.currentTimeMillis()}.apk")
                f.outputStream().use { o -> i.copyTo(o) }
                Importado.Apk(f)
            } else interpretar(leer(i), uri.toString())
        }
    }
}
