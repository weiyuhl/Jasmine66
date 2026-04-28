package com.lhzkml.jasmine.core.conversation.storage.di

import android.content.Context
import androidx.room.Room
import com.lhzkml.jasmine.core.conversation.storage.ConversationDatabase
import com.lhzkml.jasmine.core.conversation.storage.dao.ConversationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConversationDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ConversationDatabase {
        return Room.databaseBuilder(
            context,
            ConversationDatabase::class.java,
            "jasmine.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideConversationDao(database: ConversationDatabase): ConversationDao {
        return database.conversationDao()
    }
}
