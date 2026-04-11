package com.jksalcedo.librefind.util

object VersionUtils {
    private data class ParsedVersion(
        val core: List<Int>,
        val pre: PreRelease?
    )

    private data class PreRelease(
        val labelRank: Int, // alpha < beta < rc < stable
        val number: Int
    )

    private fun parseVersion(v: String): ParsedVersion {
        val normalized = v.trim().lowercase()

        val parts = normalized.split("-", limit = 2)
        val core = parts[0]
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

        val pre = if (parts.size == 1) {
            null // stable
        } else {
            val tag = parts[1].trim()

            // supports: alpha, alpha1, beta, beta11, rc, rc2, preview, nightly, etc.
            val m = Regex("""([a-z]+)(\d+)?""").matchEntire(tag)
            val label = m?.groupValues?.getOrNull(1).orEmpty()
            val num = m?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0

            val rank = when (label) {
                "nightly", "dev", "snapshot", "preview" -> 0
                "alpha" -> 1
                "beta" -> 2
                "rc" -> 3
                else -> 0 // unknown prerelease: treat as very early prerelease
            }

            PreRelease(labelRank = rank, number = num)
        }

        return ParsedVersion(core = core, pre = pre)
    }

    private fun compareVersions(a: String, b: String): Int {
        val va = parseVersion(a)
        val vb = parseVersion(b)

        val size = maxOf(va.core.size, vb.core.size)
        for (i in 0 until size) {
            val x = va.core.getOrElse(i) { 0 }
            val y = vb.core.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }

        // Same core: stable > any prerelease
        if (va.pre == null && vb.pre == null) return 0
        if (va.pre == null) return 1
        if (vb.pre == null) return -1

        // Both prerelease: compare label then number
        if (va.pre.labelRank != vb.pre.labelRank) {
            return va.pre.labelRank.compareTo(vb.pre.labelRank)
        }
        return va.pre.number.compareTo(vb.pre.number)
    }

    public fun isNewerVersion(latest: String, current: String): Boolean {
        return compareVersions(latest, current) > 0
    }
}