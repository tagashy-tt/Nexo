package com.nexo.app.core

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class Endpoint(
    val ruta: String = "",                       // vacío = sin petición
    val params: Map<String, String> = emptyMap(),
    val lista: String = "",                      // ruta JSON a la lista
    val filtro: String = "",                     // "campo~regex"
    val campos: Map<String, String> = emptyMap(),// plantillas con {ruta.json}
)

@Serializable
data class Manifiesto(
    val id: String, val nombre: String, val version: String = "1",
    val tipo: String, val baseUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val buscar: Endpoint, val capitulos: Endpoint, val contenido: Endpoint,
    val populares: Endpoint? = null, // opcional: listado de tendencias
)

/** Variables: {$q} {$page} {$offset} {$id} {$id0} {$id1}; {$root.x} {$item}; "a|b" = alternativas. */
object Engine {
    private val http = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val tpl = Regex("""\{([^}]+)}""")
    private val filtroSeg = Regex("""(\w+)?\[(\w+)=([^\]]+)]""")

    fun texto(e: JsonElement?): String? = (e as? JsonPrimitive)?.contentOrNull

    fun path(e: JsonElement?, p: String): JsonElement? {
        var cur = e
        if (p.isEmpty()) return cur
        for (seg in p.split(".")) {
            val m = filtroSeg.matchEntire(seg)
            cur = if (m != null) {
                val base = if (m.groupValues[1].isNotEmpty()) (cur as? JsonObject)?.get(m.groupValues[1]) else cur
                (base as? JsonArray)?.firstOrNull {
                    texto((it as? JsonObject)?.get(m.groupValues[2])) == m.groupValues[3]
                }
            } else when (cur) {
                is JsonObject -> cur[seg]
                is JsonArray -> seg.toIntOrNull()?.let { cur.getOrNull(it) }
                else -> null
            }
            if (cur == null) return null
        }
        return cur
    }

    fun sub(t: String, root: JsonElement, item: JsonElement, vars: Map<String, String>): String =
        tpl.replace(t) { m ->
            m.groupValues[1].split("|").firstNotNullOfOrNull { k ->
                (vars[k] ?: when {
                    k == "\$item" -> texto(item)
                    k.startsWith("\$root.") -> texto(path(root, k.removePrefix("\$root.")))
                    else -> texto(path(item, k))
                })?.takeIf { it.isNotEmpty() }
            }.orEmpty()
        }

    suspend fun run(m: Manifiesto, ep: Endpoint, vars: Map<String, String>): List<Map<String, String>> =
        withContext(Dispatchers.IO) {
            val vacio = JsonObject(emptyMap())
            val root: JsonElement = if (ep.ruta.isEmpty()) vacio else {
                val url = (m.baseUrl + sub(ep.ruta, vacio, vacio, vars)).toHttpUrl().newBuilder()
                ep.params.forEach { (k, v) -> url.addQueryParameter(k, sub(v, vacio, vacio, vars)) }
                val rb = Request.Builder().url(url.build())
                m.headers.forEach { (k, v) -> rb.header(k, v) }
                http.newCall(rb.build()).execute().use { r ->
                    if (!r.isSuccessful) error("HTTP ${r.code}")
                    json.parseToJsonElement(r.body!!.string())
                }
            }
            val filas = if (ep.lista.isEmpty()) listOf(root)
                        else (path(root, ep.lista) as? JsonArray)?.toList().orEmpty()
            val f = ep.filtro.split("~", limit = 2)
            filas.filter { f.size < 2 || Regex(f[1]).matches(texto(path(it, f[0])).orEmpty()) }
                .map { fila -> ep.campos.mapValues { (_, v) -> sub(v, root, fila, vars) } }
        }

    suspend fun descargar(url: String): Manifiesto = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            json.decodeFromString<Manifiesto>(r.body!!.string())
        }
    }
}

class FuenteManifiesto(val m: Manifiesto) : Fuente {
    override val id get() = m.id
    override val nombre get() = m.nombre
    override val tipo get() = m.tipo

    private fun ids(id: String): Map<String, String> {
        val p = id.split("|")
        return mapOf("\$id" to id, "\$id0" to p[0], "\$id1" to Uri.encode(p.getOrElse(1) { "" }, "/"))
    }

    private fun items(filas: List<Map<String, String>>) = filas.mapNotNull { r ->
        val id = r["id"]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        Item(id, r["titulo"].orEmpty().ifEmpty { id }, r["portada"]?.takeIf { it.isNotEmpty() })
    }

    private fun pag(pagina: Int, q: String = "") =
        mapOf("\$q" to q, "\$page" to "$pagina", "\$offset" to "${(pagina - 1) * 20}")

    override suspend fun buscar(texto: String, pagina: Int) = items(Engine.run(m, m.buscar, pag(pagina, texto)))

    override suspend fun populares(pagina: Int) =
        items(Engine.run(m, m.populares ?: m.buscar, pag(pagina)))

    override suspend fun capitulos(id: String) =
        Engine.run(m, m.capitulos, ids(id)).mapNotNull { r ->
            val cid = r["id"]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Capitulo(cid, r["nombre"].orEmpty().ifEmpty { cid })
        }

    override suspend fun contenido(capituloId: String) =
        Engine.run(m, m.contenido, ids(capituloId)).mapNotNull { it["url"]?.takeIf { u -> u.isNotEmpty() } }
}
