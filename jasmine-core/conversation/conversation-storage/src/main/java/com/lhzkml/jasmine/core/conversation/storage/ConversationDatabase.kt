package com.lhzkml.jasmine.core.conversation.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lhzkml.jasmine.core.conversation.storage.dao.ConversationDao
import com.lhzkml.jasmine.core.conversation.storage.entity.ConversationEntity
import com.lhzkml.jasmine.core.conversation.storage.entity.MessageEntity
import com.lhzkml.jasmine.core.conversation.storage.entity.UsageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, UsageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ConversationDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
}
