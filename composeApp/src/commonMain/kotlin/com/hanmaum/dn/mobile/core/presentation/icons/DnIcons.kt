package com.hanmaum.dn.mobile.core.presentation.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The v2 icon set: 24 dp outlines at 1.7 px, round caps and joins.
 *
 * Outlines rather than filled glyphs — the active state in this design fills
 * the shape *behind* an icon, never the icon itself, so a filled set would
 * fight the navigation bar.
 *
 * The path data is identical to the Figma components on page
 * "03 · Components" (prefix `ui/`), parsed here so the two stay comparable.
 */
private fun stroked(name: String, vararg d: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        d.forEach {
            addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private fun filled(name: String, vararg d: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        d.forEach {
            addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }
    }.build()

private fun mixed(name: String, strokes: List<String>, fills: List<String>): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        strokes.forEach {
            addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        fills.forEach {
            addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }
    }.build()

object DnIcons {
    val Home: ImageVector by lazy {
        stroked(
            "Home",
            "M3.2 10.6 12 3.4l8.8 7.2",
        "M5.6 9.6V19.6a1 1 0 0 0 1 1H9.8v-5.6h4.4v5.6h3.2a1 1 0 0 0 1-1V9.6",
        )
    }

    val Bell: ImageVector by lazy {
        stroked(
            "Bell",
            "M18 9.4a6 6 0 1 0-12 0c0 6.4-2.6 7.6-2.6 7.6h17.2S18 15.8 18 9.4Z",
        "M13.9 20.4a2.2 2.2 0 0 1-3.8 0",
        )
    }

    val News: ImageVector by lazy {
        stroked(
            "News",
            "M4 5.4h10.6a1 1 0 0 1 1 1v12.2H5a1 1 0 0 1-1-1V5.4Z",
        "M15.6 9h3.4a1 1 0 0 1 1 1v7.6a1 1 0 0 1-1 1h-3.4",
        "M6.8 8.6h5.6M6.8 12h5.6M6.8 15.4h3.4",
        )
    }

    val Calendar: ImageVector by lazy {
        stroked(
            "Calendar",
            "M8 3.2v3.2M16 3.2v3.2",
        "M6.2 5.6h11.6a2.6 2.6 0 0 1 2.6 2.6v10a2.6 2.6 0 0 1-2.6 2.6H6.2a2.6 2.6 0 0 1-2.6-2.6v-10a2.6 2.6 0 0 1 2.6-2.6Z",
        "M3.6 10.4h16.8",
        )
    }

    val Image: ImageVector by lazy {
        stroked(
            "Image",
            "M6 5.2h12a2.8 2.8 0 0 1 2.8 2.8v8a2.8 2.8 0 0 1-2.8 2.8H6a2.8 2.8 0 0 1-2.8-2.8V8A2.8 2.8 0 0 1 6 5.2Z",
        "M8.6 8.5a1.5 1.5 0 1 1 0 3 1.5 1.5 0 0 1 0-3Z",
        "m4.4 17.4 4.5-4.6a1.6 1.6 0 0 1 2.3 0l3.1 3.2 1.7-1.6a1.6 1.6 0 0 1 2.2 0l2 1.9",
        )
    }

    val User: ImageVector by lazy {
        stroked(
            "User",
            "M12 4.4a3.8 3.8 0 1 1 0 7.6 3.8 3.8 0 0 1 0-7.6Z",
        "M4.8 20.2a7.2 7.2 0 0 1 14.4 0",
        )
    }

    val ChevronRight: ImageVector by lazy {
        stroked(
            "ChevronRight",
            "m9.2 5.6 6.4 6.4-6.4 6.4",
        )
    }

    val ChevronUp: ImageVector by lazy {
        stroked(
            "ChevronUp",
            "m5.6 15.2 6.4-6.4 6.4 6.4",
        )
    }

    val ArrowLeft: ImageVector by lazy {
        stroked(
            "ArrowLeft",
            "M19.4 12H4.6M10.4 5.6 4 12l6.4 6.4",
        )
    }

    val ArrowUpRight: ImageVector by lazy {
        stroked(
            "ArrowUpRight",
            "M7.4 16.6 16.6 7.4M8.6 7.4h8v8",
        )
    }

    val MapPin: ImageVector by lazy {
        stroked(
            "MapPin",
            "M12 21.2s7-6.4 7-11.4a7 7 0 1 0-14 0c0 5 7 11.4 7 11.4Z",
        "M12 7a2.6 2.6 0 1 1 0 5.2A2.6 2.6 0 0 1 12 7Z",
        )
    }

    val Users: ImageVector by lazy {
        stroked(
            "Users",
            "M9.4 5a3.4 3.4 0 1 1 0 6.8 3.4 3.4 0 0 1 0-6.8Z",
        "M3.4 19.8a6 6 0 0 1 12 0",
        "M16.4 5.4a3.4 3.4 0 0 1 0 6.6M17.6 14.4a5.6 5.6 0 0 1 3 5.4",
        )
    }

    val UserCheck: ImageVector by lazy {
        stroked(
            "UserCheck",
            "M9.6 4.4a3.6 3.6 0 1 1 0 7.2 3.6 3.6 0 0 1 0-7.2Z",
        "M3.4 19.6a6.2 6.2 0 0 1 10.4-4.5",
        "m15.4 17.8 2.2 2.2 4-4.4",
        )
    }

    val Check: ImageVector by lazy {
        stroked(
            "Check",
            "m5 12.8 4.6 4.6L19 7.4",
        )
    }

    val Trash: ImageVector by lazy {
        stroked(
            "Trash",
            "M 4.5 6.5 L 19.5 6.5",
        "M 9.5 6.5 L 9.5 4.6 C 9.5 4 10 3.5 10.6 3.5 L 13.4 3.5 C 14 3.5 14.5 4 14.5 4.6 L 14.5 6.5",
        "M 6.7 6.5 L 7.5 18.6 C 7.55 19.4 8.2 20.1 9.1 20.1 L 14.9 20.1 C 15.8 20.1 16.45 19.4 16.5 18.6 L 17.3 6.5",
        "M 10.2 10.4 L 10.2 16.3",
        "M 13.8 10.4 L 13.8 16.3",
        )
    }

    val Book: ImageVector by lazy {
        stroked(
            "Book",
            "M4 5.2A1.6 1.6 0 0 1 5.6 3.6h5.2a2.4 2.4 0 0 1 1.2.4 2.4 2.4 0 0 1 1.2-.4h5.2A1.6 1.6 0 0 1 20 5.2v12.4a1.6 1.6 0 0 1-1.6 1.6h-5a2.6 2.6 0 0 0-1.4.5 2.6 2.6 0 0 0-1.4-.5h-5A1.6 1.6 0 0 1 4 17.6Z",
        "M12 4v15.6",
        )
    }

    val Clock: ImageVector by lazy {
        stroked(
            "Clock",
            "M12 3.4a8.6 8.6 0 1 1 0 17.2 8.6 8.6 0 0 1 0-17.2Z",
        "M12 7.2V12l3.2 1.9",
        )
    }

    val ChevronsRight: ImageVector by lazy {
        stroked(
            "ChevronsRight",
            "m6 7.4 4.6 4.6L6 16.6M13 7.4l4.6 4.6L13 16.6",
        )
    }

    val LogOut: ImageVector by lazy {
        stroked(
            "LogOut",
            "M14.4 20.4H6.2a1.8 1.8 0 0 1-1.8-1.8V5.4a1.8 1.8 0 0 1 1.8-1.8h8.2",
        "M15.6 16.2 19.8 12l-4.2-4.2M19.4 12H9.6",
        )
    }

    val Sparkle: ImageVector by lazy {
        stroked(
            "Sparkle",
            "M12 3.4 13.9 9l5.6 1.9-5.6 1.9L12 18.4l-1.9-5.6L4.5 10.9 10.1 9Z",
        )
    }

    val Mail: ImageVector by lazy {
        stroked(
            "Mail",
            "M5.8 5.4h12.4a2.6 2.6 0 0 1 2.6 2.6v8a2.6 2.6 0 0 1-2.6 2.6H5.8a2.6 2.6 0 0 1-2.6-2.6V8a2.6 2.6 0 0 1 2.6-2.6Z",
        "m4.2 7.6 6.9 4.9a1.6 1.6 0 0 0 1.8 0l6.9-4.9",
        )
    }

    val Lock: ImageVector by lazy {
        stroked(
            "Lock",
            "M7.2 10.4h9.6a2.8 2.8 0 0 1 2.8 2.8v4.6a2.8 2.8 0 0 1-2.8 2.8H7.2a2.8 2.8 0 0 1-2.8-2.8v-4.6a2.8 2.8 0 0 1 2.8-2.8Z",
        "M7.8 10.4V7.8a4.2 4.2 0 0 1 8.4 0v2.6",
        "M12 14.4v2.4",
        )
    }

    val Eye: ImageVector by lazy {
        stroked(
            "Eye",
            "M2.6 12s3.6-6 9.4-6 9.4 6 9.4 6-3.6 6-9.4 6-9.4-6-9.4-6Z",
        "M12 9.1a2.9 2.9 0 1 1 0 5.8 2.9 2.9 0 0 1 0-5.8Z",
        )
    }

    /** Warning triangle — the load-failure state. */
    val AlertTriangle: ImageVector by lazy {
        stroked(
            "AlertTriangle",
            "M12 4.1 21 19.4H3L12 4.1Z",
        "M12 10.2v3.6",
        "M12 16.9v.05",
        )
    }

    /** Eye with a slash — the "password is hidden" half of the reveal toggle. */
    val EyeOff: ImageVector by lazy {
        stroked(
            "EyeOff",
            "M4 4.5 20 19.5",
        "M9.9 9.9a2.9 2.9 0 0 0 4.1 4.1",
        "M6.6 6.7C4.2 8.3 2.6 12 2.6 12s3.6 6 9.4 6c1.5 0 2.8-.4 4-1",
        "M17.7 16A15 15 0 0 0 21.4 12S17.8 6 12 6c-.8 0-1.6.1-2.3.3",
        )
    }

    val Hourglass: ImageVector by lazy {
        stroked(
            "Hourglass",
            "M7 3.6h10M7 20.4h10",
        "M8 3.6v3.2c0 2 4 3.6 4 5.2 0 1.6-4 3.2-4 5.2v3.2",
        "M16 3.6v3.2c0 2-4 3.6-4 5.2 0 1.6 4 3.2 4 5.2v3.2",
        )
    }

    val More: ImageVector by lazy {
        filled(
            "More",
            "M12 3.9a1.5 1.5 0 1 1 0 3 1.5 1.5 0 0 1 0-3Z",
        "M12 10.5a1.5 1.5 0 1 1 0 3 1.5 1.5 0 0 1 0-3Z",
        "M12 17.1a1.5 1.5 0 1 1 0 3 1.5 1.5 0 0 1 0-3Z",
        )
    }

    val ListBulleted: ImageVector by lazy {
        mixed(
            "ListBulleted",
            listOf("M9 6.4h11M9 12h11M9 17.6h11"),
            listOf("M4.6 5.1a1.3 1.3 0 1 1 0 2.6 1.3 1.3 0 0 1 0-2.6Z", "M4.6 10.7a1.3 1.3 0 1 1 0 2.6 1.3 1.3 0 0 1 0-2.6Z", "M4.6 16.3a1.3 1.3 0 1 1 0 2.6 1.3 1.3 0 0 1 0-2.6Z"),
        )
    }

}
