/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lhzkml.jasmine.catalog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lhzkml.jasmine.core.designsystem.component.JasmineButton
import com.lhzkml.jasmine.core.designsystem.component.JasmineFilterChip
import com.lhzkml.jasmine.core.designsystem.component.JasmineIconToggleButton
import com.lhzkml.jasmine.core.designsystem.component.JasmineNavigationBar
import com.lhzkml.jasmine.core.designsystem.component.JasmineNavigationBarItem
import com.lhzkml.jasmine.core.designsystem.component.JasmineOutlinedButton
import com.lhzkml.jasmine.core.designsystem.component.JasmineTab
import com.lhzkml.jasmine.core.designsystem.component.JasmineTabRow
import com.lhzkml.jasmine.core.designsystem.component.JasmineTextButton
import com.lhzkml.jasmine.core.designsystem.component.JasmineTopicTag
import com.lhzkml.jasmine.core.designsystem.component.JasmineViewToggleButton
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import com.lhzkml.jasmine.core.designsystem.theme.JasmineTheme

/**
 * Jasmine component catalog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JasmineCatalog() {
    JasmineTheme {
        Surface {
            val contentPadding = WindowInsets
                .systemBars
                .add(WindowInsets(left = 16.dp, top = 16.dp, right = 16.dp, bottom = 16.dp))
                .asPaddingValues()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = "Jasmine Catalog",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                item { Text("Buttons", Modifier.padding(top = 16.dp)) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        JasmineButton(onClick = {}) {
                            Text(text = "Enabled")
                        }
                        JasmineOutlinedButton(onClick = {}) {
                            Text(text = "Enabled")
                        }
                        JasmineTextButton(onClick = {}) {
                            Text(text = "Enabled")
                        }
                    }
                }
                item { Text("Disabled buttons", Modifier.padding(top = 16.dp)) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        JasmineButton(
                            onClick = {},
                            enabled = false,
                        ) {
                            Text(text = "Disabled")
                        }
                        JasmineOutlinedButton(
                            onClick = {},
                            enabled = false,
                        ) {
                            Text(text = "Disabled")
                        }
                        JasmineTextButton(
                            onClick = {},
                            enabled = false,
                        ) {
                            Text(text = "Disabled")
                        }
                    }
                }
                item { Text("Buttons with leading icons", Modifier.padding(top = 16.dp)) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        JasmineButton(
                            onClick = {},
                            text = { Text(text = "Enabled") },
                            leadingIcon = {
                                Icon(imageVector = JasmineIcons.Add, contentDescription = null)
                            },
                        )
                        JasmineOutlinedButton(
                            onClick = {},
                            text = { Text(text = "Enabled") },
                            leadingIcon = {
                                Icon(imageVector = JasmineIcons.Add, contentDescription = null)
                            },
                        )
                        JasmineTextButton(
                            onClick = {},
                            text = { Text(text = "Enabled") },
                            leadingIcon = {
                                Icon(imageVector = JasmineIcons.Add, contentDescription = null)
                            },
                        )
                    }
                }
                item { Text("Disabled buttons with leading icons", Modifier.padding(top = 16.dp)) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        JasmineButton(
                            onClick = {},
                            enabled = false,
                            text = { Text(text = "Disabled") },
                            leadingIcon = {
                                Icon(imageVector = JasmineIcons.Add, contentDescription = null)
                            },
                        )
                        JasmineOutlinedButton(
                            onClick = {},
                            enabled = false,
                            text = { Text(text = "Disabled") },
                            leadingIcon = {
                                Icon(imageVector = JasmineIcons.Add, contentDescription = null)
                            },
                        )
                        JasmineTextButton(
                            onClick = {},
                            enabled = false,
                            text = { Text(text = "Disabled") },
                            leadingIcon = {
                                Icon(imageVector = JasmineIcons.Add, contentDescription = null)
                            },
                        )
                    }
                }
                item { Text("Dropdown menus", Modifier.padding(top = 16.dp)) }
                item { Text("Chips", Modifier.padding(top = 16.dp)) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        var firstChecked by rememberSaveable { mutableStateOf(false) }
                        JasmineFilterChip(
                            selected = firstChecked,
                            onSelectedChange = { checked -> firstChecked = checked },
                            label = { Text(text = "Enabled") },
                        )
                        var secondChecked by rememberSaveable { mutableStateOf(true) }
                        JasmineFilterChip(
                            selected = secondChecked,
                            onSelectedChange = { checked -> secondChecked = checked },
                            label = { Text(text = "Enabled") },
                        )
                        JasmineFilterChip(
                            selected = false,
                            onSelectedChange = {},
                            enabled = false,
                            label = { Text(text = "Disabled") },
                        )
                        JasmineFilterChip(
                            selected = true,
                            onSelectedChange = {},
                            enabled = false,
                            label = { Text(text = "Disabled") },
                        )
                    }
                }
                item { Text("Icon buttons", Modifier.padding(top = 16.dp)) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        var firstChecked by rememberSaveable { mutableStateOf(false) }
                        JasmineIconToggleButton(
                            checked = firstChecked,
                            onCheckedChange = { checked -> firstChecked = checked },
                            icon = {
                                Icon(
                                    imageVector = JasmineIcons.BookmarkBorder,
                                    contentDescription = null,
                                )
                            },
                            checkedIcon = {
                                Icon(
                                    imageVector = JasmineIcons.Bookmark,
                                    contentDescription = null,
                                )
                            },
                        )
                        var secondChecked by rememberSaveable { mutableStateOf(true) }
                        JasmineIconToggleButton(
                            checked = secondChecked,
                            onCheckedChange = { checked -> secondChecked = checked },
                            icon = {
                                Icon(
                                    imageVector = JasmineIcons.BookmarkBorder,
                                    contentDescription = null,
                                )
                            },
                            checkedIcon = {
                                Icon(
                                    imageVector = JasmineIcons.Bookmark,
                                    contentDescription = null,
                                )
                            },
                        )
                        JasmineIconToggleButton(
                            checked = false,
                            onCheckedChange = {},
                            icon = {
                                Icon(
                                    imageVector = JasmineIcons.BookmarkBorder,
                                    contentDescription = null,
                                )
                            },
                            checkedIcon = {
                                Icon(
                                    imageVector = JasmineIcons.Bookmark,
                                    contentDescription = null,
                                )
                            },
                            enabled = false,
                        )
                        JasmineIconToggleButton(
                            checked = true,
                            onCheckedChange = {},
                            icon = {
                                Icon(
                                    imageVector = JasmineIcons.BookmarkBorder,
                                    contentDescription = null,
                                )
                            },
                            checkedIcon = {
                                Icon(
                                    imageVector = JasmineIcons.Bookmark,
                                    contentDescription = null,
                                )
                            },
                            enabled = false,
                        )
                    }
                }
                item { Text("View toggle", Modifier.padding(top = 16.dp)) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        var firstExpanded by rememberSaveable { mutableStateOf(false) }
                        JasmineViewToggleButton(
                            expanded = firstExpanded,
                            onExpandedChange = { expanded -> firstExpanded = expanded },
                            compactText = { Text(text = "Compact view") },
                            expandedText = { Text(text = "Expanded view") },
                        )
                        var secondExpanded by rememberSaveable { mutableStateOf(true) }
                        JasmineViewToggleButton(
                            expanded = secondExpanded,
                            onExpandedChange = { expanded -> secondExpanded = expanded },
                            compactText = { Text(text = "Compact view") },
                            expandedText = { Text(text = "Expanded view") },
                        )
                        JasmineViewToggleButton(
                            expanded = false,
                            onExpandedChange = {},
                            compactText = { Text(text = "Disabled") },
                            expandedText = { Text(text = "Disabled") },
                            enabled = false,
                        )
                    }
                }
                item { Text("Tags", Modifier.padding(top = 16.dp)) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        JasmineTopicTag(
                            followed = true,
                            onClick = {},
                            text = { Text(text = "Topic 1".uppercase()) },
                        )
                        JasmineTopicTag(
                            followed = false,
                            onClick = {},
                            text = { Text(text = "Topic 2".uppercase()) },
                        )
                        JasmineTopicTag(
                            followed = false,
                            onClick = {},
                            text = { Text(text = "Disabled".uppercase()) },
                            enabled = false,
                        )
                    }
                }
                item { Text("Tabs", Modifier.padding(top = 16.dp)) }
                item {
                    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
                    val titles = listOf("Topics", "People")
                    JasmineTabRow(selectedTabIndex = selectedTabIndex) {
                        titles.forEachIndexed { index, title ->
                            JasmineTab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(text = title) },
                            )
                        }
                    }
                }
                item { Text("Navigation", Modifier.padding(top = 16.dp)) }
                item {
                    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
                    val items = listOf("For you", "Saved", "Interests")
                    val icons = listOf(
                        JasmineIcons.UpcomingBorder,
                        JasmineIcons.BookmarksBorder,
                        JasmineIcons.Grid3x3,
                    )
                    val selectedIcons = listOf(
                        JasmineIcons.Upcoming,
                        JasmineIcons.Bookmarks,
                        JasmineIcons.Grid3x3,
                    )
                    JasmineNavigationBar {
                        items.forEachIndexed { index, item ->
                            JasmineNavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = icons[index],
                                        contentDescription = item,
                                    )
                                },
                                selectedIcon = {
                                    Icon(
                                        imageVector = selectedIcons[index],
                                        contentDescription = item,
                                    )
                                },
                                label = { Text(item) },
                                selected = selectedItem == index,
                                onClick = { selectedItem = index },
                            )
                        }
                    }
                }
            }
        }
    }
}

