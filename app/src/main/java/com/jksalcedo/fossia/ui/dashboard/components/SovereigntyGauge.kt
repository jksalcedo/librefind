package com.jksalcedo.fossia.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jksalcedo.fossia.domain.model.SovereigntyLevel
import com.jksalcedo.fossia.domain.model.SovereigntyScore
import com.jksalcedo.fossia.ui.theme.CapturedOrange
import com.jksalcedo.fossia.ui.theme.FossGreen
import com.jksalcedo.fossia.ui.theme.SovereignGold
import com.jksalcedo.fossia.ui.theme.TransitionBlue

/**
 * Circular sovereignty gauge showing FOSS percentage
 */
@Composable
fun SovereigntyGauge(
    score: SovereigntyScore,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            // Background circle
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 12.dp
            )
            
            // Progress circle
            CircularProgressIndicator(
                progress = { score.fossPercentage / 100f },
                modifier = Modifier.fillMaxSize(),
                color = getLevelColor(score.level),
                strokeWidth = 12.dp
            )
            
            // Center text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                   text = "${score.fossPercentage.toInt()}%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = getLevelColor(score.level)
                )
                Text(
                    text = getLevelText(score.level),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "FOSS", count = score.fossCount, color = FossGreen)
            StatItem(label = "PROP", count = score.proprietaryCount, color = CapturedOrange)
            StatItem(label = "Unknown", count = score.unknownCount, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getLevelColor(level: SovereigntyLevel): androidx.compose.ui.graphics.Color {
    return when (level) {
        SovereigntyLevel.SOVEREIGN -> SovereignGold
        SovereigntyLevel.TRANSITIONING -> TransitionBlue
        SovereigntyLevel.CAPTURED -> CapturedOrange
    }
}

private fun getLevelText(level: SovereigntyLevel): String {
    return when (level) {
        SovereigntyLevel.SOVEREIGN -> "Sovereign"
        SovereigntyLevel.TRANSITIONING -> "Transitioning"
        SovereigntyLevel.CAPTURED -> "Captured"
    }
}
