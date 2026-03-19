
package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.common.network.Dispatcher
import com.lhzkml.jasmine.core.common.network.JasmineDispatchers.IO
import com.lhzkml.jasmine.core.database.dao.TopicDao
import com.lhzkml.jasmine.core.database.dao.TopicFtsDao
import com.lhzkml.jasmine.core.database.model.asExternalModel
import com.lhzkml.jasmine.core.database.model.asFtsEntity
import com.lhzkml.jasmine.core.model.data.SearchResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultSearchContentsRepository @Inject constructor(
    private val topicDao: TopicDao,
    private val topicFtsDao: TopicFtsDao,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : SearchContentsRepository {

    override suspend fun populateFtsData() {
        withContext(ioDispatcher) {
            topicFtsDao.insertAll(topicDao.getOneOffTopicEntities().map { it.asFtsEntity() })
        }
    }

    override fun searchContents(searchQuery: String): Flow<SearchResult> {
        // Surround the query by asterisks to match the query when it's in the middle of
        // a word
        val topicIds = topicFtsDao.searchAllTopics("*$searchQuery*")

        return topicIds
            .mapLatest { it.toSet() }
            .distinctUntilChanged()
            .flatMapLatest(topicDao::getTopicEntities)
            .mapLatest { topics ->
                SearchResult(
                    topics = topics.map { it.asExternalModel() },
                )
            }
    }

    override fun getSearchContentsCount(): Flow<Int> =
        topicFtsDao.getCount()
}


