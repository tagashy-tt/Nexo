package com.nexo.app.core

import kotlinx.serialization.Serializable

@Serializable data class Item(val id: String, val titulo: String, val portada: String? = null)
@Serializable data class Capitulo(val id: String, val nombre: String)

/** Interfaz común: la implementan los manifiestos JSON (y, en el futuro, extensiones). */
interface Fuente {
    val id: String
    val nombre: String
    val tipo: String // manga | libro | anime | pelicula
    suspend fun buscar(texto: String, pagina: Int = 1): List<Item>
    suspend fun populares(pagina: Int = 1): List<Item>
    suspend fun capitulos(id: String): List<Capitulo>
    suspend fun contenido(capituloId: String): List<String> // URLs de páginas o de video
}
