
package com.lhzkml.jasmine.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.lhzkml.jasmine.core.common.network.di.ApplicationScope
import com.lhzkml.jasmine.core.datastore.UserPreferences
import com.lhzkml.jasmine.core.datastore.UserPreferencesSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    internal fun providesUserPreferencesDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        userPreferencesSerializer: UserPreferencesSerializer,
    ): DataStore<UserPreferences> =
        DataStoreFactory.create(
            serializer = userPreferencesSerializer,
            scope = scope,
            // WARNING: When UserPreferences.proto schema changes (fields added/removed/renumbered),
            // you MUST add a DataMigration here. Otherwise DataStore will crash on upgrade.
            // Example: listOf(SharedPreferencesMigration(context, "old_prefs"))
            // Test: increment UserPreferencesVersion in tests and verify migration succeeds.
            migrations = listOf(),
        ) {
            context.dataStoreFile("user_preferences.pb")
        }
}

