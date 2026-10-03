package com.nexo.app.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable data class Fav(val fuenteId: String, val item: Item, val categoria: String? = null)
@Serializable data class Hist(val fuenteId: String, val item: Item, val cap: Capitulo, val fecha: Long)

@Serializable
data class Datos(
    val fuentes: List<Manifiesto> = emptyList(),
    val biblioteca: List<Fav> = emptyList(),          // títulos que sigues
    val leidos: Set<String> = emptySet(),             // "fuenteId:capituloId"
    val categorias: List<String> = emptyList(),
    val conocidos: Map<String, List<String>> = emptyMap(),
    val actualizaciones: List<Hist> = emptyList(),
    val historial: List<Hist> = emptyList(),
    val repos: List<String> = emptyList(),            // índices de extensiones (URL)
)

object Store {
    private lateinit var dir: File
    var datos by mutableStateOf(Datos())
    private val compacto = Json(Engine.json) { prettyPrint = false }
    private var cacheKey: List<Manifiesto>? = null
    private var cacheMapa: Map<String, Fuente> = emptyMap()

    fun init(ctx: Context) {
        if (::dir.isInitialized) return
        dir = ctx.applicationContext.filesDir
        val f = File(dir, "datos.json")
        if (f.exists()) runCatching { datos = Engine.json.decodeFromString<Datos>(f.readText()) }
            .onFailure { f.renameTo(File(dir, "datos.json.bad")) } // un archivo dañado no debe tumbar la app
    }

    fun guardar(d: Datos) {
        datos = d
        val tmp = File(dir, "datos.json.tmp")
        tmp.writeText(compacto.encodeToString(d))
        tmp.renameTo(File(dir, "datos.json"))
    }

    fun agregar(m: Manifiesto) = guardar(datos.copy(fuentes = datos.fuentes.filter { it.id != m.id } + m))

    // Caché: antes se recreaban todas las fuentes en cada consulta (lento en listas largas).
    private fun mapa(): Map<String, Fuente> {
        val k = datos.fuentes
        if (cacheKey !== k) { cacheMapa = k.associate { it.id to FuenteManifiesto(it) }; cacheKey = k }
        return cacheMapa
    }
    val fuentes: List<Fuente> get() = mapa().values.toList()
    fun fuente(id: String): Fuente? = mapa()[id]
}
