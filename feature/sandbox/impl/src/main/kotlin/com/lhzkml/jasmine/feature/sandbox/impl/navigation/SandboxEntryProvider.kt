package com.lhzkml.jasmine.feature.sandbox.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.feature.sandbox.api.navigation.SandboxNavKey
import com.lhzkml.jasmine.feature.sandbox.impl.ui.SandboxScreen

fun EntryProviderScope<NavKey>.sandboxEntry(navigator: Navigator) {
    entry<SandboxNavKey> {
        SandboxScreen()
    }
}
