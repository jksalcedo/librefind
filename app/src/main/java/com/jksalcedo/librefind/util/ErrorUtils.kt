package com.jksalcedo.librefind.util

object ErrorUtils {

    fun sanitizeAuthError(error: Throwable): String {
        val message = error.message ?: return "An unexpected error occurred"

        return when {
            message.contains("invalid_grant", ignoreCase = true) ||
                    message.contains("invalid login credentials", ignoreCase = true) ->
                "Invalid email or password"

            message.contains("email already registered", ignoreCase = true) ||
                    message.contains("user already exists", ignoreCase = true) ||
                    message.contains("user_already_exists", ignoreCase = true) ->
                "An account with this email already exists"

            message.contains("weak password", ignoreCase = true) ||
                    message.contains("password is too weak", ignoreCase = true) ->
                "Password is too weak. Please use a stronger password"

            message.contains("invalid email", ignoreCase = true) ->
                "Please enter a valid email address"

            message.contains("network", ignoreCase = true) ||
                    message.contains("connectivity", ignoreCase = true) ||
                    message.contains("timeout", ignoreCase = true) ->
                "Network error. Please check your connection"

            message.contains("rate limit", ignoreCase = true) ->
                "Too many attempts. Please try again later"

            message.contains("not authorized", ignoreCase = true) ||
                    message.contains("unauthorized", ignoreCase = true) ->
                "You don't have permission to perform this action"

            message.contains("email not confirmed", ignoreCase = true) ->
                "Please verify your email before signing in"

            else -> {
                val cleanMessage = message
                    .substringBefore("Code:")
                    .substringBefore("(")
                    .substringBefore("Details:")
                    .trim()

                if (cleanMessage.isNotBlank() && cleanMessage.length < 100) {
                    cleanMessage
                } else {
                    "An error occurred. Please try again"
                }
            }
        }
    }
}