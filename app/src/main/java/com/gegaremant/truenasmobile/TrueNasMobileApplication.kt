package com.gegaremant.truenasmobile

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.gegaremant.truenasmobile.data.helpers.TrueNasMobileLogger
import com.gegaremant.truenasmobile.data.workers.AlertsWorker

class TrueNasMobileApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        TrueNasMobileLogger.initialize(this)
        AlertsWorker.schedule(this)
    }
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(200 * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}