
package com.lhzkml.jasmine.feature.interests.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import com.lhzkml.jasmine.core.domain.GetFollowableTopicsUseCase
import com.lhzkml.jasmine.core.domain.TopicSortField
import com.lhzkml.jasmine.core.model.data.FollowableTopic
import com.lhzkml.jasmine.feature.interests.api.navigation.InterestsNavKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = InterestsViewModel.Factory::class)
class InterestsViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    val userDataRepository: UserDataRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,
    // TODO: see comment below
    @Assisted val key: InterestsNavKey,
) : ViewModel() {

    // TODO: this should no longer be necessary, the currently selected topic should be
    //  available through the navigation state
    // Key used to save and retrieve the currently selected topic id from saved state.
    private val selectedTopicIdKey = "selectedTopicIdKey"

    private val selectedTopicId = savedStateHandle.getStateFlow(
        key = selectedTopicIdKey,
        initialValue = key.initialTopicId,
    )

    val uiState: StateFlow<InterestsUiState> = kotlinx.coroutines.flow.MutableStateFlow(InterestsUiState.Empty)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InterestsUiState.Empty,
        )

    fun followTopic(followedTopicId: String, followed: Boolean) {
        viewModelScope.launch {
            userDataRepository.setTopicIdFollowed(followedTopicId, followed)
        }
    }

    fun onTopicClick(topicId: String?) {
        // TODO: This should modify the navigation state directly rather than just updating the
        //  savedStateHandle
        savedStateHandle[selectedTopicIdKey] = topicId
    }

    @AssistedFactory
    interface Factory {
        fun create(key: InterestsNavKey): InterestsViewModel
    }
}

sealed interface InterestsUiState {
    data object Loading : InterestsUiState

    data class Interests(
        val selectedTopicId: String?,
        val topics: List<FollowableTopic>,
    ) : InterestsUiState

    data object Empty : InterestsUiState
}

