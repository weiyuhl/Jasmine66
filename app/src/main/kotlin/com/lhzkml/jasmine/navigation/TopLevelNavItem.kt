package com.lhzkml.jasmine.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import com.lhzkml.jasmine.feature.chat.api.navigation.ChatNavKey
import com.lhzkml.jasmine.feature.tools.api.navigation.ToolsNavKey
import com.lhzkml.jasmine.feature.knowledgebase.api.navigation.KnowledgeBaseNavKey
import com.lhzkml.jasmine.core.ui.R as uiR

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

val CHAT = TopLevelNavItem(
    selectedIcon = JasmineIcons.Upcoming,
    unselectedIcon = JasmineIcons.UpcomingBorder,
    iconTextId = uiR.string.core_ui_for_you,
    titleTextId = uiR.string.core_ui_for_you,
)

val TOOLS = TopLevelNavItem(
    selectedIcon = JasmineIcons.Bookmarks,
    unselectedIcon = JasmineIcons.BookmarksBorder,
    iconTextId = uiR.string.core_ui_bookmarks,
    titleTextId = uiR.string.core_ui_bookmarks,
)

val KNOWLEDGE_BASE = TopLevelNavItem(
    selectedIcon = JasmineIcons.Grid3x3,
    unselectedIcon = JasmineIcons.Grid3x3,
    iconTextId = uiR.string.core_ui_interests,
    titleTextId = uiR.string.core_ui_interests,
)

val TOP_LEVEL_NAV_ITEMS = mapOf(
    ChatNavKey to CHAT,
    ToolsNavKey to TOOLS,
    KnowledgeBaseNavKey to KNOWLEDGE_BASE,
)
