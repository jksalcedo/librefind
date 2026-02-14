package com.jksalcedo.librefind.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import com.jksalcedo.librefind.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun TutorialOverlay(
    currentStep: TutorialStep,
    stepIndex: Int,
    totalSteps: Int,
    highlightRect: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "overlay_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .drawBehind {
                val overlayColor = Color.Black.copy(alpha = 0.7f * animatedAlpha)
                
                if (highlightRect != null && highlightRect.width > 0 && highlightRect.height > 0) {
                    val padding = 8.dp.toPx()
                    val expandedRect = Rect(
                        left = (highlightRect.left - padding).coerceAtLeast(0f),
                        top = (highlightRect.top - padding).coerceAtLeast(0f),
                        right = (highlightRect.right + padding).coerceAtMost(size.width),
                        bottom = (highlightRect.bottom + padding).coerceAtMost(size.height)
                    )
                    
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = expandedRect,
                                cornerRadius = CornerRadius(12.dp.toPx())
                            )
                        )
                    }
                    
                    clipPath(path, clipOp = ClipOp.Difference) {
                        drawRect(overlayColor)
                    }
                } else {
                    drawRect(overlayColor)
                }
            }
    ) {
        val tooltipOffsetY = if (highlightRect != null) {
            val bottomSpace = screenHeight - highlightRect.bottom
            if (bottomSpace > 200.dp.value * density.density) {
                highlightRect.bottom + 16.dp.value * density.density
            } else {
                (highlightRect.top - 180.dp.value * density.density).coerceAtLeast(100f)
            }
        } else {
            screenHeight / 2 - 100.dp.value * density.density
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, tooltipOffsetY.roundToInt()) }
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stepIndex + 1}/$totalSteps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.tutorial_skip))
                    }
                    Button(onClick = onNext) {
                        Text(if (stepIndex == totalSteps - 1) stringResource(R.string.tutorial_done) else stringResource(R.string.tutorial_next))
                    }
                }
            }
        }
    }
}
