package com.spidey.js.angad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spidey.js.angad.ui.theme.*

@Composable
fun DivineStatCard(label: String, value: String, modifier: Modifier, accent: Color) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TempleSurface),
            shape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value, style = MaterialTheme.typography.headlineMedium, color = accent, fontWeight = FontWeight.Black)
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = AncientWhite.copy(0.5f))
            }
        }
        
        // Ornamental Border
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 1.dp.toPx()
            val ornamentSize = 12.dp.toPx()
            
            // Top-Left corner accent
            drawPath(
                path = Path().apply {
                    moveTo(0f, ornamentSize)
                    lineTo(0f, 0f)
                    lineTo(ornamentSize, 0f)
                },
                color = accent.copy(alpha = 0.4f),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun DivineCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TempleSurface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(topStart = 20.dp, bottomEnd = 20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content
            )
        }
        
        // Corner Ornaments
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 2.dp.toPx()
            val ornamentSize = 24.dp.toPx()
            
            // Top-Left L-shape
            drawPath(
                path = Path().apply {
                    moveTo(0f, ornamentSize)
                    lineTo(0f, 0f)
                    lineTo(ornamentSize, 0f)
                },
                color = RoyalGold,
                style = Stroke(width = strokeWidth)
            )
            
            // Bottom-Right L-shape
            drawPath(
                path = Path().apply {
                    moveTo(size.width - ornamentSize, size.height)
                    lineTo(size.width, size.height)
                    lineTo(size.width, size.height - ornamentSize)
                },
                color = RoyalGold,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun DivineBackground() {
    Box(modifier = Modifier.fillMaxSize().background(DeepEarth)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                    center = center,
                    radius = size.maxDimension / 1.1f
                )
            )
            
            val random = java.util.Random(42)
            repeat(40) {
                val x = random.nextFloat() * size.width
                val y = random.nextFloat() * size.height
                drawCircle(
                    color = GoldGlow.copy(alpha = 0.08f),
                    radius = 1.5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
