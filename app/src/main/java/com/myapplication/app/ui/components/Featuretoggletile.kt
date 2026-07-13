package com.myapplication.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.myapplication.app.ui.theme.GlassBorder
import com.myapplication.app.ui.theme.GlassFillLight
import com.myapplication.app.ui.theme.TextPrimary
import com.myapplication.app.ui.theme.TextSecondary

// Whole-tile tap target for a feature toggle. Glows accent-red + tinted fill when on,
// sits as a plain glass tile when off — used instead of Switch rows so the enabled/disabled
// state is readable at a glance across a grid, and the tap target is bigger (helps Comfort Mode too).
@Composable
fun FeatureToggleTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    GlassPanel(
        modifier = modifier.clickable { onToggle(!enabled) },
        shape = shape,
        tint = if (enabled) AccentRed.copy(alpha = 0.16f) else GlassFillLight,
         borderColor = if (enabled) AccentRed.copy(alpha = 0.55f) else GlassBorder,
        showSpecular = true
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (enabled) AccentRed else TextSecondary, modifier = Modifier.size(22.dp))
                Box(modifier = Modifier.size(10.dp).background(if (enabled) AccentRed else Color(0xFF3A3A3E), shape = CircleShape))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
        }
    }
}
