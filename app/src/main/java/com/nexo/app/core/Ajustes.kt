package com.nexo.app.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*

object Ajustes {
    private lateinit var sp: SharedPreferences
    var modo by mutableIntStateOf(2)            // 0 sistema, 1 claro, 2 oscuro
    var amoled by mutableStateOf(true)
    var acento by mutableIntStateOf(0xFF66FF4D.toInt()) // verde por defecto
    var incognito by mutableStateOf(false)
    var soloDescargados by mutableStateOf(false)
    var autoUpdates by mutableStateOf(false)
    var destino by mutableIntStateOf(0)         // 0 preguntar, 1 en la app, 2 galería

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
        modo = sp.getInt("modo", 2); amoled = sp.getBoolean("amoled", true)
        acento = sp.getInt("acento", acento); incognito = sp.getBoolean("incognito", false)
        soloDescargados = sp.getBoolean("solo", false); autoUpdates = sp.getBoolean("auto", false)
        destino = sp.getInt("destino", 0)
    }

    fun guardar() {
        sp.edit().putInt("modo", modo).putBoolean("amoled", amoled).putInt("acento", acento)
            .putBoolean("incognito", incognito).putBoolean("solo", soloDescargados)
            .putBoolean("auto", autoUpdates).putInt("destino", destino).apply()
    }
}
