package com.lhzkml.jasmine.feature.knowledgebase.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.feature.knowledgebase.api.navigation.KnowledgeBaseNavKey
import com.lhzkml.jasmine.feature.knowledgebase.impl.KnowledgeBaseScreen

fun EntryProviderScope<NavKey>.knowledgeBaseEntry(navigator: Navigator) {
    entry<KnowledgeBaseNavKey> {
        KnowledgeBaseScreen()
    }
}
