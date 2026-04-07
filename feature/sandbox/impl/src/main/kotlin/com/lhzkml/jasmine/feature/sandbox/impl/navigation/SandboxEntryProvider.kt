package com.lhzkml.jasmine.feature.sandbox.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.feature.sandbox.api.navigation.SandboxNavKey
import com.lhzkml.jasmine.feature.sandbox.api.navigation.TerminalNavKey
import com.lhzkml.jasmine.feature.sandbox.impl.ui.SandboxScreen
import com.lhzkml.jasmine.feature.sandbox.impl.ui.TerminalScreen

fun EntryProviderScope<NavKey>.sandboxEntry(navigator: Navigator) {
    entry<SandboxNavKey> {
        SandboxScreen(onOpenTerminal = { navigator.navigate(TerminalNavKey) })
    }
    entry<TerminalNavKey> {
        TerminalScreen(onBack = { navigator.goBack() })
    }
}
