package com.lhzkml.jasmine.core.model

data class Skill(
    val name: String,
    val description: String,
    val instructions: String,
    val builtIn: Boolean = false,
    val selected: Boolean = true,
    val skillUrl: String = "",
    val requireSecret: Boolean = false,
    val requireSecretDescription: String = "",
    val homepage: String = "",
    val importDirName: String = "",
)

data class SkillState(
    val skill: Skill
)

data class SkillManagerUiState(
    val loading: Boolean = false,
    val skills: List<SkillState> = emptyList(),
    val validating: Boolean = false,
    val validationError: String? = null,
)
