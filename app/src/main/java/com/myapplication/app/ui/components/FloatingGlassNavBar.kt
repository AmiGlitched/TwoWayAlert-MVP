package com.myapplication.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myapplication.app.ui.theme.AccentRed
import com.myapplication.app.ui.theme.TextSecondary

private data class NavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

private val navItems = listOf(
    NavItem("alert", Icons.Outlined.Home, "Home"),
    NavItem("alertFeatures", Icons.Outlined.WarningAmber, "Alerts"),
    NavItem("contacts", Icons.Outlined.Contacts, "Contacts"),
    NavItem("history", Icons.AutoMirrored.Outlined.FormatListBulleted, "History")
)

// iOS-liquid-glass-style floating pill: the selection indicator slides smoothly between tabs
// (a spring-animated highlight, not an instant swap), and each tab does a small tap-scale bounce.
@Composable
fun FloatingGlassNavBar(currentRoute: String?, modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    GlassPanel(
        shape = shape,
        modifier = modifier.shadow(20.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
    ) {
        BoxWithConstraints(modifier = Modifier.padding(10.dp)) {
            val tileWidth = maxWidth / navItems.size
            val indicatorOffset by animateDpAsState(
                targetValue = tileWidth * selectedIndex,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "navIndicatorOffset"
            )

            // sliding highlight — animates position instead of instantly jumping to the new tab.
            // Fixed height here (not fillMaxHeight): this Box sits inside a BoxWithConstraints whose
            // own height is otherwise unbounded (it's docked to the bottom of a full-screen container),
            // so fillMaxHeight would blow up to the whole screen instead of just the pill's row height.
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tileWidth)
                    .height(58.dp)
                    .padding(2.dp)
                    .background(AccentRed.copy(alpha = 0.22f), shape = RoundedCornerShape(22.dp))
            )

            Row {
                navItems.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "navTapScale"
                    )

                    Column(
                        modifier = Modifier
                            .width(tileWidth)
                            .scale(scale)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(interactionSource = interactionSource, indication = null) { onNavigate(item.route) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = if (selected) AccentRed else TextSecondary, modifier = Modifier.size(22.dp))
                        Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (selected) AccentRed else TextSecondary)
                    }
                }
            }
        }
    }
}