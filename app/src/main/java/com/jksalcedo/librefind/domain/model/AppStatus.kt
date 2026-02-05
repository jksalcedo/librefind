package com.jksalcedo.librefind.domain.model

enum class AppStatus {
    FOSS,
    PROP,
    UNKN,
    IGNORED,
    PENDING;

    val sortWeight: Int
        get() = when(this) {
            PROP -> 1
            UNKN -> 2
            PENDING -> 3
            FOSS -> 4
            IGNORED -> 5
        }
}
