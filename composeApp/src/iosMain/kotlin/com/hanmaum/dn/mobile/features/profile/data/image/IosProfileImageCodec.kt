@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hanmaum.dn.mobile.features.profile.data.image

import com.hanmaum.dn.mobile.features.profile.domain.PickedProfileImage
import kotlinx.cinterop.convert
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFURLRef
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetInterpolationQuality
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGInterpolationHigh
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.ImageIO.CGImageDestinationAddImage
import platform.ImageIO.CGImageDestinationCreateWithURL
import platform.ImageIO.CGImageDestinationFinalize
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithURL
import platform.ImageIO.CGImageSourceGetCount
import platform.ImageIO.kCGImageDestinationLossyCompressionQuality
import platform.ImageIO.kCGImagePropertyPixelHeight
import platform.ImageIO.kCGImagePropertyPixelWidth
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceShouldCacheImmediately
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize

internal class IosSquareImage(val image: CGImageRef) : SquareImage {
    override fun recycle() = CGImageRelease(image)
}

/** Bridges an ImageIO key constant to an NSString usable in a Kotlin map. */
private fun key(ref: CFStringRef?): Any = CFBridgingRelease(CFRetain(ref))!!

private fun <T> withCfDictionary(map: Map<Any, Any>, block: (CFDictionaryRef?) -> T): T {
    // A Kotlin Map crosses into Objective-C as an NSDictionary.
    val dict = CFBridgingRetain(map) as CFDictionaryRef?
    try {
        return block(dict)
    } finally {
        dict?.let { CFRelease(it) }
    }
}

/**
 * ImageIO's thumbnailer decodes straight to a bounded size and applies the
 * EXIF orientation (all eight, mirroring included) — a large original is
 * never decoded in full. The square is then drawn onto white and written
 * as a fresh JPEG, so no metadata of the original is carried over.
 */
internal class IosProfileImageCodec : ProfileImageCodec {

    override suspend fun decodeSquare(picked: PickedProfileImage, edge: Int): SquareImage? {
        val path = (picked as? IosPickedProfileImage)?.path ?: return null
        val thumbnail = boundedUprightImage(path, edge) ?: return null
        try {
            return IosSquareImage(drawCentreSquare(thumbnail, edge) ?: return null)
        } finally {
            CGImageRelease(thumbnail)
        }
    }

    override fun writeJpeg(image: SquareImage, quality: Int, path: String): Boolean {
        val cg = (image as IosSquareImage).image
        val url = CFBridgingRetain(NSURL.fileURLWithPath(path)) as CFURLRef?
        val type = CFBridgingRetain("public.jpeg") as CFStringRef?
        try {
            val destination = CGImageDestinationCreateWithURL(url, type, 1u, null) ?: return false
            try {
                withCfDictionary(mapOf(key(kCGImageDestinationLossyCompressionQuality) to quality / 100.0)) {
                    CGImageDestinationAddImage(destination, cg, it)
                }
                return CGImageDestinationFinalize(destination)
            } finally {
                CFRelease(destination)
            }
        } finally {
            url?.let { CFRelease(it) }
            type?.let { CFRelease(it) }
        }
    }
}

internal fun boundedUprightImage(path: String, edge: Int): CGImageRef? {
    val url = CFBridgingRetain(NSURL.fileURLWithPath(path)) as CFURLRef?
    try {
        val source = withCfDictionary(mapOf(key(kCGImageSourceShouldCacheImmediately) to false)) {
            CGImageSourceCreateWithURL(url, it)
        } ?: return null
        try {
            if (CGImageSourceGetCount(source) < 1u) return null
            // Read the size from the header before decoding anything.
            val properties = CFBridgingRelease(CGImageSourceCopyPropertiesAtIndex(source, 0u, null)) as? NSDictionary
                ?: return null
            val width = (properties.objectForKey(key(kCGImagePropertyPixelWidth)) as? NSNumber)?.intValue ?: return null
            val height = (properties.objectForKey(key(kCGImagePropertyPixelHeight)) as? NSNumber)?.intValue ?: return null
            if (width <= 0 || height <= 0) return null
            val options = mapOf(
                key(kCGImageSourceCreateThumbnailFromImageAlways) to true,
                key(kCGImageSourceCreateThumbnailWithTransform) to true,
                key(kCGImageSourceShouldCacheImmediately) to true,
                key(kCGImageSourceThumbnailMaxPixelSize) to boundedMaxPixelSize(width, height, edge),
            )
            return withCfDictionary(options) { CGImageSourceCreateThumbnailAtIndex(source, 0u, it) }
        } finally {
            CFRelease(source)
        }
    } finally {
        url?.let { CFRelease(it) }
    }
}

private fun drawCentreSquare(image: CGImageRef, edge: Int): CGImageRef? {
    val width = CGImageGetWidth(image).toInt()
    val height = CGImageGetHeight(image).toInt()
    if (width <= 0 || height <= 0) return null
    val crop = centreSquareCrop(width, height)
    val scale = edge.toDouble() / crop.size
    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val context = CGBitmapContextCreate(
        null, edge.convert(), edge.convert(), 8u, 0u, colorSpace,
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
    )
    CGColorSpaceRelease(colorSpace)
    context ?: return null
    try {
        CGContextSetInterpolationQuality(context, kCGInterpolationHigh)
        // White first, so a transparent PNG does not turn black in the JPEG.
        CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
        CGContextFillRect(context, CGRectMake(0.0, 0.0, edge.toDouble(), edge.toDouble()))
        // Core Graphics has its origin bottom-left; a centred crop is symmetric, so only the offset sign matters.
        CGContextDrawImage(
            context,
            CGRectMake(-crop.left * scale, -(height - crop.top - crop.size) * scale, width * scale, height * scale),
            image,
        )
        return CGBitmapContextCreateImage(context)
    } finally {
        CGContextRelease(context)
    }
}
