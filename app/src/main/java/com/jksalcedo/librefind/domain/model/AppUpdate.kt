package com.jksalcedo.librefind.domain.model

data class AppUpdate(
    val version: String,
    val changelog: String,
    val downloadUrl: String,
    val fileName: String,
    val isUpdateAvailable: Boolean
)
