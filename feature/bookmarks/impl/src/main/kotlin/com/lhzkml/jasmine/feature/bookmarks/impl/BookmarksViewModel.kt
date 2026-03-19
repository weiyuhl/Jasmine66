
package com.lhzkml.jasmine.feature.bookmarks.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    val uiState: StateFlow<BookmarksUiState> =
        kotlinx.coroutines.flow.flowOf(BookmarksUiState.Empty)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BookmarksUiState.Loading,
            )
}

sealed interface BookmarksUiState {
    data object Loading : BookmarksUiState
    data object Empty : BookmarksUiState
}
