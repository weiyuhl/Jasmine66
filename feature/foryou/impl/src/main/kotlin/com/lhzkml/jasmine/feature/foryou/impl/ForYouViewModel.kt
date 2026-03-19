
package com.lhzkml.jasmine.feature.foryou.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent
import com.lhzkml.jasmine.core.analytics.AnalyticsEvent.Param
import com.lhzkml.jasmine.core.analytics.JasmineAnalyticsHelper
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import com.lhzkml.jasmine.core.domain.GetFollowableTopicsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val JasmineAnalyticsHelper: JasmineAnalyticsHelper,
    private val userDataRepository: UserDataRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,
) : ViewModel() {

    private val shouldShowOnboarding: Flow<Boolean> =
        userDataRepository.userData.map { !it.shouldHideOnboarding }

    val onboardingUiState: StateFlow<OnboardingUiState> =
        kotlinx.coroutines.flow.MutableStateFlow(OnboardingUiState.NotShown)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = OnboardingUiState.NotShown,
            )

    fun updateTopicSelection(topicId: String, isChecked: Boolean) {
        viewModelScope.launch {
            userDataRepository.setTopicIdFollowed(topicId, isChecked)
        }
    }

    fun dismissOnboarding() {
        viewModelScope.launch {
            userDataRepository.setShouldHideOnboarding(true)
        }
    }
}


