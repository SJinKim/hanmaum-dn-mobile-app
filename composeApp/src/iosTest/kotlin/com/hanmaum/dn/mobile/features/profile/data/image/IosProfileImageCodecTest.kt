@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hanmaum.dn.mobile.features.profile.data.image

import com.hanmaum.dn.mobile.features.profile.domain.PROFILE_IMAGE_EDGE
import com.hanmaum.dn.mobile.features.profile.domain.PROFILE_IMAGE_MAX_BYTES
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePreparation
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.test.runTest
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFURLRef
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGBitmapContextGetBytesPerRow
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextClearRect
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToFile
import platform.CoreFoundation.CFRetain
import platform.ImageIO.CGImageDestinationAddImage
import platform.ImageIO.CGImageDestinationCreateWithURL
import platform.ImageIO.CGImageDestinationFinalize
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithURL
import platform.ImageIO.kCGImagePropertyExifDictionary
import platform.ImageIO.kCGImagePropertyOrientation
import platform.ImageIO.kCGImagePropertyPixelHeight
import platform.ImageIO.kCGImagePropertyPixelWidth
import platform.ImageIO.kCGImagePropertyTIFFDictionary
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun key(ref: CFStringRef?): Any = CFBridgingRelease(CFRetain(ref))!!

/**
 * Real ImageIO on the simulator, no fakes: these fixtures are drawn here so
 * the expected pixels are known. A JVM fake would not prove the native crop.
 */
class IosProfileImageCodecTest {
    private val dir = NSTemporaryDirectory().trimEnd('/') + "/codec-test-" + NSUUID().UUIDString
    private val created = mutableListOf<String>()

    init {
        NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
    }

    @AfterTest
    fun cleanUp() {
        NSFileManager.defaultManager.removeItemAtPath(dir, null)
    }

    /** width × height, left half red and right half blue (as stored, before orientation). */
    private fun fixture(
        width: Int,
        height: Int,
        type: String,
        orientation: Int? = null,
        transparent: Boolean = false,
    ): String {
        val context = bitmap(width, height)
        if (transparent) {
            CGContextClearRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
        } else {
            CGContextSetRGBFillColor(context, 1.0, 0.0, 0.0, 1.0)
            CGContextFillRect(context, CGRectMake(0.0, 0.0, width / 2.0, height.toDouble()))
            CGContextSetRGBFillColor(context, 0.0, 0.0, 1.0, 1.0)
            CGContextFillRect(context, CGRectMake(width / 2.0, 0.0, width / 2.0, height.toDouble()))
        }
        val image = CGBitmapContextCreateImage(context)!!
        CGContextRelease(context)
        val path = "$dir/${NSUUID().UUIDString}"
        val url = CFBridgingRetain(NSURL.fileURLWithPath(path)) as CFURLRef?
        val uti = CFBridgingRetain(type) as CFStringRef?
        val destination = CGImageDestinationCreateWithURL(url, uti, 1u, null)!!
        val properties: Map<Any, Any> = buildMap {
            if (orientation != null) put(key(kCGImagePropertyOrientation), orientation)
            put(key(kCGImagePropertyExifDictionary), mapOf("UserComment" to "secret"))
        }
        val dict = CFBridgingRetain(properties) as platform.CoreFoundation.CFDictionaryRef?
        CGImageDestinationAddImage(destination, image, dict)
        assertTrue(CGImageDestinationFinalize(destination))
        CFRelease(destination); dict?.let { CFRelease(it) }; url?.let { CFRelease(it) }; uti?.let { CFRelease(it) }
        CGImageRelease(image)
        return path
    }

    private fun bitmap(width: Int, height: Int): CGContextRef = CGBitmapContextCreate(
        null, width.toULong(), height.toULong(), 8u, 0u, CGColorSpaceCreateDeviceRGB(),
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
    )!!

    private suspend fun prepare(path: String): String {
        val preparer = FileProfileImagePreparer(IosProfileImageCodec(), "$dir/out")
        val ready = assertIs<ProfileImagePreparation.Ready>(preparer.prepare(IosPickedProfileImage(path)))
        assertTrue(ready.image.byteLength in 1 until PROFILE_IMAGE_MAX_BYTES)
        return ready.image.preview as String
    }

    private fun load(path: String): Pair<CGImageRef, NSDictionary> {
        val url = CFBridgingRetain(NSURL.fileURLWithPath(path)) as CFURLRef?
        val source = CGImageSourceCreateWithURL(url, null)!!
        val image = CGImageSourceCreateImageAtIndex(source, 0u, null)!!
        val props = CFBridgingRelease(CGImageSourceCopyPropertiesAtIndex(source, 0u, null)) as NSDictionary
        CFRelease(source); url?.let { CFRelease(it) }
        return image to props
    }

    /** RGB at (x, y) with y counted from the top. */
    private fun pixel(image: CGImageRef, x: Int, y: Int): Triple<Int, Int, Int> {
        val w = CGImageGetWidth(image).toInt()
        val h = CGImageGetHeight(image).toInt()
        val context = bitmap(w, h)
        CGContextDrawImage(context, CGRectMake(0.0, 0.0, w.toDouble(), h.toDouble()), image)
        val data = CGBitmapContextGetData(context)!!.reinterpret<UByteVar>()
        val row = CGBitmapContextGetBytesPerRow(context).toInt()
        val i = y * row + x * 4
        val rgb = Triple(data[i].toInt(), data[i + 1].toInt(), data[i + 2].toInt())
        CGContextRelease(context)
        return rgb
    }

    private fun assertRed(rgb: Triple<Int, Int, Int>) = assertTrue(rgb.first > 200 && rgb.third < 60, "not red: $rgb")
    private fun assertBlue(rgb: Triple<Int, Int, Int>) = assertTrue(rgb.third > 200 && rgb.first < 60, "not blue: $rgb")

    @Test
    fun aLandscapeJpegBecomesA512SquareWithoutMetadata() = runTest {
        val out = prepare(fixture(1200, 800, "public.jpeg"))
        val (image, props) = load(out)

        assertEquals(PROFILE_IMAGE_EDGE, CGImageGetWidth(image).toInt())
        assertEquals(PROFILE_IMAGE_EDGE, CGImageGetHeight(image).toInt())
        // The centre crop keeps both halves: red on the left, blue on the right.
        assertRed(pixel(image, 40, 256))
        assertBlue(pixel(image, 470, 256))
        assertNull(props.objectForKey(key(kCGImagePropertyOrientation)))
        val exif = props.objectForKey(key(kCGImagePropertyExifDictionary)) as? NSDictionary
        assertNull(exif?.objectForKey("UserComment"), "nothing from the original's EXIF survives")
        CGImageRelease(image)
    }

    @Test
    fun orientationSixIsTurnedUpright() = runTest {
        // Stored landscape, displayed rotated 90° clockwise: red ends up on top.
        val (image, props) = load(prepare(fixture(1200, 800, "public.jpeg", orientation = 6)))

        assertRed(pixel(image, 256, 40))
        assertBlue(pixel(image, 256, 470))
        val tiff = props.objectForKey(key(kCGImagePropertyTIFFDictionary)) as? NSDictionary
        val orientation = (props.objectForKey(key(kCGImagePropertyOrientation)) as? NSNumber)?.intValue
            ?: (tiff?.objectForKey(key(kCGImagePropertyOrientation)) as? NSNumber)?.intValue
        assertTrue(orientation == null || orientation == 1, "orientation was $orientation")
        CGImageRelease(image)
    }

    @Test
    fun mirroredOrientationTwoIsFlipped() = runTest {
        val (image, _) = load(prepare(fixture(1200, 800, "public.jpeg", orientation = 2)))

        assertBlue(pixel(image, 40, 256))
        assertRed(pixel(image, 470, 256))
        CGImageRelease(image)
    }

    @Test
    fun orientationEightIsTurnedTheOtherWay() = runTest {
        val (image, _) = load(prepare(fixture(1200, 800, "public.jpeg", orientation = 8)))

        assertBlue(pixel(image, 256, 40))
        assertRed(pixel(image, 256, 470))
        CGImageRelease(image)
    }

    @Test
    fun transparentPngTurnsWhiteNotBlack() = runTest {
        val (image, _) = load(prepare(fixture(600, 600, "public.png", transparent = true)))

        val rgb = pixel(image, 256, 256)
        assertTrue(rgb.first > 240 && rgb.second > 240 && rgb.third > 240, "not white: $rgb")
        CGImageRelease(image)
    }

    @Test
    fun aLargeOriginalIsPreparedWithinTheLimit() = runTest {
        val (image, props) = load(prepare(fixture(6000, 4000, "public.jpeg")))

        assertEquals(PROFILE_IMAGE_EDGE, (props.objectForKey(key(kCGImagePropertyPixelWidth)) as NSNumber).intValue)
        assertEquals(PROFILE_IMAGE_EDGE, (props.objectForKey(key(kCGImagePropertyPixelHeight)) as NSNumber).intValue)
        CGImageRelease(image)
    }

    @Test
    fun aSmallImageIsScaledUpTo512() = runTest {
        val (image, _) = load(prepare(fixture(200, 300, "public.jpeg")))

        assertEquals(PROFILE_IMAGE_EDGE, CGImageGetWidth(image).toInt())
        CGImageRelease(image)
    }

    @Test
    fun aBrokenFileIsRejected() = runTest {
        val path = "$dir/broken.jpg"
        val bytes = ByteArray(64) { it.toByte() }
        bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
            .writeToFile(path, atomically = true)
        val preparer = FileProfileImagePreparer(IosProfileImageCodec(), "$dir/out")

        val result = assertIs<ProfileImagePreparation.Failed>(preparer.prepare(IosPickedProfileImage(path)))

        assertEquals(com.hanmaum.dn.mobile.features.member.domain.model.ProfileImageFailure.INVALID_IMAGE, result.failure)
    }

    @Test
    fun releasingThePickDeletesThePrivateCopy() {
        val path = fixture(100, 100, "public.jpeg")
        IosPickedProfileImage(path).release()
        assertTrue(!NSFileManager.defaultManager.fileExistsAtPath(path))
    }
}
