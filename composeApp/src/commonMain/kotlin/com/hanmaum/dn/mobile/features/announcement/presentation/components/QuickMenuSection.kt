package com.hanmaum.dn.mobile.features.announcement.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun QuickMenuSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "바로가기 메뉴",
            // M3 Typography: subtitle1 -> titleMedium
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickMenuItem(Icons.Default.OndemandVideo, "설교영상")
            QuickMenuItem(Icons.Default.Article, "주보보기")
            QuickMenuItem(Icons.Default.Work, "청년사역")
            QuickMenuItem(Icons.Default.PhotoLibrary, "교회앨범")
        }
    }
}

@Composable
private fun QuickMenuItem(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            // M3 Typography: caption -> labelMedium (oder bodySmall)
            style = MaterialTheme.typography.labelMedium,
            color = Color.Black // Explizit Schwarz, sonst oft Grau in M3
        )
    }
}