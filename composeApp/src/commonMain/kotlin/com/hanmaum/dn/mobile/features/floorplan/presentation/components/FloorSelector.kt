package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorSelector(
    floors: List<Floor>,
    selectedFloor: Floor,
    onFloorSelected: (Floor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        tonalElevation = 4.dp,
        modifier = modifier,
    ) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            floors.forEach { floor ->
                FilterChip(
                    selected = floor.id == selectedFloor.id,
                    onClick = { onFloorSelected(floor) },
                    label = { Text(floor.name, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}
