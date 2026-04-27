package com.lhzkml.jasmine.feature.settings.impl

import com.lhzkml.jasmine.core.data.repository.ChatClientManager
import com.lhzkml.jasmine.core.data.repository.ChatProviderRepository
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig
import com.lhzkml.jasmine.core.model.data.UserData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        val testRepo = object : UserDataRepository {
            override val userData = MutableStateFlow(
                UserData(
                    darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                    shouldHideOnboarding = false,
                )
            )
            override suspend fun setDarkThemeConfig(config: DarkThemeConfig) {}
            override suspend fun setShouldHideOnboarding(shouldHide: Boolean) {}
            override suspend fun setUiEnabled(enabled: Boolean) {}
            override suspend fun setWebSearchEnabled(enabled: Boolean) {}
        }

        viewModel = SettingsViewModel(
            userDataRepository = testRepo,
            providerRepo = mock(ChatProviderRepository::class.java),
            clientManager = mock(ChatClientManager::class.java),
        )
    }

    @Test
    fun constructorParametersAreProperlyAssigned() = runTest(UnconfinedTestDispatcher()) {
        // Verify the ViewModel is created and settingsUiState is accessible
        val state = viewModel.settingsUiState
        assertEquals(SettingsUiState.Loading, state.value)
    }
}
