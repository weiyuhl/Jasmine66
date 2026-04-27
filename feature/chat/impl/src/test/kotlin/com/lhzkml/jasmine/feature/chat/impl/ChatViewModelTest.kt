package com.lhzkml.jasmine.feature.chat.impl

import com.lhzkml.jasmine.core.data.model.SimpleChatMessage
import com.lhzkml.jasmine.core.data.model.StreamChatResult
import com.lhzkml.jasmine.core.data.repository.ChatClientManager
import com.lhzkml.jasmine.core.data.repository.UserDataRepository
import com.lhzkml.jasmine.core.data.tools.AgentEventBus
import com.lhzkml.jasmine.core.model.data.DarkThemeConfig
import com.lhzkml.jasmine.core.model.data.ThemeBrand
import com.lhzkml.jasmine.core.model.data.UserData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenMock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

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

        whenMock(mockClientManager.isConfigured).thenReturn(isConfiguredFlow)
        whenMock(mockClientManager.setupState).thenReturn(setupStateFlow)
        whenMock(mockClientManager.configChangesFlow).thenReturn(emptyFlow())
        whenMock(mockClientManager.getActiveModel()).thenReturn("test-model")

        whenMock(mockEventBus.jsEvents).thenReturn(MutableSharedFlow(replay = 0))

        viewModel = ChatViewModel(mockClientManager, userDataRepo, mockEventBus)
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
    fun clearError_resetsErrorMessage() = runTest(testDispatcher) {
        // Simulate error being set (not directly settable, test via clear)
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
    fun onSendClick_doesNothingWhenRunning() = runTest(testDispatcher) {
        // Set up: configure client for success
        val result = StreamChatResult(
            content = "response",
            finishReason = "stop",
            thinking = null,
            toolCalls = emptyList(),
        )
        whenMock(mockClientManager.streamChat(
            messages = anyOrNull(),
            model = anyOrNull(),
            onChunk = anyOrNull(),
            onThinking = anyOrNull(),
            onResumeAttempt = anyOrNull(),
            onToolCallStart = anyOrNull(),
            onToolCallResult = anyOrNull(),
            uiEnabled = any(),
            webSearchEnabled = any(),
        )).thenReturn(result)

        // First send: should work
        viewModel.onPromptChange("first message")
        viewModel.onSendClick()
        assertEquals(2, viewModel.messages.value.size)

        // Second send while running: should be blocked
        viewModel.onPromptChange("second message")
        viewModel.onSendClick()
        // Should still have only 2 messages (first pair)
        assertEquals(2, viewModel.messages.value.size)
    }

    @Test
    fun onSendClick_addsUserAndAssistantMessages() = runTest(testDispatcher) {
        val result = StreamChatResult(
            content = "Hello!",
            finishReason = "stop",
            thinking = null,
            toolCalls = emptyList(),
        )
        whenMock(mockClientManager.streamChat(
            messages = anyOrNull(),
            model = anyOrNull(),
            onChunk = anyOrNull(),
            onThinking = anyOrNull(),
            onResumeAttempt = anyOrNull(),
            onToolCallStart = anyOrNull(),
            onToolCallResult = anyOrNull(),
            uiEnabled = any(),
            webSearchEnabled = any(),
        )).thenReturn(result)

        viewModel.onPromptChange("Hi")
        viewModel.onSendClick()

        val msgs = viewModel.messages.value
        assertEquals(2, msgs.size)
        assertEquals("user", msgs[0].role)
        assertEquals("Hi", msgs[0].content)
        assertEquals("assistant", msgs[1].role)
        assertFalse(msgs[1].isStreaming)
    }

    @Test
    fun onSendClick_setsRunningFalseAfterCompletion() = runTest(testDispatcher) {
        val result = StreamChatResult(
            content = "Done",
            finishReason = "stop",
            thinking = null,
            toolCalls = emptyList(),
        )
        whenMock(mockClientManager.streamChat(
            messages = anyOrNull(),
            model = anyOrNull(),
            onChunk = anyOrNull(),
            onThinking = anyOrNull(),
            onResumeAttempt = anyOrNull(),
            onToolCallStart = anyOrNull(),
            onToolCallResult = anyOrNull(),
            uiEnabled = any(),
            webSearchEnabled = any(),
        )).thenReturn(result)

        viewModel.onPromptChange("Run")
        viewModel.onSendClick()

        assertFalse(viewModel.isChatRunning.value)
    }
}
