package com.jksalcedo.librefind.util

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ErrorUtilsTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    @Test
    fun `sanitizeAuthError returns correct message for invalid credentials`() {
        val error = Exception("invalid login credentials")
        val result = ErrorUtils.sanitizeAuthError(error)
        assertEquals("Invalid email or password", result)
    }

    @Test
    fun `sanitizeAuthError returns correct message for already registered email`() {
        val error = Exception("email already registered")
        val result = ErrorUtils.sanitizeAuthError(error)
        assertEquals("An account with this email already exists", result)
    }

    @Test
    fun `sanitizeAuthError returns correct message for weak password`() {
        val error = Exception("weak password")
        val result = ErrorUtils.sanitizeAuthError(error)
        assertEquals("Password is too weak. Please use a stronger password", result)
    }

    @Test
    fun `sanitizeAuthError returns correct message for network error`() {
        val error = Exception("network timeout")
        val result = ErrorUtils.sanitizeAuthError(error)
        assertEquals("Network error. Please check your connection", result)
    }

    @Test
    fun `sanitizeAuthError returns correct message for rate limit`() {
        val error = Exception("rate limit exceeded")
        val result = ErrorUtils.sanitizeAuthError(error)
        assertEquals("Too many attempts. Please try again later", result)
    }

    @Test
    fun `sanitizeAuthError returns default message for unknown error`() {
        val error = Exception("Something went wrong")
        val result = ErrorUtils.sanitizeAuthError(error)
        assertEquals("Something went wrong", result)
    }

    @Test
    fun `sanitizeAuthError cleans message with details`() {
        val error = Exception("Generic Error Details: some extra info")
        val result = ErrorUtils.sanitizeAuthError(error)
        assertEquals("Generic Error", result)
    }

    @Test
    fun `sanitizeAuthError returns fallback for empty message`() {
        val error = Exception("")
        val result = ErrorUtils.sanitizeAuthError(error)
        assertEquals("An error occurred. Please try again", result)
    }
}
