package com.hanmaum.dn.mobile.features.floorplan.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.AppScreen
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.FloorCanvas
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.FloorPlanFilterBar
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.RoomBottomSheet
import com.hanmaum.dn.mobile.features.floorplan.presentation.components.RoomPeekCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanScreen(
    onBackClick: () -> Unit,
) {
    val viewModel: FloorPlanViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    var showList by remember { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (uiState !is FloorPlanUiState.Success) showList = false
    }

    AppScreen(
        title = strings.floorPlanTitle,
        onBack = onBackClick,
        actions = {
            if (showList) {
                TextButton(onClick = { showList = false }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(strings.list, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is FloorPlanUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FloorPlanUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is FloorPlanUiState.Success -> {
                    var showFullSheet by remember { mutableStateOf(false) }
                    LaunchedEffect(state.selectedRoom) {
                        if (state.selectedRoom == null) showFullSheet = false
                    }
                    LaunchedEffect(state.selectedFloor) {
                        showList = false
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (!showList) {
                            FloorPlanFilterBar(
                                floors = state.floors,
                                selectedFloor = state.selectedFloor,
                                rooms = state.rooms,
                                selectedRoom = state.selectedRoom,
                                onFloorSelected = viewModel::selectFloor,
                                onRoomSelected = viewModel::selectRoom,
                                onRoomDeselected = viewModel::clearSelectedRoom,
                                onShowList = { showList = true },
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (showList) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                ) {
                                    items(state.rooms) { room ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    text = room.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            },
                                            supportingContent = if (room.description.isNotBlank()) {
                                                {
                                                    Text(
                                                        room.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
                                            } else null,
                                            modifier = Modifier.clickable {
                                                viewModel.selectRoom(room)
                                                showList = false
                                            },
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            } else {
                                FloorCanvas(
                                    rooms = state.rooms,
                                    selectedRoom = state.selectedRoom,
                                    onRoomTap = viewModel::selectRoom,
                                    onEmptyTap = viewModel::clearSelectedRoom,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = state.selectedRoom != null && !showList,
                                enter = slideInVertically { it },
                                exit = slideOutVertically { it },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            ) {
                                state.selectedRoom?.let { room ->
                                    RoomPeekCard(
                                        room = room,
                                        onDismiss = viewModel::clearSelectedRoom,
                                        onExpand = { showFullSheet = true },
                                    )
                                }
                            }
                        }
                    }

                    if (showFullSheet && state.selectedRoom != null) {
                        RoomBottomSheet(
                            room = state.selectedRoom!!,
                            onDismiss = { showFullSheet = false },
                        )
                    }
                }
            }
        }
    }
}
