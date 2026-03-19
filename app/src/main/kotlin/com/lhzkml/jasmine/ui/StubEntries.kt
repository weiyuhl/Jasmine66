package com.lhzkml.jasmine.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.ForYouNavKey
import com.lhzkml.jasmine.core.navigation.BookmarksNavKey
import com.lhzkml.jasmine.core.navigation.InterestsNavKey
import com.lhzkml.jasmine.core.ui.TrackScreenViewEvent

fun EntryProviderScope<NavKey>.stubEntries() {
    entry<ForYouNavKey> { _ ->
        ForYouStub()
    }
    entry<BookmarksNavKey> { _ ->
        BookmarksStub()
    }
    entry<InterestsNavKey> { _ ->
        InterestsStub()
    }
}

@Composable
private fun ForYouStub() {
    Box(Modifier.fillMaxSize())
    TrackScreenViewEvent(screenName = "ForYou")
}

@Composable
private fun BookmarksStub() {
    Box(Modifier.fillMaxSize())
    TrackScreenViewEvent(screenName = "Saved")
}

@Composable
private fun InterestsStub() {
    Box(Modifier.fillMaxSize())
    TrackScreenViewEvent(screenName = "Interests")
}
