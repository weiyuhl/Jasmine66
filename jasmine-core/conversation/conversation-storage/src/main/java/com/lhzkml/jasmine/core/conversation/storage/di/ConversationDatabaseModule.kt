package com.lhzkml.jasmine.core.conversation.storage.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private const val TAG = "ConversationDB"

    /**
     * 迁移策略：
     * - 版本升级时必须提供 Migration 对象（见 MIGRATION_1_2 示例）
     * - 版本降级时使用破坏性迁移（可接受，降级场景罕见）
     * - 未注册的升级迁移会触发 fallbackToDestructiveMigration 并记录日志
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 示例：当需要从 v1 升级到 v2 时，在此添加 SQL 迁移语句
            // db.execSQL("ALTER TABLE conversations ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            Log.i(TAG, "Migration 1→2 completed")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ConversationDatabase {
        return Room.databaseBuilder(
            context,
            ConversationDatabase::class.java,
            "jasmine.db"
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    Log.w(TAG, "Destructive migration occurred - all conversation data was lost!")
                }
            })
            .build()
    }

    @Provides
    fun provideConversationDao(database: ConversationDatabase): ConversationDao {
        return database.conversationDao()
    }
}
