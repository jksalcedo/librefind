package com.jksalcedo.librefind.util

import android.content.pm.PackageInfo
import android.os.Build
import java.security.MessageDigest

object SignerUtils {

    fun signerSha256Digests(pkg: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val si = pkg.signingInfo ?: return emptySet()
            si.apkContentsSigners?.toList().orEmpty()
        } else {
            @Suppress("DEPRECATION")
            pkg.signatures?.toList().orEmpty()
        }

        return signatures.map { sig ->
            sha256Hex(sig.toByteArray())
        }.toSet()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}