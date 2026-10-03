package com.nexo.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NexoApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        val previo = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                File(filesDir, "crash.txt").writeText("$fecha\n${e.stackTraceToString()}")
            }
            previo?.uncaughtException(t, e)
        }
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .crossfade(true)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .diskCache { DiskCache.Builder().directory(File(cacheDir, "img")).maxSizeBytes(150L * 1024 * 1024).build() }
        .build()
}
