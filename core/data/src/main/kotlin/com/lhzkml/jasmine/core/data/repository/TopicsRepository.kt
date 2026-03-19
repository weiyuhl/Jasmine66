
package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.model.data.Topic
import kotlinx.coroutines.flow.Flow

interface TopicsRepository {
    /**
     * Gets the available topics as a stream
     */
    fun getTopics(): Flow<List<Topic>>

    /**
     * Gets data for a specific topic
     */
    fun getTopic(id: String): Flow<Topic>
}
