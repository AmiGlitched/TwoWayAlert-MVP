package com.myapplication.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myapplication.app.ui.theme.AccentRed
import com.myapplication.app.ui.theme.TextPrimary
import com.myapplication.app.ui.theme.TextSecondary

@Composable
fun GlassNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .fillMaxWidth()
            .height(72.dp)
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(36.dp),
            showSpecular = true
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    isSelected = currentRoute == "alert",
                    onClick = { onNavigate("alert") }
                )
                NavBarItem(
                    icon = Icons.Filled.Contacts,
                    label = "Contacts",
                    isSelected = currentRoute == "contacts",
                    onClick = { onNavigate("contacts") }
                )
                NavBarItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "History",
                    isSelected = currentRoute == "history",
                    onClick = { onNavigate("history") }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) AccentRed else TextSecondary
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentRed)
            )
        }
    }
}
