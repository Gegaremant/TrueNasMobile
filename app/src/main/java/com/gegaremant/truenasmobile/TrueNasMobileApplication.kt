package com.gegaremant.truenasmobile

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.gegaremant.truenasmobile.data.helpers.TrueNasMobileLogger
import com.gegaremant.truenasmobile.data.workers.AlertsWorker
import com.gegaremant.truenasmobile.ui.components.ToastManager

class TrueNasMobileApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        TrueNasMobileLogger.initialize(this)
        ToastManager.init(this)
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
            // Most TrueNAS app icons are SVG, and a loader without a decoder for
            // them fails the request instead of falling back. Six call sites
            // were passing a bare url and quietly showing nothing; configuring it
            // once here covers those and every future AsyncImage.
            .components {
                add(SvgDecoder.Factory())
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}