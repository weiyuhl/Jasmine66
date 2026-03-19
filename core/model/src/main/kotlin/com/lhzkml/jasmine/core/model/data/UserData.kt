
package com.lhzkml.jasmine.core.model.data

/**
 * Class summarizing user interest data
 */
data class UserData(
    val followedTopics: Set<String>,
    val themeBrand: ThemeBrand,
    val darkThemeConfig: DarkThemeConfig,
    val useDynamicColor: Boolean,
    val shouldHideOnboarding: Boolean,
)
