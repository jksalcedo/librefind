package com.jksalcedo.librefind.domain.model

enum class DownvoteReason(val key: String) {
    DUPLICATE("duplicate"),
    WRONG_CATEGORY("wrong_category"),
    FAKE("fake"),
    NOT_ENOUGH_INFO("not_enough_info"),
    OTHER("other")
}
