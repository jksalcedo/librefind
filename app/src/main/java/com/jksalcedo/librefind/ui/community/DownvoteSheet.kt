package com.jksalcedo.librefind.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jksalcedo.librefind.R
import com.jksalcedo.librefind.domain.model.DownvoteReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownvoteSheet(
    onDismiss: () -> Unit,
    onConfirm: (reason: String, reasonDetail: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReason by remember { mutableStateOf<DownvoteReason?>(null) }
    var duplicateDetail by remember { mutableStateOf("") }
    var otherDetail by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }

    val categories = listOf(
        stringResource(R.string.downvote_category_foss),
        stringResource(R.string.downvote_category_proprietary),
        stringResource(R.string.downvote_category_source_available),
        stringResource(R.string.downvote_category_pwa)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.downvote_sheet_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.downvote_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DownvoteReason.entries.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = {
                            selectedReason = if (selectedReason == reason) null else reason
                            duplicateDetail = ""
                            otherDetail = ""
                            selectedCategory = ""
                        },
                        label = { Text(reason.displayName()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            when (selectedReason) {
                DownvoteReason.DUPLICATE -> {
                    OutlinedTextField(
                        value = duplicateDetail,
                        onValueChange = { duplicateDetail = it },
                        label = { Text(stringResource(R.string.downvote_duplicate_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                DownvoteReason.WRONG_CATEGORY -> {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.downvote_category_hint)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                DownvoteReason.OTHER -> {
                    OutlinedTextField(
                        value = otherDetail,
                        onValueChange = { otherDetail = it },
                        label = { Text(stringResource(R.string.downvote_other_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val reason = selectedReason?.key ?: return@Button
                        val detail: String? = when (selectedReason) {
                            DownvoteReason.DUPLICATE -> duplicateDetail.ifBlank { null }
                            DownvoteReason.WRONG_CATEGORY -> selectedCategory.ifBlank { null }
                            DownvoteReason.OTHER -> otherDetail.ifBlank { null }
                            else -> null
                        }
                        onConfirm(reason, detail)
                    },
                    enabled = selectedReason != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.downvote_confirm))
                }
            }
        }
    }
}

@Composable
private fun DownvoteReason.displayName(): String = when (this) {
    DownvoteReason.DUPLICATE -> stringResource(R.string.downvote_reason_duplicate)
    DownvoteReason.WRONG_CATEGORY -> stringResource(R.string.downvote_reason_wrong_category)
    DownvoteReason.FAKE -> stringResource(R.string.downvote_reason_fake)
    DownvoteReason.NOT_ENOUGH_INFO -> stringResource(R.string.downvote_reason_not_enough_info)
    DownvoteReason.OTHER -> stringResource(R.string.downvote_reason_other)
}
