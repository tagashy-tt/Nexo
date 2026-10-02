package com.nexo.app.core

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.work.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object Descargas {
    fun carpeta(ctx: Context, capId: String) =
        File(ctx.filesDir, "descargas/" + capId.replace(Regex("[^A-Za-z0-9]"), "_").take(80))

    fun archivos(ctx: Context, capId: String): List<File> =
        carpeta(ctx, capId).listFiles { f -> f.name != "titulo.txt" && !f.name.endsWith(".tmp") }
            ?.sortedBy { it.name }.orEmpty()

    fun todas(ctx: Context): List<Pair<File, String>> =
        File(ctx.filesDir, "descargas").listFiles()?.filter { it.isDirectory }?.map {
            it to File(it, "titulo.txt").takeIf { t -> t.exists() }?.readText().orEmpty().ifEmpty { it.name }
        }.orEmpty()

    /** destino: "app" (almacenamiento interno de Nexo) o "galeria" (MediaStore, visible en la galería). */
    fun encolar(ctx: Context, fuenteId: String, cap: Capitulo, titulo: String, destino: String) {
        val nombre = "$titulo - ${cap.nombre}"
        val req = OneTimeWorkRequestBuilder<DescargaWorker>()
            .addTag("descarga").addTag("t:$nombre").addTag("d:$destino")
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf("fuente" to fuenteId, "cap" to cap.id, "titulo" to nombre, "destino" to destino))
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("$destino:${cap.id}", ExistingWorkPolicy.KEEP, req)
    }

    private val prohibidos = Regex("[\\\\/:*?\"<>|]")
    fun limpio(s: String) = s.replace(prohibidos, "_").take(100)
    private val videoExt = setOf("mp4", "mkv", "webm", "m4v", "mov", "avi", "ts")

    fun guardarEnGaleria(ctx: Context, http: OkHttpClient, url: String, base: String, i: Int, total: Int) {
        val ext = url.substringBefore('?').substringAfterLast('.', "bin").lowercase().take(4)
        val mimeBase = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        val esVideo = ext in videoExt || mimeBase?.startsWith("video") == true
        val esImagen = mimeBase?.startsWith("image") == true
        val mime = mimeBase ?: if (esVideo) "video/mp4" else "application/octet-stream"
        val (coleccion, ruta) = when {
            esVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "Movies/Nexo"
            esImagen -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "Pictures/Nexo/${limpio(base)}"
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI to "Download/Nexo"
        }
        val nombre = when {
            esImagen -> "%04d.%s".format(i, ext)
            total > 1 -> "${limpio(base)}-${i + 1}.$ext"
            else -> "${limpio(base)}.$ext"
        }
        val valores = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, ruta)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val res = ctx.contentResolver
        val uri = res.insert(coleccion, valores) ?: error("MediaStore")
        try {
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) error("HTTP ${r.code}")
                res.openOutputStream(uri)!!.use { out -> r.body!!.byteStream().copyTo(out) }
            }
            valores.clear(); valores.put(MediaStore.MediaColumns.IS_PENDING, 0)
            res.update(uri, valores, null, null)
        } catch (e: Exception) { res.delete(uri, null, null); throw e }
    }
}

class DescargaWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    private val http = OkHttpClient()
    override suspend fun doWork(): Result {
        Store.init(applicationContext)
        val capId = inputData.getString("cap") ?: return Result.failure()
        val fuente = Store.fuente(inputData.getString("fuente") ?: "") ?: return Result.failure()
        val titulo = inputData.getString("titulo").orEmpty()
        return try {
            val urls = fuente.contenido(capId)
            if (inputData.getString("destino") == "galeria") {
                urls.forEachIndexed { i, u -> Descargas.guardarEnGaleria(applicationContext, http, u, titulo.ifEmpty { capId }, i, urls.size) }
                return Result.success()
            }
            val dir = Descargas.carpeta(applicationContext, capId).also { it.mkdirs() }
            File(dir, "titulo.txt").writeText(titulo)
            urls.forEachIndexed { i, u ->
                val ext = u.substringBefore('?').substringAfterLast('.', "bin").take(4)
                val destino = File(dir, "%04d.%s".format(i, ext))
                if (destino.exists()) return@forEachIndexed
                val tmp = File(dir, destino.name + ".tmp")
                http.newCall(Request.Builder().url(u).build()).execute().use { r ->
                    if (!r.isSuccessful) error("HTTP ${r.code}")
                    r.body!!.byteStream().use { inp -> tmp.outputStream().use { inp.copyTo(it) } }
                }
                tmp.renameTo(destino)
            }
            Result.success()
        } catch (e: Exception) { Result.retry() }
    }
}
