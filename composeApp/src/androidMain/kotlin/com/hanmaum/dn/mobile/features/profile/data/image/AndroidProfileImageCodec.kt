package com.hanmaum.dn.mobile.features.profile.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.os.Build
import com.hanmaum.dn.mobile.features.profile.domain.PickedProfileImage
import java.io.File
import java.io.FileOutputStream

private class AndroidSquareImage(val bitmap: Bitmap) : SquareImage {
    override fun recycle() = bitmap.recycle()
}

/**
 * API 28+ decodes with [ImageDecoder], which applies the EXIF orientation
 * itself and decodes straight to a target size. API 24–27 samples down with
 * [BitmapFactory] and rotates/mirrors by [ExifInterface] by hand.
 */
internal class AndroidProfileImageCodec(private val context: Context) : ProfileImageCodec {

    override suspend fun decodeSquare(picked: PickedProfileImage, edge: Int): SquareImage? {
        val uri = (picked as? AndroidPickedProfileImage)?.uri ?: return null
        val upright = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = boundedDecodeSize(info.size.width, info.size.height, edge)
                decoder.setTargetSize(w, h)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodeLegacy(uri, edge) ?: return null
        }
        return AndroidSquareImage(centreSquare(upright, edge))
    }

    override fun writeJpeg(image: SquareImage, quality: Int, path: String): Boolean {
        val bitmap = (image as AndroidSquareImage).bitmap
        // A fresh Bitmap carries no EXIF, so nothing from the original survives.
        return FileOutputStream(File(path)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    }

    private fun decodeLegacy(uri: android.net.Uri, edge: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, edge)
        }
        val sampled = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = orientationMatrix(orientation) ?: return sampled
        return Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
            .also { if (it !== sampled) sampled.recycle() }
    }

    /** Draws the centre square scaled to [edge] onto white, so transparency does not turn black in JPEG. */
    private fun centreSquare(src: Bitmap, edge: Int): Bitmap {
        val crop = centreSquareCrop(src.width, src.height)
        val out = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(
                src,
                Rect(crop.left, crop.top, crop.left + crop.size, crop.top + crop.size),
                RectF(0f, 0f, edge.toFloat(), edge.toFloat()),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
            )
        }
        src.recycle()
        return out
    }
}

/** The matrix for an EXIF orientation, or null for "normal" / unknown. Covers all eight, mirrored ones included. */
private fun orientationMatrix(orientation: Int): Matrix? = when (orientation) {
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { setScale(-1f, 1f) }
    ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { setRotate(180f) }
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { setScale(1f, -1f) }
    ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { setRotate(90f); postScale(-1f, 1f) }
    ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { setRotate(90f) }
    ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply { setRotate(-90f); postScale(-1f, 1f) }
    ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { setRotate(-90f) }
    else -> null
}
