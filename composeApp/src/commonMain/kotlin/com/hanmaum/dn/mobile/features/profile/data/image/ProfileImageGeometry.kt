package com.hanmaum.dn.mobile.features.profile.data.image

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Longest edge any decode may produce, so a panorama never becomes a huge bitmap. */
internal const val MAX_DECODE_EDGE = 4096

internal data class SquareCrop(val left: Int, val top: Int, val size: Int)

/** The centred square of a width × height image. */
internal fun centreSquareCrop(width: Int, height: Int): SquareCrop {
    val size = min(width, height)
    return SquareCrop(left = (width - size) / 2, top = (height - size) / 2, size = size)
}

/**
 * The size to decode at so the short edge just covers [edge], keeping the
 * aspect ratio and capping the long edge at [MAX_DECODE_EDGE]. A source that
 * is already smaller is decoded as it is, never enlarged here.
 */
internal fun boundedDecodeSize(width: Int, height: Int, edge: Int): Pair<Int, Int> {
    require(width > 0 && height > 0)
    val short = min(width, height)
    val long = max(width, height)
    if (short <= edge && long <= MAX_DECODE_EDGE) return width to height
    val scale = min(edge.toDouble() / short, MAX_DECODE_EDGE.toDouble() / long).coerceAtMost(1.0)
    return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
}

/** The longest edge to ask ImageIO's thumbnailer for; same rule as [boundedDecodeSize]. */
internal fun boundedMaxPixelSize(width: Int, height: Int, edge: Int): Int {
    val (w, h) = boundedDecodeSize(width, height, edge)
    return max(w, h)
}

/** The largest power-of-two sample size whose result still covers [edge] on the short side. */
internal fun sampleSizeFor(width: Int, height: Int, edge: Int): Int {
    var sample = 1
    while (min(width, height) / (sample * 2) >= edge || max(width, height) / sample > MAX_DECODE_EDGE * 2) {
        if (min(width, height) / (sample * 2) < 1) break
        sample *= 2
    }
    return sample
}
