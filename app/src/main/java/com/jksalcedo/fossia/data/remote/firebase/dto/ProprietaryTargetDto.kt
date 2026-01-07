package com.jksalcedo.fossia.data.remote.firebase.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Firestore "proprietary_targets" collection
 * 
 * Firestore Schema:
 * - doc_id: Sanitized package name (e.g., "com_whatsapp")
 * - name: Human-readable name
 * - icon: Icon URL
 * - category: App category
 * - alternatives: Array of alternative IDs
 */
data class ProprietaryTargetDto(
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("icon")
    val icon: String = "",
    
    @SerializedName("category")
    val category: String = "",
    
    @SerializedName("alternatives")
    val alternatives: List<String> = emptyList(),
    
    @SerializedName("package_name")
    val packageName: String = ""
)
