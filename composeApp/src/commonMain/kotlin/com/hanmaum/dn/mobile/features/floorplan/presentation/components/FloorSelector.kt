package com.hanmaum.dn.mobile.features.floorplan.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanmaum.dn.mobile.features.floorplan.domain.model.Floor

@Composable
fun FloorSelector(
    floors: List<Floor>,
    selectedFloor: Floor,
    onFloorSelected: (Floor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = floors.indexOfFirst { it.id == selectedFloor.id }.coerceAtLeast(0)
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.secondary,
    ) {
        floors.forEach { floor ->
            Tab(
                selected = floor.id == selectedFloor.id,
                onClick = { onFloorSelected(floor) },
                text = { Text(floor.name, style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}
