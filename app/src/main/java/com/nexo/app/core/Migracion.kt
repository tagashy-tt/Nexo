package com.nexo.app.core

object Migracion {
    private val num = Regex("""(\d+(?:\.\d+)?)""")
    private fun numero(nombre: String) = num.findAll(nombre).lastOrNull()?.value?.toDoubleOrNull()

    /** Cambia un título que sigues a otra fuente, conservando categoría y (opcional) capítulos leídos por número. */
    suspend fun migrar(viejo: Fav, destino: Fuente, nuevo: Item, conservarLeidos: Boolean) {
        val capsNuevos = destino.capitulos(nuevo.id)
        var leidos = Store.datos.leidos
        if (conservarLeidos) {
            val capsViejos = Store.fuente(viejo.fuenteId)?.let { runCatching { it.capitulos(viejo.item.id) }.getOrNull() }.orEmpty()
            val numerosLeidos = capsViejos.filter { "${viejo.fuenteId}:${it.id}" in Store.datos.leidos }
                .mapNotNull { numero(it.nombre) }.toSet()
            leidos = leidos + capsNuevos.filter { c -> numero(c.nombre)?.let { it in numerosLeidos } == true }
                .map { "${destino.id}:${it.id}" }
        }
        val d = Store.datos
        val keyV = "${viejo.fuenteId}:${viejo.item.id}"
        val keyN = "${destino.id}:${nuevo.id}"
        Store.guardar(d.copy(
            biblioteca = d.biblioteca.map { if (it == viejo) Fav(destino.id, nuevo, viejo.categoria) else it },
            leidos = leidos,
            conocidos = d.conocidos - keyV + (keyN to capsNuevos.map { it.id }),
            actualizaciones = d.actualizaciones.filterNot { it.fuenteId == viejo.fuenteId && it.item.id == viejo.item.id },
        ))
    }
}
