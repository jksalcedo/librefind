package com.jksalcedo.fossia.domain.repository

import com.jksalcedo.fossia.domain.model.AppItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for device inventory operations
 * 
 * Abstracts the detection and classification of installed apps.
 * Implementation will use Android PackageManager API.
 */
interface DeviceInventoryRepo {
    /**
     * Scans and classifies all user-installed applications
     * 
     * Returns a Flow that emits the classified app list.
     * Apps are sorted by classification priority (PROP > UNKN > FOSS)
     * 
     * @return Flow of classified AppItem list
     */
    suspend fun scanAndClassify(): Flow<List<AppItem>>
    
    /**
     * Gets the installer package name for a specific app
     * 
     * @param packageName Package to query
     * @return Installer package name or null if unknown
     */
    fun getInstaller(packageName: String): String?
}
