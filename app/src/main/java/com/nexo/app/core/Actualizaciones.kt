package com.nexo.app.core

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object Actualizaciones {
    /** Compara los capítulos actuales de cada título que sigues con los ya conocidos. Devuelve los nuevos. */
    suspend fun refrescar(progreso: (String) -> Unit = {}): List<Hist> {
        val nuevos = mutableListOf<Hist>()
        for (fav in Store.datos.biblioteca) {
            progreso(fav.item.titulo)
            val f = Store.fuente(fav.fuenteId) ?: continue
            val caps = runCatching { f.capitulos(fav.item.id) }.getOrNull() ?: continue
            val key = "${fav.fuenteId}:${fav.item.id}"
            val d = Store.datos
            val previos = d.conocidos[key]
            val nuevas = if (previos == null) emptyList() else caps.filter { it.id !in previos }
            val ahora = System.currentTimeMillis()
            val hist = nuevas.map { Hist(fav.fuenteId, fav.item, it, ahora) }
            nuevos += hist
            Store.guardar(d.copy(
                conocidos = d.conocidos + (key to caps.map { it.id }),
                actualizaciones = (hist + d.actualizaciones).take(300),
            ))
        }
        return nuevos
    }

    fun programar(ctx: Context, activar: Boolean) {
        val wm = WorkManager.getInstance(ctx)
        if (!activar) { wm.cancelUniqueWork("auto-updates"); return }
        val red = if (Ajustes.soloWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
        val req = PeriodicWorkRequestBuilder<ActualizacionesWorker>(Ajustes.intervaloH.toLong().coerceAtLeast(1), TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(red).build()).build()
        wm.enqueueUniquePeriodicWork("auto-updates", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class ActualizacionesWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        Store.init(applicationContext)
        Ajustes.init(applicationContext)
        Notificaciones.nuevos(applicationContext, Actualizaciones.refrescar())
        return Result.success()
    }
}
