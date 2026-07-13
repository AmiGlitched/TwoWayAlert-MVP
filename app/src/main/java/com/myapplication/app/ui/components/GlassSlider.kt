package com.myapplication.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.myapplication.app.ui.theme.GlassFillLighter
import kotlin.math.roundToInt

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    tint: Color = GlassFillLighter
) {
    var width by remember { mutableStateOf(0f) }
    val thumbSize = 32.dp
    val density = LocalDensity.current
    
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val horizontalPaddingPx = with(density) { 8.dp.toPx() }
    val verticalPaddingPx = with(density) { 8.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .onSizeChanged { width = it.width.toFloat() }
    ) {
        // Track
        GlassPanel(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            tint = tint,
            showSpecular = true
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (startIcon != null) {
                    Icon(startIcon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (endIcon != null) {
                    Icon(endIcon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }

        // Thumb
        val maxOffset = (width - thumbSizePx - (horizontalPaddingPx * 2)).coerceAtLeast(0f)
        val offsetX = value * maxOffset

        Box(
            modifier = Modifier
                .offset { IntOffset((offsetX + horizontalPaddingPx).roundToInt(), verticalPaddingPx.roundToInt()) }
                .size(thumbSize)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .background(Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (maxOffset > 0) {
                            val newValue = (value + dragAmount.x / maxOffset).coerceIn(0f, 1f)
                            onValueChange(newValue)
                        }
                    }
                }
        )
    }
}
