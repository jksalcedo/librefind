package com.jksalcedo.librefind.ui.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.domain.model.SovereigntyLevel
import com.jksalcedo.librefind.domain.model.SovereigntyScore
import com.jksalcedo.librefind.ui.theme.CapturedOrange
import com.jksalcedo.librefind.ui.theme.FossGreen
import com.jksalcedo.librefind.ui.theme.SovereignGold
import com.jksalcedo.librefind.ui.theme.TransitionBlue

/**
 * Circular sovereignty gauge showing FOSS percentage
 */
@Composable
fun SovereigntyGauge(
    score: SovereigntyScore,
    currentFilter: AppStatus?,
    onFilterClick: (AppStatus?) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp).clickable(
                interactionSource = null,
                indication = null,
                onClick = {
                    onClick()
                }
            )
        ) {
            CircularProgressIndicator(
                progress = { score.fossPercentage / 100f },
                modifier = Modifier.fillMaxSize(),
                color = getLevelColor(score.level),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${score.fossPercentage.toInt()}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = getLevelColor(score.level)
                )
                Text(
                    text = getLevelText(score.level),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .height(140.dp),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatItem(
                label = "FOSS",
                count = score.fossCount,
                color = FossGreen,
                isActive = currentFilter == AppStatus.FOSS,
                onClick = {
                    onFilterClick(
                        if (currentFilter == AppStatus.FOSS) null
                        else AppStatus.FOSS
                    )
                }
            )
            StatItem(
                label = "PROPRIETARY",
                count = score.proprietaryCount,
                color = CapturedOrange,
                isActive = currentFilter == AppStatus.PROP,
                onClick = {
                    onFilterClick(
                        if (currentFilter == AppStatus.PROP) null
                        else AppStatus.PROP
                    )
                }
            )
            StatItem(
                label = "Unknown",
                count = score.unknownCount,
                color = MaterialTheme.colorScheme.outline,
                isActive = currentFilter == AppStatus.UNKN,
                onClick = {
                    onFilterClick(
                        if (currentFilter == AppStatus.UNKN) null
                        else AppStatus.UNKN
                    )
                }
            )
            StatItem(
                label = "Ignored",
                count = score.ignoredCount,
                color = MaterialTheme.colorScheme.error,
                isActive = currentFilter == AppStatus.IGNORED,
                onClick = {
                    onFilterClick(
                        if (currentFilter == AppStatus.IGNORED) null
                        else AppStatus.IGNORED
                    )
                }
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    count: Int,
    color: Color,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun getLevelColor(level: SovereigntyLevel): Color {
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

@Preview(showBackground = true)
@Composable
fun PreviewSovereigntyGauge() {
    val mockScore = SovereigntyScore(
        totalApps = 100,
        fossCount = 45,
        proprietaryCount = 40,
        unknownCount = 10,
        ignoredCount = 5
    )

    MaterialTheme {
        SovereigntyGauge(
            score = mockScore,
            currentFilter = null,
            onFilterClick = {},
            onClick = {}
        )
    }
}