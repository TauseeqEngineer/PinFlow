package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppCard
import com.example.ui.theme.PinterestRed

@Composable
fun SettingSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = PinterestRed,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingSectionGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}
