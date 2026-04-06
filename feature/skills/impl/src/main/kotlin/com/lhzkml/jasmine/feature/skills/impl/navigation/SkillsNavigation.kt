package com.lhzkml.jasmine.feature.skills.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lhzkml.jasmine.core.navigation.Navigator
import com.lhzkml.jasmine.feature.skills.api.SkillsNavKey
import com.lhzkml.jasmine.feature.skills.impl.SkillManagerViewModel
import com.lhzkml.jasmine.feature.skills.impl.SkillsRoute

fun EntryProviderScope<NavKey>.skillsEntry(
    navigator: Navigator,
) {
    entry<SkillsNavKey> {
        SkillsRoute(
            onBackClick = { navigator.goBack() },
        )
    }
}
