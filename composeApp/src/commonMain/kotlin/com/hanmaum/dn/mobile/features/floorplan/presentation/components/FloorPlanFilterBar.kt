package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanFilterBar(
    floors: List<Floor>,
    selectedFloor: Floor,
    rooms: List<Room>,
    selectedRoom: Room?,
    onFloorSelected: (Floor) -> Unit,
    onRoomSelected: (Room) -> Unit,
    onRoomDeselected: () -> Unit,
    onShowList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            val roomScrollState = rememberScrollState()
            LaunchedEffect(selectedFloor.id) { roomScrollState.scrollTo(0) }

            // Row 1: scrollable floor chips + 목록 button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    floors.forEach { floor ->
                        FilterChip(
                            selected = floor.id == selectedFloor.id,
                            onClick = { onFloorSelected(floor) },
                            label = { Text(floor.name, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onShowList,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("목록", style = MaterialTheme.typography.labelSmall)
                }
            }
            // Row 2: scrollable room chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(roomScrollState)
                    .padding(bottom = 8.dp),
            ) {
                rooms.forEach { room ->
                    val selected = room.id == selectedRoom?.id
                    FilterChip(
                        selected = selected,
                        onClick = { if (selected) onRoomDeselected() else onRoomSelected(room) },
                        label = { Text(room.name) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
        }
    }
}
