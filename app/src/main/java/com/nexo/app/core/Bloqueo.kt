package com.nexo.app.core

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.nexo.app.R

object Bloqueo {
    var bloqueado by mutableStateOf(false)
    private var salida = 0L
    private var desbloqueo = 0L

    private fun auth() = if (Build.VERSION.SDK_INT >= 30) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK
    fun disponible(ctx: Context) = BiometricManager.from(ctx).canAuthenticate(auth()) == BiometricManager.BIOMETRIC_SUCCESS

    fun alIniciar() { if (Ajustes.bloqueo) bloqueado = true }
    fun alSalir() { salida = SystemClock.elapsedRealtime() }
    fun alVolver() {
        val ahora = SystemClock.elapsedRealtime()
        if (!Ajustes.bloqueo || bloqueado || ahora - desbloqueo < 2000) return
        val minutos = longArrayOf(0, 1, 5)[Ajustes.bloqueoMin.coerceIn(0, 2)]
        if (ahora - salida >= minutos * 60_000) bloqueado = true
    }

    fun pedir(act: FragmentActivity) {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(act.getString(R.string.unlock_title))
            .setAllowedAuthenticators(auth())
            .apply { if (Build.VERSION.SDK_INT < 30) setNegativeButtonText(act.getString(R.string.cancel)) }
            .build()
        BiometricPrompt(act, ContextCompat.getMainExecutor(act), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                desbloqueo = SystemClock.elapsedRealtime(); bloqueado = false
            }
        }).authenticate(info)
    }
}
