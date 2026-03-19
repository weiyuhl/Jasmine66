
package com.lhzkml.jasmine.feature.bookmarks.impl.navigation

import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.feature.bookmarks.api.navigation.BookmarksNavKey
import com.lhzkml.jasmine.feature.bookmarks.impl.BookmarksScreen
import com.lhzkml.jasmine.feature.topic.api.navigation.navigateToTopic

fun EntryProviderScope<NavKey>.bookmarksEntry(navigator: Navigator) {
    entry<BookmarksNavKey> {
        val snackbarHostState = LocalSnackbarHostState.current
        BookmarksScreen(
            onTopicClick = navigator::navigateToTopic,
            onShowSnackbar = { message, action ->
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = action,
                    duration = Short,
                ) == ActionPerformed
            },
        )
    }
}

// TODO: Why is this here?
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState state should be initialized at runtime")
}
