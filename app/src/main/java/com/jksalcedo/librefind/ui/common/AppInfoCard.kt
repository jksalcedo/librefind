package com.jksalcedo.librefind.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jksalcedo.librefind.R

@Composable
fun AppInfoCard(
    appName: String,
    packageName: String,
    category: String?,
    license: String?,
    description: String?,
    modifier: Modifier = Modifier
) {
    val hasMetadata =
        !category.isNullOrBlank() || !license.isNullOrBlank() || !description.isNullOrBlank()
    val unknown = stringResource(R.string.app_info_unknown)

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.app_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            InfoRow(label = stringResource(R.string.app_info_label_name), value = appName)
            InfoRow(label = stringResource(R.string.app_info_label_package), value = packageName)
            InfoRow(
                label = stringResource(R.string.app_info_label_category),
                value = category?.takeIf { it.isNotBlank() } ?: unknown
            )
            InfoRow(
                label = stringResource(R.string.app_info_label_license),
                value = license?.takeIf { it.isNotBlank() } ?: unknown
            )

            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_info_label_description),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                InfoRow(
                    label = stringResource(R.string.app_info_label_description),
                    value = unknown
                )
            }

            if (!hasMetadata) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_info_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
