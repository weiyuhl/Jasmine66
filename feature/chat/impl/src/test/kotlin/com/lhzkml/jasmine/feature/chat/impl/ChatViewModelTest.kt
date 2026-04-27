package com.lhzkml.jasmine.feature.chat.impl

import com.lhzkml.jasmine.core.data.model.StreamChatResult
import com.lhzkml.jasmine.core.data.repository.ChatClientManager
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import com.lhzkml.jasmine.core.data.tools.AgentEventBus
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig
import com.lhzkml.jasmine.core.model.data.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ChatViewModel
    private lateinit var mockClientManager: ChatClientManager
    private lateinit var mockEventBus: AgentEventBus
    private lateinit var userDataFlow: MutableStateFlow<UserData>
    private lateinit var isConfiguredFlow: MutableStateFlow<Boolean>
    private lateinit var setupStateFlow: MutableStateFlow<String>

    private val testUserData = UserData(
        darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
        shouldHideOnboarding = true,
        uiEnabled = true,
        webSearchEnabled = true,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        userDataFlow = MutableStateFlow(testUserData)
        isConfiguredFlow = MutableStateFlow(true)
        setupStateFlow = MutableStateFlow("")

        val userDataRepo = object : UserDataRepository {
            override val userData = userDataFlow
            override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {}
            override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) {}
            override suspend fun setUiEnabled(uiEnabled: Boolean) {}
            override suspend fun setWebSearchEnabled(webSearchEnabled: Boolean) {}
        }

        mockClientManager = mock(ChatClientManager::class.java)
        mockEventBus = mock(AgentEventBus::class.java)

        whenever(mockClientManager.isConfigured).thenReturn(isConfiguredFlow)
        whenever(mockClientManager.setupState).thenReturn(setupStateFlow)
        whenever(mockClientManager.configChangesFlow).thenReturn(emptyFlow())
        whenever(mockClientManager.getActiveModel()).thenReturn("test-model")
        whenever(mockEventBus.jsEvents).thenReturn(MutableSharedFlow(replay = 0))

        viewModel = ChatViewModel(mockClientManager, userDataRepo, mockEventBus)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isCorrect() {
        assertEquals("", viewModel.chatPrompt.value)
        assertFalse(viewModel.isChatRunning.value)
        assertNull(viewModel.errorMessage.value)
        assertTrue(viewModel.messages.value.isEmpty())
        assertTrue(viewModel.toolCallEvents.value.isEmpty())
    }

    @Test
    fun onPromptChange_updatesChatPrompt() {
        viewModel.onPromptChange("hello")
        assertEquals("hello", viewModel.chatPrompt.value)
    }

    @Test
    fun clearError_resetsErrorMessage() {
        viewModel.clearError()
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun clearToolCallEvents_resetsEvents() {
        viewModel.clearToolCallEvents()
        assertTrue(viewModel.toolCallEvents.value.isEmpty())
    }

    @Test
    fun onSendClick_doesNothingWhenBlank() {
        viewModel.onPromptChange("   ")
        viewModel.onSendClick()
        assertTrue(viewModel.messages.value.isEmpty())
    }

    @Test
    fun onSendClick_sequentialCallsBothSucceed() = runTest(testDispatcher) {
        val result = StreamChatResult("response", "stop", null, emptyList())
        whenever(mockClientManager.streamChat(
            messages = any(),
            model = any(),
            onChunk = any(),
            onThinking = any(),
            onResumeAttempt = any(),
            onToolCallStart = any(),
            onToolCallResult = any(),
            uiEnabled = any(),
            webSearchEnabled = any(),
        )).thenReturn(result)

        // First send
        viewModel.onPromptChange("first")
        viewModel.onSendClick()
        advanceUntilIdle()
        assertEquals(2, viewModel.messages.value.size)

        // Second send after first completes — should succeed (guard only blocks concurrent)
        viewModel.onPromptChange("second")
        viewModel.onSendClick()
        advanceUntilIdle()
        assertEquals(4, viewModel.messages.value.size)
    }

    @Test
    fun onSendClick_addsUserAndAssistantMessages() = runTest(testDispatcher) {
        val result = StreamChatResult("Hello!", "stop", null, emptyList())
        whenever(mockClientManager.streamChat(
            messages = any(),
            model = any(),
            onChunk = any(),
            onThinking = any(),
            onResumeAttempt = any(),
            onToolCallStart = any(),
            onToolCallResult = any(),
            uiEnabled = any(),
            webSearchEnabled = any(),
        )).thenReturn(result)

        viewModel.onPromptChange("Hi")
        viewModel.onSendClick()
        advanceUntilIdle()

        val msgs = viewModel.messages.value
        assertEquals(2, msgs.size)
        assertEquals("user", msgs[0].role)
        assertEquals("Hi", msgs[0].content)
        assertEquals("assistant", msgs[1].role)
        assertFalse(msgs[1].isStreaming)
    }

    @Test
    fun onSendClick_setsRunningFalseAfterCompletion() = runTest(testDispatcher) {
        val result = StreamChatResult("Done", "stop", null, emptyList())
        whenever(mockClientManager.streamChat(
            messages = any(),
            model = any(),
            onChunk = any(),
            onThinking = any(),
            onResumeAttempt = any(),
            onToolCallStart = any(),
            onToolCallResult = any(),
            uiEnabled = any(),
            webSearchEnabled = any(),
        )).thenReturn(result)

        viewModel.onPromptChange("Run")
        viewModel.onSendClick()
        advanceUntilIdle()

        assertFalse(viewModel.isChatRunning.value)
    }
}
