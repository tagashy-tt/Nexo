package com.nexo.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request

@Serializable data class GhAsset(val name: String = "", val browser_download_url: String = "")
@Serializable data class GhRelease(val tag_name: String = "", val body: String? = null, val assets: List<GhAsset> = emptyList())
data class NuevaVersion(val version: String, val notas: String, val apkUrl: String)

sealed interface ResUpd {
    object SinConfig : ResUpd
    object AlDia : ResUpd
    data class Nueva(val v: NuevaVersion) : ResUpd
}

/** Busca el último release de GitHub y, si es más nuevo, descarga el APK y abre el instalador del sistema. */
object Actualizador {
    fun versionActual(ctx: Context): String =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"

    fun repo(): String = Ajustes.repoUpdates.trim().ifBlank { Config.REPO_ACTUALIZACIONES }

    private fun partes(v: String) = Regex("\\d+").findAll(v).map { it.value.toInt() }.toList()
    fun esMayor(a: String, b: String): Boolean {
        val x = partes(a); val y = partes(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    suspend fun buscar(ctx: Context): ResUpd = withContext(Dispatchers.IO) {
        val r = repo()
        if (!Regex("[\\w.-]+/[\\w.-]+").matches(r)) return@withContext ResUpd.SinConfig
        val req = Request.Builder().url("https://api.github.com/repos/$r/releases/latest")
            .header("Accept", "application/vnd.github+json").build()
        val rel = Repos.http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            Engine.json.decodeFromString<GhRelease>(Importador.leer(resp.body!!.byteStream()))
        }
        val apk = rel.assets.firstOrNull { it.name.endsWith(".apk", true) }
        if (apk != null && esMayor(rel.tag_name, versionActual(ctx)))
            ResUpd.Nueva(NuevaVersion(rel.tag_name.removePrefix("v"), rel.body.orEmpty().take(600), apk.browser_download_url))
        else ResUpd.AlDia
    }

    /** true = instalador abierto; false = falta permitir "instalar apps desconocidas" a Nexo. */
    suspend fun descargarEInstalar(ctx: Context, n: NuevaVersion): Boolean =
        Repos.instalar(ctx, Repos.descargarApk(ctx, n.apkUrl, "nexo-${n.version}.apk"))
}
