package com.lhzkml.jasmine.feature.search.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.core.navigation.InterestsNavKey
import com.lhzkml.jasmine.feature.search.api.navigation.SearchNavKey
import com.lhzkml.jasmine.feature.search.impl.SearchScreen

fun EntryProviderScope<NavKey>.searchEntry(navigator: Navigator) {
    entry<SearchNavKey> {
        SearchScreen(
            onBackClick = { navigator.goBack() },
            onTopicClick = { }, // Topics deleted
        )
    }
}
