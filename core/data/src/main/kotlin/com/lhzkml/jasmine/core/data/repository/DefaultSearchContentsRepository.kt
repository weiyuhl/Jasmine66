/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lhzkml.jasmine.core.data.repository

import com.lhzkml.jasmine.core.common.network.Dispatcher
import com.lhzkml.jasmine.core.common.network.NiaDispatchers.IO
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
