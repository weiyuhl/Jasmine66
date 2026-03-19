package com.lhzkml.jasmine.feature.tools.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.feature.tools.api.navigation.ToolsNavKey
import com.lhzkml.jasmine.feature.tools.impl.ToolsScreen

fun EntryProviderScope<NavKey>.toolsEntry(navigator: Navigator) {
    entry<ToolsNavKey> {
        ToolsScreen()
    }
}
