/*
 * Copyright 2024 The Android Open Source Project
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

package com.lhzkml.jasmine.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhzkml.jasmine.core.designsystem.component.DynamicAsyncImage
import com.lhzkml.jasmine.core.designsystem.component.JasmineIconToggleButton
import com.lhzkml.jasmine.core.designsystem.icon.JasmineIcons
import com.lhzkml.jasmine.core.designsystem.theme.JasmineTheme
import com.lhzkml.jasmine.core.ui.R.string

@Composable
fun InterestsItem(
    name: String,
    following: Boolean,
    topicImageUrl: String,
    onClick: () -> Unit,
    onFollowButtonClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    description: String = "",
    isSelected: Boolean = false,
) {
    ListItem(
        leadingContent = {
            InterestsIcon(topicImageUrl, iconModifier.size(48.dp))
        },
        headlineContent = {
            Text(text = name)
        },
        supportingContent = {
            Text(text = description)
        },
        trailingContent = {
            JasmineIconToggleButton(
                checked = following,
                onCheckedChange = onFollowButtonClick,
                icon = {
                    Icon(
                        imageVector = JasmineIcons.Add,
                        contentDescription = stringResource(
                            id = string.core_ui_interests_card_follow_button_content_desc,
                        ),
                    )
                },
                checkedIcon = {
                    Icon(
                        imageVector = JasmineIcons.Check,
                        contentDescription = stringResource(
                            id = string.core_ui_interests_card_unfollow_button_content_desc,
                        ),
                    )
                },
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.Transparent
            },
        ),
        modifier = modifier
            .semantics(mergeDescendants = true) {
                selected = isSelected
            }
            .clickable(enabled = true, onClick = onClick),
    )
}

@Composable
private fun InterestsIcon(topicImageUrl: String, modifier: Modifier = Modifier) {
    if (topicImageUrl.isEmpty()) {
        Icon(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp),
            imageVector = JasmineIcons.Person,
            // decorative image
            contentDescription = null,
        )
    } else {
        DynamicAsyncImage(
            imageUrl = topicImageUrl,
            contentDescription = null,
            modifier = modifier,
        )
    }
}

@Preview
@Composable
private fun InterestsCardPreview() {
    JasmineTheme {
        Surface {
            InterestsItem(
                name = "Compose",
                description = "Description",
                following = false,
                topicImageUrl = "",
                onClick = { },
                onFollowButtonClick = { },
            )
        }
    }
}

@Preview
@Composable
private fun InterestsCardLongNamePreview() {
    JasmineTheme {
        Surface {
            InterestsItem(
                name = "This is a very very very very long name",
                description = "Description",
                following = true,
                topicImageUrl = "",
                onClick = { },
                onFollowButtonClick = { },
            )
        }
    }
}

@Preview
@Composable
private fun InterestsCardLongDescriptionPreview() {
    JasmineTheme {
        Surface {
            InterestsItem(
                name = "Compose",
                description = "This is a very very very very very very very " +
                    "very very very long description",
                following = false,
                topicImageUrl = "",
                onClick = { },
                onFollowButtonClick = { },
            )
        }
    }
}

@Preview
@Composable
private fun InterestsCardWithEmptyDescriptionPreview() {
    JasmineTheme {
        Surface {
            InterestsItem(
                name = "Compose",
                description = "",
                following = true,
                topicImageUrl = "",
                onClick = { },
                onFollowButtonClick = { },
            )
        }
    }
}

@Preview
@Composable
private fun InterestsCardSelectedPreview() {
    JasmineTheme {
        Surface {
            InterestsItem(
                name = "Compose",
                description = "",
                following = true,
                topicImageUrl = "",
                onClick = { },
                onFollowButtonClick = { },
                isSelected = true,
            )
        }
    }
}
