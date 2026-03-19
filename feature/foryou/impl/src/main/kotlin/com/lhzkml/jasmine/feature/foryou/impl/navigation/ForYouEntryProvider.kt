
package com.lhzkml.jasmine.feature.foryou.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.feature.foryou.api.navigation.ForYouNavKey
import com.lhzkml.jasmine.feature.foryou.impl.ForYouScreen
import com.lhzkml.jasmine.feature.topic.api.navigation.navigateToTopic

fun EntryProviderScope<NavKey>.forYouEntry(navigator: Navigator) {
    entry<ForYouNavKey> {
        ForYouScreen(
            onTopicClick = navigator::navigateToTopic,
        )
    }
}
