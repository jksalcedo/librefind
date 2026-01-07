package com.jksalcedo.fossia.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jksalcedo.fossia.domain.model.AppStatus
import com.jksalcedo.fossia.ui.theme.FossGreen
import com.jksalcedo.fossia.ui.theme.PropRed
import com.jksalcedo.fossia.ui.theme.UnknownGray
import androidx.compose.foundation.layout.padding

/**
 * Visual status badge component
 * 
 * Displays FOSS/PROP/UNKN status with color coding
 */
@Composable
fun StatusBadge(
    status: AppStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when(status) {
        AppStatus.FOSS -> FossGreen to "FOSS"
        AppStatus.PROP -> PropRed to "PROP"
        AppStatus.UNKN -> UnknownGray to "?"
    }
    
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
