package com.nexo.app.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable data class Fav(val fuenteId: String, val item: Item, val categoria: String? = null)
@Serializable data class Hist(val fuenteId: String, val item: Item, val cap: Capitulo, val fecha: Long)

@Serializable
data class Datos(
    val fuentes: List<Manifiesto> = emptyList(),
    val biblioteca: List<Fav> = emptyList(),
    val leidos: Set<String> = emptySet(), // "fuenteId:capituloId"
    val categorias: List<String> = emptyList(),
    val conocidos: Map<String, List<String>> = emptyMap(), // capítulos vistos por título
    val actualizaciones: List<Hist> = emptyList(),
    val historial: List<Hist> = emptyList(),
)

object Store {
    private lateinit var dir: File
    var datos by mutableStateOf(Datos())

    fun init(ctx: Context) {
        if (::dir.isInitialized) return
        dir = ctx.applicationContext.filesDir
        File(dir, "datos.json").takeIf { it.exists() }?.let {
            runCatching { datos = Engine.json.decodeFromString(it.readText()) }
        }
    }

    fun guardar(d: Datos) {
        datos = d
        File(dir, "datos.json").writeText(Engine.json.encodeToString(d))
    }

    fun agregar(m: Manifiesto) =
        guardar(datos.copy(fuentes = datos.fuentes.filter { it.id != m.id } + m))

    val fuentes: List<Fuente> get() = datos.fuentes.map(::FuenteManifiesto)
    fun fuente(id: String): Fuente? = fuentes.firstOrNull { it.id == id }
}
