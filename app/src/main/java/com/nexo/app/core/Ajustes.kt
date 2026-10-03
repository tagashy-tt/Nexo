package com.nexo.app.core

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*

object Ajustes {
    private lateinit var sp: SharedPreferences
    // Apariencia
    var modo by mutableIntStateOf(2)                    // 0 sistema, 1 claro, 2 oscuro
    var amoled by mutableStateOf(true)
    var acento by mutableIntStateOf(0xFF66FF4D.toInt()) // verde por defecto
    var animaciones by mutableStateOf(true)
    var fondoAnimado by mutableStateOf(true)
    var efectosCristal by mutableStateOf(true)
    // Biblioteca / descargas
    var incognito by mutableStateOf(false)
    var soloDescargados by mutableStateOf(false)
    var destino by mutableIntStateOf(0)                 // 0 preguntar, 1 app, 2 galería
    var soloWifi by mutableStateOf(false)
    // Lector / reproductor
    var lectorModo by mutableIntStateOf(0)              // 0 vertical, 1 paginado
    var pantallaEncendida by mutableStateOf(true)
    var velocidad by mutableFloatStateOf(1f)
    // Actualizaciones
    var autoUpdates by mutableStateOf(false)
    var intervaloH by mutableIntStateOf(12)
    var notificaciones by mutableStateOf(false)
    // Seguridad
    var ocultarNotif by mutableStateOf(false)
    var bloqueo by mutableStateOf(false)
    var bloqueoMin by mutableIntStateOf(0)              // 0 inmediato, 1 → 1 min, 2 → 5 min
    var pantallaSegura by mutableStateOf(false)
    // App
    var repoUpdates by mutableStateOf("")
    var autoCheckApp by mutableStateOf(true)
    var ultimaComprobacion = 0L
    var versionOmitida = ""

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
        val poca = ctx.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
        modo = sp.getInt("modo", 2); amoled = sp.getBoolean("amoled", true); acento = sp.getInt("acento", acento)
        animaciones = sp.getBoolean("anim", true); fondoAnimado = sp.getBoolean("fondo", !poca)
        efectosCristal = sp.getBoolean("efectos", !poca)
        incognito = sp.getBoolean("incognito", false); soloDescargados = sp.getBoolean("solo", false)
        destino = sp.getInt("destino", 0); soloWifi = sp.getBoolean("wifi", false)
        lectorModo = sp.getInt("lector", 0); pantallaEncendida = sp.getBoolean("encendida", true)
        velocidad = sp.getFloat("velocidad", 1f)
        autoUpdates = sp.getBoolean("auto", false); intervaloH = sp.getInt("intervalo", 12)
        notificaciones = sp.getBoolean("notif", false); ocultarNotif = sp.getBoolean("ocultar", false)
        bloqueo = sp.getBoolean("bloqueo", false); bloqueoMin = sp.getInt("bloqueoMin", 0)
        pantallaSegura = sp.getBoolean("segura", false)
        repoUpdates = sp.getString("repo", "").orEmpty(); autoCheckApp = sp.getBoolean("autoApp", true)
        ultimaComprobacion = sp.getLong("ultima", 0L); versionOmitida = sp.getString("omitida", "").orEmpty()
    }

    fun guardar() {
        sp.edit().putInt("modo", modo).putBoolean("amoled", amoled).putInt("acento", acento)
            .putBoolean("anim", animaciones).putBoolean("fondo", fondoAnimado).putBoolean("efectos", efectosCristal)
            .putBoolean("incognito", incognito).putBoolean("solo", soloDescargados)
            .putInt("destino", destino).putBoolean("wifi", soloWifi)
            .putInt("lector", lectorModo).putBoolean("encendida", pantallaEncendida).putFloat("velocidad", velocidad)
            .putBoolean("auto", autoUpdates).putInt("intervalo", intervaloH)
            .putBoolean("notif", notificaciones).putBoolean("ocultar", ocultarNotif)
            .putBoolean("bloqueo", bloqueo).putInt("bloqueoMin", bloqueoMin).putBoolean("segura", pantallaSegura)
            .putString("repo", repoUpdates).putBoolean("autoApp", autoCheckApp)
            .putLong("ultima", ultimaComprobacion).putString("omitida", versionOmitida).apply()
    }
}
