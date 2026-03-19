/*
 * Copyright 2024 The Android Open Source Project
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

package com.lhzkml.jasmine.core.database.di

import com.lhzkml.jasmine.core.database.JasmineDatabase
import com.lhzkml.jasmine.core.database.dao.RecentSearchQueryDao
import com.lhzkml.jasmine.core.database.dao.TopicDao
import com.lhzkml.jasmine.core.database.dao.TopicFtsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {
    @Provides
    fun providesTopicsDao(
        database: JasmineDatabase,
    ): TopicDao = database.topicDao()


    @Provides
    fun providesTopicFtsDao(
        database: JasmineDatabase,
    ): TopicFtsDao = database.topicFtsDao()


    @Provides
    fun providesRecentSearchQueryDao(
        database: JasmineDatabase,
    ): RecentSearchQueryDao = database.recentSearchQueryDao()
}

