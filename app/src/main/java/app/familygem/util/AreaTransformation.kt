package app.familygem.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import org.folg.gedcom.model.Media
import java.security.MessageDigest

/**
 * Glide-трансформация для обрезки изображения по координатам _AREA.
 * Формат: "left top right bottom" (значения 0.0..1.0)
 */
class AreaTransformation(private val media: Media) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        if (outWidth < 2 || outHeight < 2) return toTransform
        val areaStr = media.area ?: return toTransform
        val coords = parseArea(areaStr) ?: return toTransform
        if (coords[0] >= coords[2] || coords[1] >= coords[3]) return toTransform

        val srcRect = RectF(
            coords[0] * toTransform.width,
            coords[1] * toTransform.height,
            coords[2] * toTransform.width,
            coords[3] * toTransform.height
        )

        // Calculate aspect ratio of the cropped area
        val srcWidth = srcRect.width()
        val srcHeight = srcRect.height()
        val dstWidth = outWidth.toFloat()
        val dstHeight = outHeight.toFloat()

        val srcRatio = srcWidth / srcHeight
        val dstRatio = dstWidth / dstHeight

        var finalSrcRect = srcRect
        // If the source ratio is wider than target, crop horizontally
        if (srcRatio > dstRatio) {
            val newWidth = srcHeight * dstRatio
            val dx = (srcWidth - newWidth) / 2
            finalSrcRect = RectF(
                srcRect.left + dx,
                srcRect.top,
                srcRect.right - dx,
                srcRect.bottom
            )
        }
        // If the source ratio is taller than target, crop vertically
        else if (srcRatio < dstRatio) {
            val newHeight = srcWidth / dstRatio
            val dy = (srcHeight - newHeight) / 2
            finalSrcRect = RectF(
                srcRect.left,
                srcRect.top + dy,
                srcRect.right,
                srcRect.bottom - dy
            )
        }

        val matrix = Matrix()
        matrix.setRectToRect(
            finalSrcRect,
            RectF(0f, 0f, dstWidth, dstHeight),
            Matrix.ScaleToFit.CENTER
        )

        val result = Bitmap.createBitmap(
            outWidth,
            outHeight,
            toTransform.config ?: Bitmap.Config.ARGB_8888  // ← обработка null
        )
        val canvas = Canvas(result)
        canvas.drawBitmap(toTransform, matrix, null)
        return result
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID.toByteArray(Charsets.UTF_8))
        media.area?.let { messageDigest.update(it.toByteArray(Charsets.UTF_8)) }
    }

    companion object {
        private const val ID = "app.familygem.util.AreaTransformation"

        private fun parseArea(areaStr: String): FloatArray? {
            return try {
                val parts = areaStr.trim().split(Regex("\\s+"))
                if (parts.size != 4) return null
                floatArrayOf(
                    parts[0].toFloat().coerceIn(0f, 1f),
                    parts[1].toFloat().coerceIn(0f, 1f),
                    parts[2].toFloat().coerceIn(0f, 1f),
                    parts[3].toFloat().coerceIn(0f, 1f)
                ).also {
                    if (it[0] >= it[2] || it[1] >= it[3]) null
                    else it
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}