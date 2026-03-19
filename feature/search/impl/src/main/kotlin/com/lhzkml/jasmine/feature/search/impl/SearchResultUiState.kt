package com.lhzkml.jasmine.feature.search.impl

sealed interface SearchResultUiState {
    data object Loading : SearchResultUiState
    data object EmptyQuery : SearchResultUiState
    data object LoadFailed : SearchResultUiState
    data object Success : SearchResultUiState
    data object SearchNotReady : SearchResultUiState
}
