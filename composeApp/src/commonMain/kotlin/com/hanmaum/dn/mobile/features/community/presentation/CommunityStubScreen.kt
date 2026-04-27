package com.hanmaum.dn.mobile.features.community.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hanmaum.dn.mobile.core.presentation.components.ChurchTopBar

// TODO: Replace with full Community feature implementation
@Composable
fun CommunityStubScreen() {
    Scaffold(
        topBar = { ChurchTopBar(title = "커뮤니티") }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("준비 중입니다")
        }
    }
}
