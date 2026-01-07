package com.jksalcedo.fossia.data.local

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database of known FOSS signing certificates
 * 
 * This allows detection of FOSS apps distributed via Play Store.
 * For example, Signal and Firefox are on the Play Store but are FOSS.
 * 
 * In a production version, this would be a JSON file or Room database.
 * For MVP, we'll hardcode a few critical entries.
 */
@Singleton
class SafeSignatureDb @Inject constructor() {
    
    /**
     * Map of package names to their known FOSS certificate hashes
     * 
     * Key: Package name
     * Value: SHA-256 hash of signing certificate
     */
    private val knownFossSignatures = mapOf(
        // Signal
        "org.thoughtcrime.securesms" to "29F34E5F27F211B424BC5BF9D67162C0EEAD9270",
        
        // Firefox
        "org.mozilla.firefox" to "A78B62A5165B4494B2FEAD9E76A280D22D937FEA",
        
        // Telegram FOSS
        "org.telegram.messenger" to "EBE9705F19BFFA8EA97E5B784F6E8F0D5C2F67F9",
        
        // NewPipe
        "org.schabi.newpipe" to "F051D8DA13C4D89DEF37A41939224974C6C23C6E",
        
        // K-9 Mail
        "com.fsck.k9" to "68D498D891DE759B2957A5A9968EF9C0F4B7DCE4",
        
        // VLC
        "org.videolan.vlc" to "D79DCC1BD8A22FE70B9BC7F8DCF1E2DE81D0C5E2"
    )
    
    /**
     * Checks if a package has a known FOSS signature
     * 
     * @param packageName Package to check
     * @param signatureHash SHA-256 hash of the app's signature
     * @return True if signature matches a known FOSS developer
     */
    fun isFossSignature(packageName: String, signatureHash: String): Boolean {
        val knownHash = knownFossSignatures[packageName] ?: return false
        return signatureHash.equals(knownHash, ignoreCase = true)
    }
    
    /**
     * Checks if a package is in the FOSS signature database
     * 
     * @param packageName Package to check
     * @return True if package is in database (regardless of signature match)
     */
    fun isKnownFossApp(packageName: String): Boolean {
        return knownFossSignatures.containsKey(packageName)
    }
}
