package com.example.poketeambuilder

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

class App : Application() {
    override fun onCreate() {
        super.onCreate()

    // Configurar Coil ImageLoader con cachés de memoria y disco
        val imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .diskCache {
                // Caché en disco de 50 MB en el directorio de caché de la app
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L)
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // usar hasta 25% de la memoria disponible para imágenes
                    .build()
            }
            .build()

        Coil.setImageLoader(imageLoader)
    }
}
