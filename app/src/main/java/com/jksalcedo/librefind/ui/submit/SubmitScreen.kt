package com.jksalcedo.librefind.ui.submit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.model.SubmissionType
import com.jksalcedo.librefind.ui.common.FieldWithHelp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import com.jksalcedo.librefind.R

@Composable
fun SubmitScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    onNavigateToTargetSubmission: (appName: String, packageName: String) -> Unit = { _, _ -> },
    viewModel: SubmitViewModel = koinViewModel(),
    prefilledAppName: String? = null,
    prefilledPackageName: String? = null,
    prefilledType: String? = null,
    submissionId: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(submissionId) {
        if (submissionId != null) {
            viewModel.loadSubmission(submissionId)
        }
    }

    LaunchedEffect(prefilledAppName, prefilledPackageName) {
        if (!prefilledPackageName.isNullOrBlank()) {
            viewModel.checkDuplicate(prefilledPackageName)
        }
    }

    SubmitContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onSuccess = onSuccess,
        onNavigateToTargetSubmission = onNavigateToTargetSubmission,
        prefilledAppName = prefilledAppName,
        prefilledPackageName = prefilledPackageName,
        prefilledType = prefilledType,
        onCheckDuplicate = viewModel::checkDuplicate,
        onValidatePackageName = viewModel::validatePackageName,
        onSearchSolutions = viewModel::searchSolutions,
        onClearSolutionSearchResults = viewModel::clearSolutionSearchResults,
        onAddAlternative = viewModel::addAlternative,
        onAddTarget = viewModel::setLinkTarget,
        onRemoveAlternative = viewModel::removeAlternative,
        onSearchFossApps = viewModel::searchFossApps,
        onSelectFossApp = viewModel::selectFossApp,
        onClearLinkedApp = viewModel::clearLinkedApp,
        onValidateRepoUrl = viewModel::validateRepoUrl,
        onSubmit = viewModel::submit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitContent(
    uiState: SubmitUiState,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    onNavigateToTargetSubmission: (appName: String, packageName: String) -> Unit,
    prefilledAppName: String?,
    prefilledPackageName: String?,
    prefilledType: String?,
    onCheckDuplicate: (String) -> Unit,
    onValidatePackageName: (String) -> Unit,
    onSearchSolutions: (String) -> Unit,
    onClearSolutionSearchResults: () -> Unit,
    onAddAlternative: (String) -> Unit,
    onAddTarget: (String) -> Unit,
    onRemoveAlternative: (String) -> Unit,
    onSearchFossApps: (String) -> Unit,
    onSelectFossApp: (Alternative) -> Unit,
    onClearLinkedApp: () -> Unit,
    onValidateRepoUrl: (String) -> Unit,
    onSubmit: (SubmissionType, String, String, String, String, String, String, String) -> Unit
) {
    val isPrefilled = prefilledAppName != null && prefilledPackageName != null
    val defaultType = when {
        prefilledType == "foss" -> SubmissionType.NEW_ALTERNATIVE
        prefilledType == "proprietary" -> SubmissionType.NEW_PROPRIETARY
        isPrefilled -> SubmissionType.NEW_PROPRIETARY
        else -> SubmissionType.NEW_ALTERNATIVE
    }
    var type by remember { mutableStateOf(defaultType) }
    var appName by remember { mutableStateOf(prefilledAppName ?: "") }
    var packageName by remember { mutableStateOf(prefilledPackageName ?: "") }
    var description by remember { mutableStateOf("") }
    var repoUrl by remember { mutableStateOf("") }
    var fdroidId by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }
    var selectedProprietaryPackages by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(uiState.loadedSubmission) {
        uiState.loadedSubmission?.let { sub ->
            type = sub.type
            appName = sub.submittedApp.name
            packageName = sub.submittedApp.packageName
            description = sub.submittedApp.description

            if (sub.type == SubmissionType.NEW_ALTERNATIVE) {
                // proprietaryPackages is a comma separated string in the Submission model
                selectedProprietaryPackages =
                    sub.proprietaryPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        .toSet()
            }

            repoUrl = sub.submittedApp.repoUrl
            fdroidId = sub.submittedApp.fdroidId
            license = sub.submittedApp.license
        }
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success && uiState.submittedAppName == null) {
            onSuccess()
        }
    }

    if (uiState.submittedAppName != null) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text(if (uiState.isEditing) stringResource(R.string.submit_title_edit) else stringResource(R.string.submit_success_title)) },
            text = {
                Text(if (uiState.isEditing) stringResource(R.string.submit_update_message, uiState.submittedAppName ?: "") else stringResource(R.string.submit_success_message, uiState.submittedAppName ?: ""))
            },
            confirmButton = {
                Button(
                    onClick = onSuccess
                ) {
                    Text(stringResource(R.string.submit_done))
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) stringResource(R.string.submit_title_edit) else stringResource(R.string.submit_title_new)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.submit_what_submitting),
                    style = MaterialTheme.typography.titleMedium
                )
                val uriHandler = LocalUriHandler.current
                TextButton(onClick = { uriHandler.openUri("https://github.com/jksalcedo/librefind/wiki/How-to-Contribute") }) {
                    Text(stringResource(R.string.submit_contribution_guide), style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == SubmissionType.NEW_ALTERNATIVE,
                    onClick = { type = SubmissionType.NEW_ALTERNATIVE },
                    label = { Text(stringResource(R.string.submit_type_foss)) }
                )
                FilterChip(
                    selected = type == SubmissionType.NEW_PROPRIETARY,
                    onClick = { type = SubmissionType.NEW_PROPRIETARY },
                    label = { Text(stringResource(R.string.submit_type_proprietary)) }
                )
                FilterChip(
                    selected = type == SubmissionType.LINKING,
                    onClick = { type = SubmissionType.LINKING },
                    label = { Text(stringResource(R.string.submit_type_link)) }
                )
            }

            HorizontalDivider()

            uiState.duplicateWarning?.let { warning ->
                Text(
                    text = warning,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (type != SubmissionType.LINKING) {

                val appNameHelp = SubmitFieldHelp.getAppName()
                FieldWithHelp(
                    helpTitle = appNameHelp.title,
                    helpText = appNameHelp.description,
                    tipText = appNameHelp.tip
                ) {
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text(stringResource(R.string.submit_app_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = uiState.linkedSolution != null
                    )
                }

                val packageNameHelp = SubmitFieldHelp.getPackageName()
                FieldWithHelp(
                    helpTitle = packageNameHelp.title,
                    helpText = packageNameHelp.description,
                    tipText = packageNameHelp.tip
                ) {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = {
                            packageName = it
                            onCheckDuplicate(it)
                            onValidatePackageName(it)
                        },
                        label = { Text(stringResource(R.string.submit_package_name)) },
                        placeholder = { Text(stringResource(R.string.submit_package_placeholder)) },
                        singleLine = true,
                        isError = uiState.packageNameError != null,
                        supportingText = {
                            uiState.packageNameError?.let { error ->
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = uiState.linkedSolution != null
                    )
                }

                val descriptionHelp = SubmitFieldHelp.getDescription()
                FieldWithHelp(
                    helpTitle = descriptionHelp.title,
                    helpText = descriptionHelp.description
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = {
                            Text(
                                if (type == SubmissionType.NEW_ALTERNATIVE) stringResource(R.string.submit_description_required)
                                else stringResource(R.string.submit_description)
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (type == SubmissionType.NEW_PROPRIETARY) {
                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.submit_add_alternatives),
                        style = MaterialTheme.typography.titleMedium
                    )

                    var alternativeSearchQuery by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = alternativeSearchQuery,
                        onValueChange = {
                            alternativeSearchQuery = it
                            onSearchSolutions(it)
                        },
                        label = { Text(stringResource(R.string.submit_search_alternatives)) },
                        placeholder = { Text(stringResource(R.string.submit_search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (alternativeSearchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    alternativeSearchQuery = ""
                                    onClearSolutionSearchResults()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.solutionSearchResults.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column {
                                uiState.solutionSearchResults.take(5).forEach { solution ->
                                    val isSelected =
                                        uiState.selectedAlternatives.contains(solution.packageName)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) {
                                                    onRemoveAlternative(solution.packageName)
                                                } else {
                                                    onAddAlternative(solution.packageName)
                                                }
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = solution.name,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = solution.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null
                                        )
                                    }
                                    if (solution != uiState.solutionSearchResults.last()) {
                                        HorizontalDivider(modifier = Modifier.alpha(0.5f))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.selectedAlternatives.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.submit_selected_alternatives),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.selectedAlternatives.forEach { packageName ->
                                val solution =
                                    uiState.solutionSearchResults.find { it.packageName == packageName }
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveAlternative(packageName) },
                                    label = { Text(solution?.name ?: packageName) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(InputChipDefaults.AvatarSize)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (type == SubmissionType.NEW_ALTERNATIVE) {
                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.submit_alternative_details),
                        style = MaterialTheme.typography.titleMedium
                    )

                    // FOSS Search Section
                    if (uiState.linkedSolution == null) {
                        var fossSearchQuery by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = fossSearchQuery,
                            onValueChange = {
                                fossSearchQuery = it
                                onSearchFossApps(it)
                            },
                            label = { Text(stringResource(R.string.submit_search_foss_optional)) },
                            placeholder = { Text(stringResource(R.string.submit_search_foss_placeholder)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (fossSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        fossSearchQuery = ""
                                        onSearchFossApps("")
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.fossSearchResults.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                tonalElevation = 2.dp,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Column {
                                    uiState.fossSearchResults.forEach { app ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onSelectFossApp(app)
                                                    // Auto-fill fields
                                                    appName = app.name
                                                    packageName = app.packageName
                                                    description = app.description
                                                    repoUrl = app.repoUrl
                                                    fdroidId = app.fdroidId
                                                    license = app.license
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.name,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = app.packageName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                Icons.Default.Link,
                                                contentDescription = "Link",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (app != uiState.fossSearchResults.last()) {
                                            HorizontalDivider(modifier = Modifier.alpha(0.5f))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        // Linked App Display
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.submit_linked_app),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = uiState.linkedSolution.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = uiState.linkedSolution.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.8f
                                        )
                                    )
                                }
                                IconButton(onClick = {
                                    onClearLinkedApp()
                                }) {
                                    Icon(
                                        Icons.Default.LinkOff,
                                        contentDescription = "Unlink",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }


                    Text(
                        text = stringResource(R.string.submit_foss_requirements),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var showDialog by remember { mutableStateOf(false) }

                    val targetAppsHelp = SubmitFieldHelp.getTargetProprietaryApps()
                    FieldWithHelp(
                        helpTitle = targetAppsHelp.title,
                        helpText = targetAppsHelp.description,
                        tipText = targetAppsHelp.tip
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (selectedProprietaryPackages.isEmpty()) "" else "${selectedProprietaryPackages.size} selected",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.submit_target_proprietary)) },
                                placeholder = { Text(stringResource(R.string.submit_search_to_add)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDialog) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDialog = true }
                            )
                        }
                    }

                    if (selectedProprietaryPackages.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            selectedProprietaryPackages.forEach { selectedPkg ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        // Remove from set on click
                                        selectedProprietaryPackages =
                                            selectedProprietaryPackages - selectedPkg
                                    },
                                    label = { Text(selectedPkg) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(InputChipDefaults.AvatarSize)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (showDialog) {
                        MultiSelectDialog(
                            allPackages = uiState.proprietaryTargets,
                            unknownApps = uiState.unknownApps,
                            initialSelection = selectedProprietaryPackages,
                            onDismiss = { showDialog = false },
                            onConfirm = { newSelection ->
                                selectedProprietaryPackages = newSelection
                                showDialog = false
                            },
                            onSuggestAsTarget = { packageName, label ->
                                showDialog = false
                                onNavigateToTargetSubmission(label, packageName)
                            }
                        )
                    }

                    val repoUrlHelp = SubmitFieldHelp.getRepoUrl()
                    FieldWithHelp(
                        helpTitle = repoUrlHelp.title,
                        helpText = repoUrlHelp.description,
                        tipText = repoUrlHelp.tip
                    ) {
                        OutlinedTextField(
                            value = repoUrl,
                            onValueChange = {
                                repoUrl = it
                                onValidateRepoUrl(it)
                            },
                            label = { Text(stringResource(R.string.submit_repo_url)) },
                            placeholder = { Text(stringResource(R.string.submit_repo_placeholder)) },
                            singleLine = true,
                            isError = uiState.repoUrlError != null,
                            supportingText = {
                                uiState.repoUrlError?.let { error ->
                                    Text(error, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = uiState.linkedSolution != null
                        )
                    }

                    val fdroidIdHelp = SubmitFieldHelp.getFdroidId()
                    FieldWithHelp(
                        helpTitle = fdroidIdHelp.title,
                        helpText = fdroidIdHelp.description,
                        tipText = fdroidIdHelp.tip
                    ) {
                        OutlinedTextField(
                            value = fdroidId,
                            onValueChange = { fdroidId = it },
                            label = { Text(stringResource(R.string.submit_fdroid_id)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = uiState.linkedSolution != null
                        )
                    }

                    var showLicenseDropdown by remember { mutableStateOf(false) }
                    val commonLicenses = listOf(
                        "MIT",
                        "Apache-2.0",
                        "GPL-3.0",
                        "GPL-2.0",
                        "LGPL-3.0",
                        "LGPL-2.1",
                        "BSD-3-Clause",
                        "BSD-2-Clause",
                        "MPL-2.0",
                        "AGPL-3.0",
                        "ISC",
                        "Unlicense",
                        "EPL-2.0",
                        "CC0-1.0",
                        "CC-BY-4.0",
                        "CC-BY-SA-4.0",
                        "WTFPL",
                        "Unknown",
                        "WTFPL",
                        "Unknown",
                        stringResource(R.string.submit_other)
                    )

                    val isCustomLicense = license.isNotBlank() && license !in commonLicenses
                    var showCustomLicenseField by remember(license) { mutableStateOf(isCustomLicense || license == stringResource(R.string.submit_other)) }

                    val licenseHelp = SubmitFieldHelp.getLicense()
                    FieldWithHelp(
                        helpTitle = licenseHelp.title,
                        helpText = licenseHelp.description,
                        tipText = licenseHelp.tip
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (isCustomLicense) stringResource(R.string.submit_other) else license,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.submit_license)) },
                                placeholder = { Text(stringResource(R.string.submit_license_placeholder)) },
                                trailingIcon = {
                                    if (uiState.linkedSolution == null)
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLicenseDropdown)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (uiState.linkedSolution == null) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showLicenseDropdown = true }
                                )
                            }
                        }
                    }

                    if (showCustomLicenseField || license == stringResource(R.string.submit_other)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = if (license == stringResource(R.string.submit_other) || (license in commonLicenses && license != stringResource(R.string.submit_other))) "" else license,
                            onValueChange = { license = it },
                            label = { Text(stringResource(R.string.submit_custom_license)) },
                            placeholder = { Text(stringResource(R.string.submit_custom_license_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = uiState.linkedSolution != null
                        )
                    }

                    if (showLicenseDropdown) {
                        AlertDialog(
                            onDismissRequest = { showLicenseDropdown = false },
                            title = { Text(stringResource(R.string.submit_select_license)) },
                            text = {
                                LazyColumn {
                                    items(commonLicenses) { licenseName ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    license = if (licenseName == stringResource(R.string.submit_other)) {
                                                        stringResource(R.string.submit_other)
                                                    } else {
                                                        licenseName
                                                    }
                                                    showLicenseDropdown = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = licenseName,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        if (licenseName != commonLicenses.last()) {
                                            HorizontalDivider(modifier = Modifier.alpha(0.5f))
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showLicenseDropdown = false }) {
                                    Text(stringResource(R.string.submit_cancel))
                                }
                            }
                        )
                    }
                }
            } else {
                // LINKING
                Text(
                    text = stringResource(R.string.submit_link_solutions_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.submit_link_solutions_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Target App Selection
                var showTargetDialog by remember { mutableStateOf(false) }

                val targetAppsHelp = SubmitFieldHelp.getTargetProprietaryApps()
                FieldWithHelp(
                    helpTitle = stringResource(R.string.submit_target_app),
                    helpText = stringResource(R.string.submit_link_solutions_target)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.linkTargetPackage ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.submit_target_app)) },
                            placeholder = { Text(stringResource(R.string.submit_select_target)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTargetDialog) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTargetDialog = true }
                        )
                    }
                }

                if (showTargetDialog) {
                    MultiSelectDialog(
                        allPackages = uiState.proprietaryTargets, // Reuse existing list for now
                        unknownApps = uiState.unknownApps,
                        initialSelection = uiState.linkTargetPackage?.let { setOf(it) } ?: emptySet(),
                        onDismiss = { showTargetDialog = false },
                        onConfirm = { newSelection ->
                            // MultiSelectDialog returns a Set, but we only want one for target
                            newSelection.firstOrNull()?.let {
                                onAddTarget(it)
                            }
                            showTargetDialog = false
                        },
                        onSuggestAsTarget = { packageName, label ->
                            showTargetDialog = false
                            onNavigateToTargetSubmission(label, packageName)
                        }
                    )
                }

                HorizontalDivider()

                // Solutions Selection
                Text(
                    text = stringResource(R.string.submit_select_solutions),
                    style = MaterialTheme.typography.titleMedium
                )

                var alternativeSearchQuery by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = alternativeSearchQuery,
                    onValueChange = {
                        alternativeSearchQuery = it
                        onSearchSolutions(it)
                    },
                    label = { Text(stringResource(R.string.submit_search_solutions)) },
                    placeholder = { Text(stringResource(R.string.submit_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (alternativeSearchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                alternativeSearchQuery = ""
                                onClearSolutionSearchResults()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.solutionSearchResults.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column {
                            uiState.solutionSearchResults.take(5).forEach { solution ->
                                val isSelected =
                                    uiState.selectedAlternatives.contains(solution.packageName)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) {
                                                onRemoveAlternative(solution.packageName)
                                            } else {
                                                onAddAlternative(solution.packageName)
                                            }
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = solution.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = solution.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )
                                }
                                if (solution != uiState.solutionSearchResults.last()) {
                                    HorizontalDivider(modifier = Modifier.alpha(0.5f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.selectedAlternatives.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.submit_selected_solutions),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.selectedAlternatives.forEach { packageName ->
                            // Look up name in search results or just show package name
                            val solution =
                                uiState.solutionSearchResults.find { it.packageName == packageName }
                            InputChip(
                                selected = true,
                                onClick = { onRemoveAlternative(packageName) },
                                label = { Text(solution?.name ?: packageName) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(InputChipDefaults.AvatarSize)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSubmit(
                        type,
                        appName,
                        packageName,
                        description,
                        repoUrl,
                        fdroidId,
                        license,
                        selectedProprietaryPackages.joinToString(", ")
                    )
                },
                enabled = !uiState.isLoading && (
                    if (type == SubmissionType.LINKING) {
                        uiState.linkTargetPackage != null && uiState.selectedAlternatives.isNotEmpty()
                    } else {
                        appName.isNotBlank() &&
                                packageName.isNotBlank() &&
                                uiState.duplicateWarning == null &&
                                uiState.packageNameError == null &&
                                if (type == SubmissionType.NEW_ALTERNATIVE) {
                                    description.isNotBlank() && repoUrl.isNotBlank() && license.isNotBlank() && uiState.repoUrlError == null
                                } else {
                                    true
                                }
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (uiState.isEditing) stringResource(R.string.submit_update_button) else stringResource(R.string.submit_submit_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDialog(
    allPackages: List<String>,
    unknownApps: Map<String, String>,
    initialSelection: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    onSuggestAsTarget: (packageName: String, label: String) -> Unit
) {
    var tempSelection by remember { mutableStateOf(initialSelection) }
    var searchText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 90% of screen width
                .fillMaxHeight(0.8f), // 80% of screen height
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = stringResource(R.string.submit_select_targets_dialog),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar inside Dialog
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text(stringResource(R.string.submit_search)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val filteredList = allPackages.filter {
                        it.contains(searchText, ignoreCase = true)
                    }

                    val filteredUnknown = if (searchText.isNotBlank()) {
                        unknownApps.filter { (pkg, label) ->
                            label.contains(searchText, ignoreCase = true) ||
                                    pkg.contains(searchText, ignoreCase = true)
                        }
                    } else {
                        emptyMap()
                    }

                    if (filteredList.isEmpty() && searchText.isNotBlank() && filteredUnknown.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.submit_no_targets),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.submit_unknown_apps),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(filteredUnknown.toList()) { (packageName, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSuggestAsTarget(packageName, label)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.submit_suggest_target),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(modifier = Modifier.alpha(0.5f))
                        }
                    } else if (filteredList.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.submit_no_results),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    items(filteredList) { pkg ->
                        val isSelected = tempSelection.contains(pkg)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelection = if (isSelected) {
                                        tempSelection - pkg
                                    } else {
                                        tempSelection + pkg
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    tempSelection =
                                        if (checked) tempSelection + pkg else tempSelection - pkg
                                }
                            )
                            Text(
                                text = pkg,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        HorizontalDivider(modifier = Modifier.alpha(0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.submit_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(tempSelection) }) {
                        Text("${stringResource(R.string.submit_confirm)} (${tempSelection.size})")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SubmitScreenPreview() {
    SubmitContent(
        uiState = SubmitUiState(),
        onBackClick = {},
        onSuccess = {},
        onNavigateToTargetSubmission = { _, _ -> },
        prefilledAppName = null,
        prefilledPackageName = null,
        prefilledType = null,
        onCheckDuplicate = {},
        onValidatePackageName = {},
        onSearchSolutions = {},
        onClearSolutionSearchResults = {},
        onAddAlternative = {},
        onRemoveAlternative = {},
        onSearchFossApps = {},
        onSelectFossApp = {},
        onClearLinkedApp = {},
        onValidateRepoUrl = {},
        onSubmit = { _, _, _, _, _, _, _, _ -> },
        onAddTarget = {}
    )
}