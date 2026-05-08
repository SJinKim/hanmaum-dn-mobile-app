package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Point
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room
import com.hanmaum.dn.mobile.features.floorplan.domain.model.hitTest

@Composable
fun FloorCanvas(
    rooms: List<Room>,
    selectedRoom: Room?,
    onRoomTap: (Room) -> Unit,
    onEmptyTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow
    val surfaceContainerLowest = MaterialTheme.colorScheme.surfaceContainerLowest
    val onSurface = MaterialTheme.colorScheme.onSurface

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange
    }
    LaunchedEffect(rooms) {
        scale = 1f
        offset = Offset.Zero
    }

    Canvas(
        modifier = modifier
            .transformable(state = transformState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
                transformOrigin = TransformOrigin(0f, 0f),
            )
            .pointerInput(rooms, selectedRoom) {
                detectTapGestures { tapOffset ->
                    val normalizedTap = Point(
                        x = (tapOffset.x - offset.x) / (size.width * scale),
                        y = (tapOffset.y - offset.y) / (size.height * scale),
                    )
                    val hit = rooms.firstOrNull { it.points.hitTest(normalizedTap) }
                    if (hit != null) onRoomTap(hit) else onEmptyTap()
                }
            },
    ) {
        drawRect(surfaceContainerLow)
        rooms.forEach { room ->
            val scaled = room.points.map { Offset(it.x * size.width, it.y * size.height) }
            if (scaled.isEmpty()) return@forEach

            val path = Path().apply {
                moveTo(scaled.first().x, scaled.first().y)
                scaled.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }

            val fillColor = if (room.id == selectedRoom?.id) primaryContainer else surfaceContainerLowest
            drawPath(path, color = fillColor, style = Fill)
            drawPath(path, color = onSurface.copy(alpha = 0.15f), style = Stroke(width = 1.dp.toPx()))

            val cx = room.points.map { it.x }.average().toFloat() * size.width
            val cy = room.points.map { it.y }.average().toFloat() * size.height
            val measured = textMeasurer.measure(
                text = room.name,
                style = TextStyle(fontSize = 12.sp, color = onSurface),
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(cx - measured.size.width / 2f, cy - measured.size.height / 2f),
            )
        }
    }
}
