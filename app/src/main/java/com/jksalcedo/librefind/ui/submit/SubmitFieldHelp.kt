package com.jksalcedo.librefind.ui.submit

object SubmitFieldHelp {

    val appName = FieldHelpContent(
        title = "App Name",
        description = "The display name of the app as users know it (e.g. \"Signal\", \"Firefox\", \"VLC\").",
        tip = "Use the official name from the app's website or store listing."
    )

    val packageName = FieldHelpContent(
        title = "Package Name",
        description = "The unique technical identifier for the app (e.g. \"org.mozilla.firefox\"). " +
                "This is NOT the display name.",
        tip = "Find it on F-Droid (listed on the app page), or on the Play Store URL: " +
                "play.google.com/store/apps/details?id=<package.name>. " +
                "On your device: Settings → Apps → [App] → App info."
    )

    val description = FieldHelpContent(
        title = "Description",
        description = "A brief summary of what the app does. Keep it to 1-3 sentences.",
        tip = null
    )

    val repoUrl = FieldHelpContent(
        title = "Repository URL",
        description = "The link to the app's public source code repository. " +
                "This is required to verify the app is truly open source.",
        tip = "Look for it on the app's website, F-Droid listing, or search " +
                "\"<app name> source code\" on GitHub/GitLab/Codeberg."
    )

    val fdroidId = FieldHelpContent(
        title = "F-Droid ID",
        description = "The package name as listed on F-Droid. Usually the same as the package name, " +
                "but some apps use a different ID for F-Droid builds.",
        tip = "Find it at f-droid.org — the ID is in the URL: " +
                "f-droid.org/packages/<fdroid.id>/"
    )

    val license = FieldHelpContent(
        title = "License",
        description = "The open source license the app is released under (e.g. GPL-3.0, MIT, Apache-2.0). " +
                "This is required for FOSS submissions.",
        tip = "Check the LICENSE or COPYING file in the app's source code repository, " +
                "or look at the F-Droid listing."
    )

    val targetProprietaryApps = FieldHelpContent(
        title = "Target Proprietary Apps",
        description = "The proprietary apps that this FOSS app can replace. " +
                "For example, Signal replaces WhatsApp/Telegram.",
        tip = "Select from the list of known proprietary apps. If the target isn't listed, " +
                "you can suggest it as a new proprietary target first."
    )

    val searchAlternatives = FieldHelpContent(
        title = "Search Alternatives",
        description = "Search for existing FOSS alternatives in our database to link to this proprietary app.",
        tip = null
    )
}

data class FieldHelpContent(
    val title: String,
    val description: String,
    val tip: String?
)