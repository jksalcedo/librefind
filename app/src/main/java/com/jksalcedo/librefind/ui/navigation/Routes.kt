package com.jksalcedo.librefind.ui.navigation

sealed class Route(val route: String) {
    data object Dashboard : Route("dashboard")
    data object Details : Route("details/{appName}/{packageName}") {
        fun createRoute(appName: String ,packageName: String) = "details/$appName/$packageName"
    }
    data object AlternativeDetail : Route("alternative/{altId}") {
        fun createRoute(altId: String) = "alternative/$altId"
    }
    data object Auth : Route("auth")
    data object ProfileSetup : Route("profile_setup")
    data object Submit : Route("submit?appName={appName}&packageName={packageName}&type={type}&submissionId={submissionId}") {
        fun createRoute(
            appName: String? = null,
            packageName: String? = null,
            type: String? = null,
            submissionId: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (appName != null) params.add("appName=$appName")
            if (packageName != null) params.add("packageName=$packageName")
            if (type != null) params.add("type=$type")
            if (submissionId != null) params.add("submissionId=$submissionId")

            return if (params.isEmpty()) "submit" else "submit?${params.joinToString("&")}"
        }
    }
    data object MySubmissions : Route("my_submissions")
    data object IgnoredApps : Route("ignored_apps")
}


