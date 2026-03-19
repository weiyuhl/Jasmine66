
package com.lhzkml.jasmine.feature.interests.impl.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.feature.interests.api.navigation.InterestsNavKey
import com.lhzkml.jasmine.feature.interests.impl.InterestsDetailPlaceholder
import com.lhzkml.jasmine.feature.interests.impl.InterestsScreen
import com.lhzkml.jasmine.feature.interests.impl.InterestsViewModel
import com.lhzkml.jasmine.feature.topic.api.navigation.navigateToTopic

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.interestsEntry(navigator: Navigator) {
    entry<InterestsNavKey>(
        metadata = ListDetailSceneStrategy.listPane {
            InterestsDetailPlaceholder()
        },
    ) { key ->
        val viewModel = hiltViewModel<InterestsViewModel, InterestsViewModel.Factory> {
            it.create(key)
        }
        InterestsScreen(
            // TODO: This event should either be provided by the ViewModel or by the navigator, not both
            onTopicClick = navigator::navigateToTopic,

            // TODO: This should be dynamically calculated based on the rendering scene
            //  See https://github.com/android/nav3-recipes/commit/488f4811791ca3ed7192f4fe3c86e7371b32ebdc#diff-374e02026cdd2f68057dd940f203dc4ba7319930b33e9555c61af7e072211cabR89
            shouldHighlightSelectedTopic = false,
            viewModel = viewModel,
        )
    }
}
