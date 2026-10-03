package com.nexo.app.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nexo.app.MainActivity
import com.nexo.app.R

object Notificaciones {
    private const val CANAL = "updates"

    fun nuevos(ctx: Context, lista: List<Hist>) {
        if (!Ajustes.notificaciones || lista.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ctx.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CANAL, ctx.getString(R.string.tab_updates), NotificationManager.IMPORTANCE_DEFAULT))
        val oculto = Ajustes.ocultarNotif
        val generica = ctx.getString(R.string.notif_generic)
        val titulo = if (oculto) ctx.getString(R.string.app_name) else lista.first().item.titulo
        val texto = when {
            oculto -> generica
            lista.size == 1 -> lista.first().cap.nombre
            else -> ctx.getString(R.string.notif_many, lista.size)
        }
        val publica = NotificationCompat.Builder(ctx, CANAL).setSmallIcon(R.drawable.ic_stat_nexo)
            .setContentTitle(ctx.getString(R.string.app_name)).setContentText(generica).build()
        val abrir = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CANAL).setSmallIcon(R.drawable.ic_stat_nexo)
            .setContentTitle(titulo).setContentText(texto).setAutoCancel(true).setContentIntent(abrir)
            .setVisibility(if (oculto) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setPublicVersion(publica).build()
        NotificationManagerCompat.from(ctx).notify(1001, n)
    }
}
