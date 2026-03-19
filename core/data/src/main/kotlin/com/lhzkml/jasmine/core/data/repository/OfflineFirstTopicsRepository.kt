
package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.database.dao.TopicDao
import com.lhzkml.jasmine.core.database.model.TopicEntity
import com.lhzkml.jasmine.core.database.model.asExternalModel
import com.lhzkml.jasmine.core.model.data.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Disk storage backed implementation of the [TopicsRepository].
 * Reads are exclusively from local storage to support offline access.
 */
internal class OfflineFirstTopicsRepository @Inject constructor(
    private val topicDao: TopicDao,
) : TopicsRepository {

    override fun getTopics(): Flow<List<Topic>> =
        topicDao.getTopicEntities()
            .map { it.map(TopicEntity::asExternalModel) }

    override fun getTopic(id: String): Flow<Topic> =
        topicDao.getTopicEntity(id).map { it.asExternalModel() }
}

