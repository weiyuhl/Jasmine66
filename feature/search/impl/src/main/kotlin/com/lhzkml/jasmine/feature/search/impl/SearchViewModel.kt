package com.lhzkml.jasmine.feature.search.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.analytics.JasmineAnalyticsHelper
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent.Param
import com.lhzkml.jasmine.core.data.repository.RecentSearchRepository
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import com.lhzkml.jasmine.core.domain.GetRecentSearchQueriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    recentSearchQueriesUseCase: GetRecentSearchQueriesUseCase,
    private val recentSearchRepository: RecentSearchRepository,
    private val userDataRepository: UserDataRepository,
    private val savedStateHandle: SavedStateHandle,
    private val JasmineAnalyticsHelper: JasmineAnalyticsHelper,
) : ViewModel() {

    val searchQuery = savedStateHandle.getStateFlow(key = SEARCH_QUERY, initialValue = "")

    val searchResultUiState: StateFlow<SearchResultUiState> =
        searchQuery.map { query ->
            if (query.trim().length < SEARCH_QUERY_MIN_LENGTH) {
                SearchResultUiState.EmptyQuery
            } else {
                SearchResultUiState.Success
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchResultUiState.Loading,
        )

    val recentSearchQueriesUiState: StateFlow<RecentSearchQueriesUiState> =
        recentSearchQueriesUseCase()
            .map { RecentSearchQueriesUiState.Success(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RecentSearchQueriesUiState.Loading,
            )

    fun onSearchQueryChanged(query: String) {
        savedStateHandle[SEARCH_QUERY] = query
    }

    fun onSearchTriggered(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            recentSearchRepository.insertOrReplaceRecentSearch(searchQuery = query)
        }
        JasmineAnalyticsHelper.logEventSearchTriggered(query)
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            recentSearchRepository.clearRecentSearches()
        }
    }
}

private fun JasmineAnalyticsHelper.logEventSearchTriggered(query: String) =
    logEvent(
        AnalyticsEvent(
            type = "search_query",
            extras = listOf(Param(key = "search_query", value = query)),
        ),
    )

/** Minimum length of a search query to be considered valid. */
private const val SEARCH_QUERY_MIN_LENGTH = 2

/** Key used to save and retrieve the search query from [SavedStateHandle]. */
private const val SEARCH_QUERY = "searchQuery"
