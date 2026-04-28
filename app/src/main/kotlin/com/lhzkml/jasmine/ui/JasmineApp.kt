package com.lhzkml.jasmine.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.core.designsystem.component.JasmineNavigationSuiteScaffold
import com.lhzkml.jasmine.core.designsystem.component.TopAppBar
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.core.navigation.SettingsNavKey
import com.lhzkml.jasmine.core.navigation.toEntries
import com.lhzkml.jasmine.feature.chat.impl.navigation.chatEntry
import com.lhzkml.jasmine.feature.knowledgebase.impl.navigation.knowledgeBaseEntry
import com.lhzkml.jasmine.feature.search.api.navigation.SearchNavKey
import com.lhzkml.jasmine.feature.search.impl.navigation.searchEntry
import com.lhzkml.jasmine.feature.settings.impl.settingsEntry
import com.lhzkml.jasmine.feature.sandbox.impl.navigation.sandboxEntry
import com.lhzkml.jasmine.feature.skills.api.SkillsNavKey
import com.lhzkml.jasmine.feature.skills.impl.navigation.skillsEntry
import com.lhzkml.jasmine.feature.tools.impl.navigation.toolsEntry
import com.lhzkml.jasmine.navigation.TOP_LEVEL_NAV_ITEMS
import com.lhzkml.jasmine.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch
import com.lhzkml.jasmine.feature.search.api.R as searchR
import com.lhzkml.jasmine.feature.settings.impl.R as settingsR

@Composable
fun JasmineApp(
    appState: JasmineAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline by appState.isOffline.collectAsStateWithLifecycle()
    val notConnectedMessage = stringResource(R.string.not_connected)

    LaunchedEffect(isOffline) {
        if (isOffline) {
            snackbarHostState.showSnackbar(
                message = notConnectedMessage,
                duration = Indefinite,
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        JasmineAppContent(
            appState = appState,
            modifier = modifier,
            windowAdaptiveInfo = windowAdaptiveInfo,
        )
    }
}

@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalLayoutApi::class,
)
internal fun JasmineAppContent(
    appState: JasmineAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val unreadNavKeys by appState.topLevelNavKeysWithUnreadResources
        .collectAsStateWithLifecycle()

    val snackbarHostState = LocalSnackbarHostState.current
    val navigator = remember { Navigator(appState.navigationState) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val isTopLevelDestination = appState.navigationState.currentKey in appState.navigationState.topLevelKeys

    DismissibleNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTopLevelDestination,
        drawerContent = {
            DismissibleDrawerSheet {
                NavigationDrawerItem(
                    label = { Text(stringResource(searchR.string.feature_search_api_title)) },
                    selected = appState.navigationState.currentKey == SearchNavKey,
                    onClick = {
                        navigator.navigate(SearchNavKey)
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = {
                        Icon(
                            imageVector = JasmineIcons.Search,
                            contentDescription = stringResource(searchR.string.feature_search_api_title),
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                Spacer(Modifier.weight(1f))
                 NavigationDrawerItem(
                     label = { Text(stringResource(settingsR.string.feature_settings_impl_title)) },
                     selected = appState.navigationState.currentKey == SettingsNavKey,
                     onClick = {
                         navigator.navigate(SettingsNavKey)
                         coroutineScope.launch { drawerState.close() }
                     },
                     icon = {
                         Icon(
                             imageVector = JasmineIcons.Settings,
                             contentDescription = stringResource(settingsR.string.feature_settings_impl_title),
                         )
                     },
                     modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                 )
                 NavigationDrawerItem(
                     label = { Text(stringResource(R.string.skill_management)) },
                     selected = appState.navigationState.currentKey == SkillsNavKey,
                     onClick = {
                         navigator.navigate(SkillsNavKey)
                         coroutineScope.launch { drawerState.close() }
                     },
                     icon = {
                         Icon(
                             imageVector = JasmineIcons.Settings,
                             contentDescription = stringResource(R.string.skill_management),
                         )
                     },
                     modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                 )
            }
        },
    ) {
        val currentKey = appState.navigationState.currentKey
        val showBottomBar = currentKey in appState.navigationState.topLevelKeys

        JasmineNavigationSuiteScaffold(
            showNavigationSuite = showBottomBar,
            navigationSuiteItems = {
                TOP_LEVEL_NAV_ITEMS.forEach { (navKey, navItem) ->
                    val hasUnread = unreadNavKeys.contains(navKey)
                    val selected = navKey == appState.navigationState.currentTopLevelKey
                    item(
                        selected = selected,
                        onClick = { navigator.navigate(navKey) },
                        icon = {
                            Icon(
                                imageVector = navItem.unselectedIcon,
                                contentDescription = stringResource(navItem.iconTextId),
                            )
                        },
                        selectedIcon = {
                            Icon(
                                imageVector = navItem.selectedIcon,
                                contentDescription = stringResource(navItem.iconTextId),
                            )
                        },
                        label = { Text(stringResource(navItem.iconTextId)) },
                        modifier = Modifier
                            .testTag("NavItem")
                            .then(if (hasUnread) Modifier.notificationDot() else Modifier),
                    )
                }
            },
            windowAdaptiveInfo = windowAdaptiveInfo,
        ) {
            val currentKey = appState.navigationState.currentKey
            val shouldShowTopAppBar = currentKey in appState.navigationState.topLevelKeys

                Scaffold(
                    modifier = modifier.semantics {
                        testTagsAsResourceId = true
                    },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        if (shouldShowTopAppBar) {
                            val destination = TOP_LEVEL_NAV_ITEMS[appState.navigationState.currentTopLevelKey]
                            if (destination != null) {
                                TopAppBar(
                                    titleRes = destination.titleTextId,
                                    navigationIcon = JasmineIcons.Menu,
                                    navigationIconContentDescription = stringResource(
                                        id = settingsR.string.feature_settings_impl_top_app_bar_navigation_icon_description,
                                    ),
                                    actionIcon = null,
                                    actionIconContentDescription = null,
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                    ),
                                    onActionClick = { },
                                    onNavigationClick = {
                                        coroutineScope.launch {
                                            if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                        }
                                    },
                                )
                            }
                        }
                    },
                    snackbarHost = {
                    SnackbarHost(
                        snackbarHostState,
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                        ),
                    )
                },
            ) { innerPadding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing
                                .exclude(WindowInsets.ime)
                                .only(WindowInsetsSides.Horizontal),
                        ),
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

                        val entryProvider = entryProvider {
                            chatEntry(navigator)
                            toolsEntry(navigator)
                            knowledgeBaseEntry(navigator)
                             searchEntry(navigator)
                             settingsEntry(navigator)
                             sandboxEntry(navigator)
                             skillsEntry(navigator)
                        }

                        NavDisplay(
                            entries = appState.navigationState.toEntries(entryProvider),
                            sceneStrategy = listDetailStrategy,
                            onBack = { navigator.goBack() },
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.notificationDot(): Modifier =
    composed {
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        drawWithContent {
            drawContent()
            drawCircle(
                tertiaryColor,
                radius = 5.dp.toPx(),
                center = center + Offset(
                    64.dp.toPx() * .45f,
                    32.dp.toPx() * -.45f - 6.dp.toPx(),
                ),
            )
        }
    }
