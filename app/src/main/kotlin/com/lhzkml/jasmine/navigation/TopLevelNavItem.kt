package com.lhzkml.jasmine.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import com.lhzkml.jasmine.feature.bookmarks.api.navigation.BookmarksNavKey
import com.lhzkml.jasmine.feature.foryou.api.navigation.ForYouNavKey
import com.lhzkml.jasmine.feature.interests.api.navigation.InterestsNavKey
import com.lhzkml.jasmine.feature.bookmarks.api.R as bookmarksR
import com.lhzkml.jasmine.feature.foryou.api.R as forYouR
import com.lhzkml.jasmine.feature.search.api.R as searchR

/**
 * Type for the top level navigation items in the application. Contains UI information about the
 * current route that is used in the top app bar and common navigation UI.
 *
 * @param selectedIcon The icon to be displayed in the navigation UI when this destination is
 * selected.
 * @param unselectedIcon The icon to be displayed in the navigation UI when this destination is
 * not selected.
 * @param iconTextId Text that to be displayed in the navigation UI.
 * @param titleTextId Text that is displayed on the top app bar.
 */
data class TopLevelNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
)

val FOR_YOU = TopLevelNavItem(
    selectedIcon = JasmineIcons.Upcoming,
    unselectedIcon = JasmineIcons.UpcomingBorder,
    iconTextId = forYouR.string.feature_foryou_api_title,
    titleTextId = R.string.app_name,
)

val BOOKMARKS = TopLevelNavItem(
    selectedIcon = JasmineIcons.Bookmarks,
    unselectedIcon = JasmineIcons.BookmarksBorder,
    iconTextId = bookmarksR.string.feature_bookmarks_api_title,
    titleTextId = bookmarksR.string.feature_bookmarks_api_title,
)

val INTERESTS = TopLevelNavItem(
    selectedIcon = JasmineIcons.Grid3x3,
    unselectedIcon = JasmineIcons.Grid3x3,
    iconTextId = searchR.string.feature_search_api_interests,
    titleTextId = searchR.string.feature_search_api_interests,
)

val TOP_LEVEL_NAV_ITEMS = mapOf(
    ForYouNavKey to FOR_YOU,
    BookmarksNavKey to BOOKMARKS,
    InterestsNavKey(null) to INTERESTS,
)

