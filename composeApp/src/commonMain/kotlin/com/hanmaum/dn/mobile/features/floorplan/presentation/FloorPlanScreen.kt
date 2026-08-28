package com.hanmaum.dn.mobile.features.floorplan.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnChip
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.glass.GlassLevel
import com.hanmaum.dn.mobile.core.presentation.glass.dnGlass
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import androidx.compose.ui.text.drawText
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Point
import com.hanmaum.dn.mobile.features.floorplan.domain.model.hitTest
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room
import org.koin.compose.viewmodel.koinViewModel

/**
 * 교회 지도.
 *
 * The plan is drawn from normalised room polygons, so it scales to any
 * screen. The peek card floats over the map — it belongs to the navigation
 * layer, which is why it is the one surface here that gets the glass.
 */
@Composable
fun FloorPlanScreen(onBackClick: () -> Unit) {
    val viewModel: FloorPlanViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val c = DnTheme.colors

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(
                title = strings.floorPlanTitle,
                onBack = onBackClick,
                actionIcon = DnIcons.ListBulleted,
                actionDescription = strings.list,
                onAction = { },
            )

            when (val state = uiState) {
                is FloorPlanUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = c.lime)
                }

                is FloorPlanUiState.Error ->
                    DnErrorState(onRetry = viewModel::loadFloors)

                is FloorPlanUiState.Success -> {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.floors) { floor ->
                            DnChip(
                                label = floor.name,
                                selected = floor.id == state.selectedFloor.id,
                                onClick = { viewModel.selectFloor(floor) },
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        FloorCanvas(
                            rooms = state.rooms,
                            selectedRoom = state.selectedRoom,
                            onRoomTap = viewModel::selectRoom,
                            onEmptyTap = viewModel::clearSelectedRoom,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 24.dp)
                                .clip(DnCardShape)
                                .background(c.surface, DnCardShape)
                                .border(1.dp, c.strokeSubtle, DnCardShape),
                        )

                        state.selectedRoom?.let { room ->
                            RoomPeekCard(
                                room = room,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloorCanvas(
    rooms: List<Room>,
    selectedRoom: Room?,
    onRoomTap: (Room) -> Unit,
    onEmptyTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = DnTheme.colors
    val measurer = rememberTextMeasurer()

    Canvas(
        modifier.pointerInput(rooms, selectedRoom) {
            detectTapGestures { tap ->
                val p = Point(x = tap.x / size.width, y = tap.y / size.height)
                val hit = rooms.firstOrNull { it.points.hitTest(p) }
                if (hit != null) onRoomTap(hit) else onEmptyTap()
            }
        }
    ) {
        rooms.forEach { room ->
            val pts = room.points.map { Offset(it.x * size.width, it.y * size.height) }
            if (pts.isEmpty()) return@forEach

            val path = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            val selected = room.id == selectedRoom?.id
            drawPath(path, color = if (selected) c.limeDim else c.surface2)
            drawPath(
                path,
                color = if (selected) c.lime else c.strokeSubtle,
                style = Stroke(width = (if (selected) 1.5f else 1f) * density),
            )

            val cx = room.points.map { it.x }.average().toFloat() * size.width
            val cy = room.points.map { it.y }.average().toFloat() * size.height
            val measured = measurer.measure(
                text = room.name,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = if (selected) c.limeInk else c.textSecondary,
                ),
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(cx - measured.size.width / 2f, cy - measured.size.height / 2f),
            )
        }
    }
}

@Composable
private fun RoomPeekCard(room: Room, modifier: Modifier = Modifier) {
    val c = DnTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .dnGlass(shape = RoundedCornerShape(30.dp), level = GlassLevel.Strong)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(c.limeDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DnIcons.MapPin, null, tint = c.limeInk, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(room.name, style = DnTheme.typography.captionStrong, color = c.textPrimary)
            if (room.description.isNotBlank()) {
                Text(room.description, style = DnTheme.typography.caption, color = c.textSecondary)
            }
        }
    }
}
