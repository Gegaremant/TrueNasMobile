package com.imnotndesh.truehub.data.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object IconCache {
    private const val DIR_NAME = "quick_launch_icons"
    private const val ICON_PX = 192

    suspend fun cacheIcon(context: Context, appName: String, iconUrl: String?): String? {
        if (iconUrl.isNullOrBlank()) return null
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(iconUrl)
                .decoderFactory(SvgDecoder.Factory())
                .size(ICON_PX, ICON_PX)
                .build()

            val drawable = loader.execute(request).drawable ?: return null
            val bitmap = drawable.toBitmap(width = ICON_PX, height = ICON_PX)

            val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            val file = File(dir, "${appName.hashCode()}.png")
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        out
                    )
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            TrueHubLogger.e("IconCache", "Failed to cache icon for $appName", e)
            null
        }
    }

    fun loadCachedBitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    fun deleteCachedIcon(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}