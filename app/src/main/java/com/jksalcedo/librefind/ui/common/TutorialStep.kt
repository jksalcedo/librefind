package com.jksalcedo.librefind.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

sealed class TutorialStep(
    val title: String,
    val description: String,
    val targetArea: TargetArea
) {
    data object GaugeStep : TutorialStep(
        title = "Sovereignty Score",
        description = "This gauge shows your FOSS usage. Higher means more free software!",
        targetArea = TargetArea.GAUGE
    )

    data object FilterStep : TutorialStep(
        title = "Filter Apps",
        description = "Tap these chips to filter by app type: FOSS, Proprietary, or Unknown.",
        targetArea = TargetArea.FILTER_CHIPS
    )

    data object SearchStep : TutorialStep(
        title = "Search",
        description = "Search for installed apps by name.",
        targetArea = TargetArea.SEARCH
    )

    data object ProfileStep : TutorialStep(
        title = "Profile & Settings",
        description = "Access your profile, submissions, ignored apps, and settings here.",
        targetArea = TargetArea.PROFILE
    )

    data object FabStep : TutorialStep(
        title = "Submit an App",
        description = "Know if an app is FOSS? Tap here to submit it and help the community!",
        targetArea = TargetArea.FAB
    )

    companion object {
        val allSteps = listOf(GaugeStep, FilterStep, SearchStep, ProfileStep, FabStep)
    }
}

enum class TargetArea {
    GAUGE,
    FILTER_CHIPS,
    SEARCH,
    PROFILE,
    FAB
}
