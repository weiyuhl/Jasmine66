package com.lhzkml.jasmine.core.model.data

/**
 * Class summarizing user interest data
 */data class UserData(
    val darkThemeConfig: DarkThemeConfig,
    val shouldHideOnboarding: Boolean,
    val kaiUiEnabled: Boolean = true,
    val webSearchEnabled: Boolean = true,
)
