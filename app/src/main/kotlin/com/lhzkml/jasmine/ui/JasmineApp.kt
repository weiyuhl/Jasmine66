package com.lhzkml.jasmine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import com.lhzkml.jasmine.R
import com.lhzkml.jasmine.core.designsystem.component.JasmineNavigationSuiteScaffold
import com.lhzkml.jasmine.core.designsystem.component.TopAppBar
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import com.lhzkml.jasmine.feature.chat.api.navigation.ChatNavKey
import com.lhzkml.jasmine.feature.tools.api.navigation.ToolsNavKey
import com.lhzkml.jasmine.feature.knowledgebase.api.navigation.KnowledgeBaseNavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.core.navigation.SettingsNavKey
import com.lhzkml.jasmine.core.navigation.toEntries
import com.lhzkml.jasmine.ui.LocalSnackbarHostState
import com.lhzkml.jasmine.feature.chat.impl.navigation.chatEntry
import com.lhzkml.jasmine.feature.tools.impl.navigation.toolsEntry
import com.lhzkml.jasmine.feature.knowledgebase.impl.navigation.knowledgeBaseEntry
import com.lhzkml.jasmine.feature.search.api.navigation.SearchNavKey
import com.lhzkml.jasmine.feature.search.api.R as searchR
import com.lhzkml.jasmine.feature.search.impl.navigation.searchEntry
import com.lhzkml.jasmine.feature.settings.impl.settingsEntry
import com.lhzkml.jasmine.navigation.TOP_LEVEL_NAV_ITEMS
import kotlinx.coroutines.launch
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
    val isChatDestination = appState.navigationState.currentKey == ChatNavKey
    
    var chatPrompt by rememberSaveable { mutableStateOf("") }
    var isChatRunning by rememberSaveable { mutableStateOf(false) }

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
                            contentDescription = null,
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
                            contentDescription = null,
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
                                contentDescription = null,
                            )
                        },
                        selectedIcon = {
                            Icon(
                                imageVector = navItem.selectedIcon,
                                contentDescription = null,
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
            Scaffold(
                modifier = modifier.semantics {
                    testTagsAsResourceId = true
                },
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        snackbarHostState,
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.exclude(
                                WindowInsets.ime,
                            ),
                        ),
                    )
                },
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal,
                            ),
                        ),
                ) {
                    var shouldShowTopAppBar = false

                    if (appState.navigationState.currentKey in appState.navigationState.topLevelKeys) {
                        shouldShowTopAppBar = true

                        val destination = TOP_LEVEL_NAV_ITEMS[appState.navigationState.currentTopLevelKey]
                            ?: error("Top level nav item not found for ${appState.navigationState.currentTopLevelKey}")

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

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .consumeWindowInsets(
                                if (shouldShowTopAppBar) {
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                                } else {
                                    WindowInsets(0, 0, 0, 0)
                                },
                            ),
                    ) {
                        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

                        val entryProvider = entryProvider {
                            chatEntry(navigator)
                            toolsEntry(navigator)
                            knowledgeBaseEntry(navigator)
                            searchEntry(navigator)
                            settingsEntry(navigator)
                        }

                        NavDisplay(
                            entries = appState.navigationState.toEntries(entryProvider),
                            sceneStrategy = listDetailStrategy,
                            onBack = { navigator.goBack() },
                        )
                    }
                    
                    if (isChatDestination) {
                        ChatComposer(
                            value = chatPrompt,
                            enabled = true,
                            onValueChange = { chatPrompt = it },
                            onSendClick = {
                                isChatRunning = true
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    isChatRunning = false
                                    chatPrompt = ""
                                }
                            },
                            isRunning = isChatRunning
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    val context = LocalContext.current
    val jsonString = remember {
        context.assets.open("Coding_Slide.json").bufferedReader().use { it.readText() }
    }
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(jsonString)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            modifier = Modifier.size(240.dp),
            painter = rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever,
                speed = 0.6f
            ),
            contentDescription = null
        )
    }
}

@Composable
private fun ChatComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isRunning: Boolean
) {
    val composerShape = RoundedCornerShape(22.dp)
    val sendEnabled = enabled && value.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (isRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    text = "⏳",
                    style = TextStyle(
                        color = Color(0xFF10A37F),
                        fontSize = 12.sp
                    )
                )
                BasicText(
                    text = "正在生成回复...",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE8E8E8), composerShape)
                .background(Color.White, composerShape)
                .padding(start = 16.dp, top = 10.dp, end = 12.dp, bottom = 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                singleLine = false,
                maxLines = 3,
                textStyle = TextStyle(
                    color = Color(0xFF1F2937),
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(Color(0xFF111111)),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, end = 8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (value.isEmpty()) {
                            BasicText(
                                text = "询问任何问题",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                        .background(Color(0xFFF8F8F8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "+",
                        style = TextStyle(
                            fontSize = 20.sp,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                IconButton(
                    onClick = onSendClick,
                    enabled = sendEnabled,
                    modifier = Modifier
                        .size(34.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (sendEnabled) Color(0xFF111111) else Color(0xFFF3F4F6),
                        contentColor = if (sendEnabled) Color.White else Color(0xFF9CA3AF),
                    )
                ) {
                    BasicText(
                        text = "↑",
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
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
