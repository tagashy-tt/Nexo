package com.nexo.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.nexo.app.core.Ajustes
import com.nexo.app.core.Bloqueo
import com.nexo.app.core.Store
import com.nexo.app.ui.NexoApp
import com.nexo.app.ui.NexoTheme
import com.nexo.app.ui.PantallaBloqueo

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(applicationContext)
        Ajustes.init(applicationContext)
        aplicarSeguro()
        Bloqueo.alIniciar()
        setContent {
            NexoTheme {
                if (Bloqueo.bloqueado) PantallaBloqueo { Bloqueo.pedir(this@MainActivity) } else NexoApp()
            }
        }
    }

    override fun onStart() { super.onStart(); Bloqueo.alVolver() }
    override fun onStop() { super.onStop(); Bloqueo.alSalir() }

    /** Oculta el contenido en "apps recientes" y bloquea capturas de pantalla. */
    fun aplicarSeguro() {
        if (Ajustes.pantallaSegura) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
