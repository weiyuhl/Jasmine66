
package com.lhzkml.jasmine.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lhzkml.jasmine.core.database.JasmineDatabase
import org.junit.After
import org.junit.Before

internal abstract class DatabaseTest {

    private lateinit var db: JasmineDatabase
    protected lateinit var topicDao: TopicDao

    @Before
    fun setup() {
        db = run {
            val context = ApplicationProvider.getApplicationContext<Context>()
            Room.inMemoryDatabaseBuilder(
                context,
                JasmineDatabase::class.java,
            ).build()
        }
        topicDao = db.topicDao()
    }

    @After
    fun teardown() = db.close()
}

