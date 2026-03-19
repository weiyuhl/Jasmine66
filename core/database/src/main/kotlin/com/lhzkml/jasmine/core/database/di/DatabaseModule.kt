
package com.lhzkml.jasmine.core.database.di

import android.content.Context
import androidx.room.Room
import com.lhzkml.jasmine.core.database.JasmineDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesJasmineDatabase(
        @ApplicationContext context: Context,
    ): JasmineDatabase = Room.databaseBuilder(
        context,
        JasmineDatabase::class.java,
        "jasmine-database",
    ).build()
}

