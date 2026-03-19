package com.lhzkml.jasmine.feature.chat.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.feature.chat.api.navigation.ChatNavKey
import com.lhzkml.jasmine.feature.chat.impl.ChatScreen

fun EntryProviderScope<NavKey>.chatEntry(navigator: Navigator) {
    entry<ChatNavKey> {
        ChatScreen()
    }
}
