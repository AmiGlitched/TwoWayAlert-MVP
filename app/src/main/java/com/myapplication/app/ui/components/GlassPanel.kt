package com.myapplication.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.myapplication.app.ui.theme.GlassBorder
import com.myapplication.app.ui.theme.GlassFillLight

// The frosted-glass panel used everywhere instead of a flat Card. Layers a soft top-to-bottom
// translucency gradient (catches the "light passing through glass" look) over a hairline border.
// Added a specular highlight built into the border gradient to follow the shape correctly.
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = GlassFillLight,
    borderColor: Color = GlassBorder,
    showSpecular: Boolean = true,
    content: @Composable () -> Unit
) {
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(18.dp) else Modifier

    Box(modifier = modifier.clip(shape)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(blurModifier)
                .background(
                    Brush.verticalGradient(
                        0.0f to tint,
                        0.5f to tint.copy(alpha = tint.alpha * 0.8f),
                        1.0f to tint.copy(alpha = tint.alpha * 0.5f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = if (showSpecular) {
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.35f), // Specular highlight at the top
                            0.1f to borderColor,
                            1.0f to borderColor.copy(alpha = 0.1f)   // Fades out at the bottom
                        )
                    } else {
                        Brush.verticalGradient(colors = listOf(borderColor, borderColor))
                    },
                    shape = shape
                )
        )
        content()
    }
}
