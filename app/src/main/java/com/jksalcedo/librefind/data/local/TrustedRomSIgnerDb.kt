package com.jksalcedo.librefind.data.local

class TrustedRomSignerDb {
    /**
     * SHA-256 of cert bytes from Signature.toByteArray()
     * digests collected from:
     * - Pixel stock ROM
     * - GrapheneOS
     * - LineageOS
     */
    private val trustedSignerSha256 = setOf(
        // "..." // Pixel platform cert digest
        // "..." // GrapheneOS platform cert digest
        // "..." // LineageOS platform cert digest
        // TODO add signatures
        ""
    )

    fun isTrustedSigner(signerDigests: Set<String>): Boolean =
        signerDigests.any { it in trustedSignerSha256 }
}