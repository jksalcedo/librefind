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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jksalcedo.librefind.domain.model.SubmissionType
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(submissionId) {
        if (submissionId != null) {
            viewModel.loadSubmission(submissionId)
        }
    }

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

    LaunchedEffect(prefilledAppName, prefilledPackageName) {
        if (!prefilledPackageName.isNullOrBlank()) {
            viewModel.checkDuplicate(prefilledPackageName)
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
            title = { Text(if (uiState.isEditing) "Submission Updated!" else "Submission Received!") },
            text = {
                Text(if (uiState.isEditing) "Your submission for '${uiState.submittedAppName}' has been updated." else "Thanks! Your submission for '${uiState.submittedAppName}' has been received.")
            },
            confirmButton = {
                Button(
                    onClick = onSuccess
                ) {
                    Text("Done")
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Submission" else "Submit App") },
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
            Text(
                text = "What are you submitting?",
                style = MaterialTheme.typography.titleMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == SubmissionType.NEW_ALTERNATIVE,
                    onClick = { type = SubmissionType.NEW_ALTERNATIVE },
                    label = { Text("FOSS App") }
                )
                FilterChip(
                    selected = type == SubmissionType.NEW_PROPRIETARY,
                    onClick = { type = SubmissionType.NEW_PROPRIETARY },
                    label = { Text("Proprietary App") }
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

            OutlinedTextField(
                value = appName,
                onValueChange = {
                    appName = it
                },
                label = { Text("App Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = packageName,
                onValueChange = {
                    packageName = it
                    viewModel.checkDuplicate(it)
                    viewModel.validatePackageName(it)
                },
                label = { Text("Package Name *") },
                placeholder = { Text("com.example.app") },
                singleLine = true,
                isError = uiState.packageNameError != null,
                supportingText = {
                    uiState.packageNameError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            if (type == SubmissionType.NEW_PROPRIETARY) {
                HorizontalDivider()

                Text(
                    text = "Add Alternatives (optional)",
                    style = MaterialTheme.typography.titleMedium
                )

                var alternativeSearchQuery by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = alternativeSearchQuery,
                    onValueChange = {
                        alternativeSearchQuery = it
                        viewModel.searchSolutions(it)
                    },
                    label = { Text("Search for alternatives") },
                    placeholder = { Text("Search by app name or package...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (alternativeSearchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                alternativeSearchQuery = ""
                                viewModel.clearSolutionSearchResults()
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
                                                viewModel.removeAlternative(solution.packageName)
                                            } else {
                                                viewModel.addAlternative(solution.packageName)
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
                        text = "Selected Alternatives:",
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
                                onClick = { viewModel.removeAlternative(packageName) },
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
                    text = "Alternative Details",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "License and Repository URL are required for FOSS alternatives",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var showDialog by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedProprietaryPackages.isEmpty()) "" else "${selectedProprietaryPackages.size} selected",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Proprietary Apps") },
                        placeholder = { Text("Search to add...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDialog) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDialog = true }
                    )
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

                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = {
                        repoUrl = it
                        viewModel.validateRepoUrl(it)
                    },
                    label = { Text("Repository URL *") },
                    placeholder = { Text("https://github.com/...") },
                    singleLine = true,
                    isError = uiState.repoUrlError != null,
                    supportingText = {
                        uiState.repoUrlError?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fdroidId,
                    onValueChange = { fdroidId = it },
                    label = { Text("F-Droid ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                    "Other"
                )

                val isCustomLicense = license.isNotBlank() && license !in commonLicenses
                var showCustomLicenseField by remember(license) { mutableStateOf(isCustomLicense || license == "Other") }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (isCustomLicense) "Other" else license,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("License *") },
                        placeholder = { Text("Select a license") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLicenseDropdown) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showLicenseDropdown = true }
                    )
                }

                if (showCustomLicenseField || license == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (license == "Other" || (license in commonLicenses && license != "Other")) "" else license,
                        onValueChange = { license = it },
                        label = { Text("Custom License Name *") },
                        placeholder = { Text("e.g. My Custom License") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showLicenseDropdown) {
                    AlertDialog(
                        onDismissRequest = { showLicenseDropdown = false },
                        title = { Text("Select License") },
                        text = {
                            LazyColumn {
                                items(commonLicenses) { licenseName ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (licenseName == "Other") {
                                                    license = "Other"
                                                } else {
                                                    license = licenseName
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
                                Text("Cancel")
                            }
                        }
                    )
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
                    viewModel.submit(
                        type = type,
                        appName = appName,
                        packageName = packageName,
                        description = description,
                        repoUrl = repoUrl,
                        fdroidId = fdroidId,
                        license = license,
                        proprietaryPackages = selectedProprietaryPackages.joinToString(", ")
                    )
                },
                enabled = appName.isNotBlank() &&
                        packageName.isNotBlank() &&
                        uiState.duplicateWarning == null &&
                        !uiState.isLoading &&
                        uiState.packageNameError == null &&
                        uiState.repoUrlError == null &&
                        (type == SubmissionType.NEW_PROPRIETARY || (repoUrl.isNotBlank() && license.isNotBlank())),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (uiState.isEditing) "Update Submission" else "Submit for Review")
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
    var showSheet by remember { mutableStateOf(false) }

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
                    text = "Select Target Apps",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar inside Dialog
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search") },
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
                                "No targets found in database.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Unknown apps from your device:",
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
                                    text = "Suggest as target",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(modifier = Modifier.alpha(0.5f))
                        }
                    } else if (filteredList.isEmpty()) {
                        item {
                            Text(
                                "No results found.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
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
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(tempSelection) }) {
                        Text("Confirm (${tempSelection.size})")
                    }
                }
            }
        }
    }
}