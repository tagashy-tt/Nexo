package com.nexo.app.core

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object Actualizaciones {
    /** Compara los capítulos actuales de cada título de la biblioteca con los ya conocidos. */
    suspend fun refrescar(progreso: (String) -> Unit = {}) {
        for (fav in Store.datos.biblioteca) {
            progreso(fav.item.titulo)
            val f = Store.fuente(fav.fuenteId) ?: continue
            val caps = runCatching { f.capitulos(fav.item.id) }.getOrNull() ?: continue
            val key = "${fav.fuenteId}:${fav.item.id}"
            val d = Store.datos
            val previos = d.conocidos[key]
            val nuevas = if (previos == null) emptyList() else caps.filter { it.id !in previos }
            val ahora = System.currentTimeMillis()
            Store.guardar(d.copy(
                conocidos = d.conocidos + (key to caps.map { it.id }),
                actualizaciones = (nuevas.map { Hist(fav.fuenteId, fav.item, it, ahora) } + d.actualizaciones).take(300),
            ))
        }
    }

    fun programar(ctx: Context, activar: Boolean) {
        val wm = WorkManager.getInstance(ctx)
        if (!activar) { wm.cancelUniqueWork("auto-updates"); return }
        val req = PeriodicWorkRequestBuilder<ActualizacionesWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        wm.enqueueUniquePeriodicWork("auto-updates", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class ActualizacionesWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        Store.init(applicationContext)
        Actualizaciones.refrescar()
        return Result.success()
    }
}
