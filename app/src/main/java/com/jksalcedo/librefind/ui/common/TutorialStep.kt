package com.jksalcedo.librefind.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

import androidx.annotation.StringRes
import com.jksalcedo.librefind.R

sealed class TutorialStep(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val targetArea: TargetArea
) {
    data object GaugeStep : TutorialStep(
        titleRes = R.string.tutorial_gauge_title,
        descriptionRes = R.string.tutorial_gauge_desc,
        targetArea = TargetArea.GAUGE
    )

    data object FilterStep : TutorialStep(
        titleRes = R.string.tutorial_filter_title,
        descriptionRes = R.string.tutorial_filter_desc,
        targetArea = TargetArea.FILTER_CHIPS
    )

    data object SearchStep : TutorialStep(
        titleRes = R.string.tutorial_search_title,
        descriptionRes = R.string.tutorial_search_desc,
        targetArea = TargetArea.SEARCH
    )

    data object ProfileStep : TutorialStep(
        titleRes = R.string.tutorial_profile_title,
        descriptionRes = R.string.tutorial_profile_desc,
        targetArea = TargetArea.PROFILE
    )

    data object FabStep : TutorialStep(
        titleRes = R.string.tutorial_fab_title,
        descriptionRes = R.string.tutorial_fab_desc,
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
