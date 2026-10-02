package com.nexo.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.nexo.app.core.Ajustes
import com.nexo.app.core.Store
import com.nexo.app.ui.NexoApp
import com.nexo.app.ui.NexoTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(applicationContext)
        Ajustes.init(applicationContext)
        setContent { NexoTheme { NexoApp() } }
    }
}
