package com.lhzkml.jasmine.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.ChatNavKey
import com.lhzkml.jasmine.core.navigation.ToolsNavKey
import com.lhzkml.jasmine.core.navigation.KnowledgeBaseNavKey
import com.lhzkml.jasmine.core.ui.TrackScreenViewEvent

fun EntryProviderScope<NavKey>.stubEntries() {
    entry<ChatNavKey> { _ ->
        ChatStub()
    }
    entry<ToolsNavKey> { _ ->
        ToolsStub()
    }
    entry<KnowledgeBaseNavKey> { _ ->
        KnowledgeBaseStub()
    }
}

@Composable
private fun ChatStub() {
    Box(Modifier.fillMaxSize())
    TrackScreenViewEvent(screenName = "Chat")
}

@Composable
private fun ToolsStub() {
    Box(Modifier.fillMaxSize())
    TrackScreenViewEvent(screenName = "Tools")
}

@Composable
private fun KnowledgeBaseStub() {
    Box(Modifier.fillMaxSize())
    TrackScreenViewEvent(screenName = "KnowledgeBase")
}
