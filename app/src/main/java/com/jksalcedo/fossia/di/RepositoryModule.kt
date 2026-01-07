package com.jksalcedo.fossia.di

import com.jksalcedo.fossia.data.repository.DeviceInventoryRepoImpl
import com.jksalcedo.fossia.data.repository.KnowledgeGraphRepoImpl
import com.jksalcedo.fossia.domain.repository.DeviceInventoryRepo
import com.jksalcedo.fossia.domain.repository.KnowledgeGraphRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding repository interfaces to implementations
 * 
 * Uses @Binds instead of @Provides for more efficient code generation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDeviceInventoryRepo(
        impl: DeviceInventoryRepoImpl
    ): DeviceInventoryRepo

    @Binds
    @Singleton
    abstract fun bindKnowledgeGraphRepo(
        impl: KnowledgeGraphRepoImpl
    ): KnowledgeGraphRepo
}
