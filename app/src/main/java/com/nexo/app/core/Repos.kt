package com.nexo.app.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

// Formato del índice de repositorios de extensiones de Mihon/Aniyomi (index.min.json).
@Serializable data class ExtSource(val name: String = "", val lang: String = "", val baseUrl: String = "")
@Serializable
data class ExtRepo(
    val name: String = "", val pkg: String = "", val apk: String = "", val lang: String = "",
    val version: String = "", val nsfw: Int = 0, val sources: List<ExtSource> = emptyList(),
) {
    val esAnime get() = ".animeextension" in pkg
    val titulo get() = name.removePrefix("Tachiyomi: ").removePrefix("Aniyomi: ")
}

data class ExtDisponible(val ext: ExtRepo, val indiceUrl: String) {
    val apkUrl get() = indiceUrl.substringBeforeLast("/") + "/apk/" + ext.apk
}
data class ExtInstalada(val nombre: String, val pkg: String, val anime: Boolean, val version: String)

object Repos {
    val http = OkHttpClient()

    fun archivoApk(ctx: Context, nombre: String): File =
        File(ctx.cacheDir, "apk").apply { mkdirs() }.resolve(nombre.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "ext.apk" })

    suspend fun indice(url: String): List<ExtDisponible> = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            Engine.json.decodeFromString<List<ExtRepo>>(Importador.leer(r.body!!.byteStream())).map { ExtDisponible(it, url) }
        }
    }

    suspend fun descargarApk(ctx: Context, url: String, nombre: String = url.substringBefore('?').substringAfterLast('/')): File =
        withContext(Dispatchers.IO) {
            val f = archivoApk(ctx, nombre)
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) error("HTTP ${r.code}")
                r.body!!.byteStream().use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            }
            f
        }

    /** false = el sistema aún no permite instalar apps desde Nexo (se abre el ajuste correspondiente). */
    fun instalar(ctx: Context, apk: File): Boolean {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", apk)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    fun desinstalar(ctx: Context, pkg: String) =
        ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    @Suppress("DEPRECATION")
    fun detectar(ctx: Context): List<ExtInstalada> {
        val pm = ctx.packageManager
        return pm.getInstalledPackages(PackageManager.GET_CONFIGURATIONS)
            .filter { p -> p.reqFeatures?.any { it.name == "tachiyomi.extension" || it.name == "tachiyomi.animeextension" } == true }
            .map { p ->
                val nombre = p.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: p.packageName
                ExtInstalada(nombre, p.packageName, ".animeextension" in p.packageName, p.versionName.orEmpty())
            }
    }
}
