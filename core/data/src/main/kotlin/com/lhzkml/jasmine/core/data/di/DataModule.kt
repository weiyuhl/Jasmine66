package com.lhzkml.jasmine.core.data.di

import com.lhzkml.jasmine.core.data.repository.DefaultRecentSearchRepository
import com.lhzkml.jasmine.core.data.repository.OfflineFirstUserDataRepository
import com.lhzkml.jasmine.core.data.repository.RecentSearchRepository
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import com.lhzkml.jasmine.core.data.util.ConnectivityManagerNetworkMonitor
import com.lhzkml.jasmine.core.data.util.NetworkMonitor
import com.lhzkml.jasmine.core.data.util.TimeZoneBroadcastMonitor
import com.lhzkml.jasmine.core.data.util.TimeZoneMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    internal abstract fun bindsUserDataRepository(
        userDataRepository: OfflineFirstUserDataRepository,
    ): UserDataRepository

    @Binds
    @Singleton
    internal abstract fun bindsRecentSearchRepository(
        recentSearchRepository: DefaultRecentSearchRepository,
    ): RecentSearchRepository

    @Binds
    @Singleton
    internal abstract fun bindsNetworkMonitor(
        networkMonitor: ConnectivityManagerNetworkMonitor,
    ): NetworkMonitor

    @Binds
    @Singleton
    internal abstract fun binds(impl: TimeZoneBroadcastMonitor): TimeZoneMonitor
}
