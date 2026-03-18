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

package com.lhzkml.jasmine.feature.search.impl

import androidx.lifecycle.SavedStateHandle
import com.lhzkml.jasmine.core.analytics.NoOpAnalyticsHelper
import com.lhzkml.jasmine.core.domain.GetRecentSearchQueriesUseCase
import com.lhzkml.jasmine.core.domain.GetSearchContentsUseCase
import com.lhzkml.jasmine.core.testing.data.topicsTestData
import com.lhzkml.jasmine.core.testing.repository.TestRecentSearchRepository
import com.lhzkml.jasmine.core.testing.repository.TestSearchContentsRepository
import com.lhzkml.jasmine.core.testing.repository.TestUserDataRepository
import com.lhzkml.jasmine.core.testing.repository.emptyUserData
import com.lhzkml.jasmine.core.testing.util.MainDispatcherRule
import com.lhzkml.jasmine.feature.search.impl.RecentSearchQueriesUiState.Success
import com.lhzkml.jasmine.feature.search.impl.SearchResultUiState.EmptyQuery
import com.lhzkml.jasmine.feature.search.impl.SearchResultUiState.Loading
import com.lhzkml.jasmine.feature.search.impl.SearchResultUiState.SearchNotReady
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * To learn more about how this test handles Flows created with stateIn, see
 * https://developer.android.com/kotlin/flow/test#statein
 */
class SearchViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val userDataRepository = TestUserDataRepository()
    private val searchContentsRepository = TestSearchContentsRepository()
    private val getSearchContentsUseCase = GetSearchContentsUseCase(
        searchContentsRepository = searchContentsRepository,
        userDataRepository = userDataRepository,
    )
    private val recentSearchRepository = TestRecentSearchRepository()
    private val getRecentQueryUseCase = GetRecentSearchQueriesUseCase(recentSearchRepository)

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        viewModel = SearchViewModel(
            getSearchContentsUseCase = getSearchContentsUseCase,
            recentSearchQueriesUseCase = getRecentQueryUseCase,
            searchContentsRepository = searchContentsRepository,
            savedStateHandle = SavedStateHandle(),
            recentSearchRepository = recentSearchRepository,
            userDataRepository = userDataRepository,
            analyticsHelper = NoOpAnalyticsHelper(),
        )
        userDataRepository.setUserData(emptyUserData)
    }

    @Test
    fun stateIsInitiallyLoading() = runTest {
        assertEquals(Loading, viewModel.searchResultUiState.value)
    }

    @Test
    fun stateIsEmptyQuery_withEmptySearchQuery() = runTest {
        searchContentsRepository.addTopics(topicsTestData)
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

        viewModel.onSearchQueryChanged("")

        assertEquals(EmptyQuery, viewModel.searchResultUiState.value)
    }

    @Test
    fun emptyResultIsReturned_withNotMatchingQuery() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

        viewModel.onSearchQueryChanged("XXX")
        searchContentsRepository.addTopics(topicsTestData)

        val result = viewModel.searchResultUiState.value
        assertIs<SearchResultUiState.Success>(result)
    }

    @Test
    fun recentSearches_verifyUiStateIsSuccess() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.recentSearchQueriesUiState.collect() }
        viewModel.onSearchTriggered("kotlin")

        val result = viewModel.recentSearchQueriesUiState.value
        assertIs<Success>(result)
    }

    @Test
    fun searchNotReady_withNoFtsTableEntity() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

        viewModel.onSearchQueryChanged("")

        assertEquals(SearchNotReady, viewModel.searchResultUiState.value)
    }

    @Test
    fun emptySearchText_isNotAddedToRecentSearches() = runTest {
        viewModel.onSearchTriggered("")

        val recentSearchQueriesStream = getRecentQueryUseCase()
        val recentSearchQueries = recentSearchQueriesStream.first()
        val recentSearchQuery = recentSearchQueries.firstOrNull()

        assertNull(recentSearchQuery)
    }

    @Test
    fun searchTextWithThreeSpaces_isEmptyQuery() = runTest {
        searchContentsRepository.addTopics(topicsTestData)
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

        viewModel.onSearchQueryChanged("   ")

        assertIs<EmptyQuery>(viewModel.searchResultUiState.value)

        collectJob.cancel()
    }

    @Test
    fun searchTextWithThreeSpacesAndOneLetter_isEmptyQuery() = runTest {
        searchContentsRepository.addTopics(topicsTestData)
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

        viewModel.onSearchQueryChanged("   a")

        assertIs<EmptyQuery>(viewModel.searchResultUiState.value)

        collectJob.cancel()
    }
}
