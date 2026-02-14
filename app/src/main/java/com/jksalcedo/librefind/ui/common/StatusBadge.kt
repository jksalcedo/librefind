package com.jksalcedo.librefind.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.ui.theme.FossGreen
import com.jksalcedo.librefind.ui.theme.IgnoredGray
import com.jksalcedo.librefind.ui.theme.PendingOrange
import com.jksalcedo.librefind.ui.theme.PropRed
import com.jksalcedo.librefind.ui.theme.UnknownGray
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.res.stringResource
import com.jksalcedo.librefind.R

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
    val isIgnored = status == AppStatus.IGNORED

    val color = when (status) {
        AppStatus.FOSS -> FossGreen
        AppStatus.PROP -> PropRed
        AppStatus.UNKN -> UnknownGray
        AppStatus.PENDING -> PendingOrange
        else -> IgnoredGray
    }

    val text = when (status) {
        AppStatus.FOSS -> stringResource(R.string.status_foss)
        AppStatus.PROP -> stringResource(R.string.status_proprietary)
        AppStatus.UNKN -> stringResource(R.string.status_unknown)
        AppStatus.PENDING -> stringResource(R.string.status_pending)
        else -> stringResource(R.string.status_ignored)
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
            fontWeight = if (isIgnored) FontWeight.Normal else FontWeight.Bold,
            fontStyle = if (isIgnored) FontStyle.Italic else FontStyle.Normal
        )
    }
}

@Composable
@Preview
fun StatusBadgePreview() {
    StatusBadge(status = AppStatus.FOSS)
}
