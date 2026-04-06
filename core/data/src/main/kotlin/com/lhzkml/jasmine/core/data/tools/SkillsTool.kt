package com.lhzkml.jasmine.core.data.tools

import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.domain.repository.SkillManager
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Skills 工具 - 允许 Agent 管理和使用 Skills
 */
@Singleton
class SkillsTool @Inject constructor(
    private val skillManager: SkillManager
) : Tool() {

    companion object {
        const val TOOL_NAME = "manage_skills"
    }

    override val descriptor = ToolDescriptor(
        name = TOOL_NAME,
        description = "Manage and use skills. Can list available skills, load a skill to get its instructions, check if a skill is active, or enable/disable a skill.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                "action",
                "The action to perform: 'list', 'load', 'check', 'enable', 'disable'",
                ToolParameterType.StringType
            )
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                "skill_name",
                "The name of the skill (required for 'load', 'check', 'enable', 'disable' actions)",
                ToolParameterType.StringType
            )
        )
    )

    override suspend fun execute(arguments: String): String {
        val json = Json.parseToJsonElement(arguments).jsonObject
        val action = json["action"]?.jsonPrimitive?.content ?: ""
        val skillName = json["skill_name"]?.jsonPrimitive?.content?.trim() ?: ""

        // 确保 Skills 已加载
        skillManager.loadSkills()

        return when (action.lowercase()) {
            "list" -> listSkills()
            "load" -> loadSkill(skillName)
            "check" -> checkSkill(skillName)
            "enable" -> enableSkill(skillName)
            "disable" -> disableSkill(skillName)
            else -> "Error: Unknown action '$action'. Valid actions are: 'list', 'load', 'check', 'enable', 'disable'"
        }
    }

    private fun listSkills(): String {
        val skills = skillManager.getAllSkills()
        if (skills.isEmpty()) {
            return "No skills available. Skills can be added from the Skills settings page."
        }

        val skillList = skills.joinToString("\n") { skill ->
            val status = if (skill.selected) "✅" else "❌"
            "- $status **${skill.name}**: ${skill.description}"
        }

        return """
            |Available skills:
            |
            |$skillList
            |
            |Use action='load' with a skill_name to get detailed instructions for a specific skill.
            |Use action='enable' or action='disable' to activate/deactivate a skill.
        """.trimMargin()
    }

    private fun loadSkill(skillName: String): String {
        if (skillName.isBlank()) {
            return "Error: skill_name is required for 'load' action. Use action='list' to see available skills."
        }

        val skill = skillManager.getSkillByName(skillName)
        if (skill == null) {
            return "Error: Skill '$skillName' not found. Use action='list' to see available skills."
        }

        // 自动启用该 Skill
        skillManager.updateSkillSelection(skillName, true)

        return """
            |✅ Skill Loaded and Activated: ${skill.name}
            |
            |Description: ${skill.description}
            |
            |Instructions:
            |${skill.instructions}
            |
            |You should now follow these instructions when interacting with the user.
        """.trimMargin()
    }

    private fun checkSkill(skillName: String): String {
        if (skillName.isBlank()) {
            return "Error: skill_name is required for 'check' action."
        }

        val skill = skillManager.getSkillByName(skillName)
        val isActive = skill != null && skill.selected

        return if (isActive) {
            "✅ Yes, the skill '$skillName' is active and loaded."
        } else if (skill != null) {
            "⚠️ The skill '$skillName' exists but is not currently active. Use action='enable' to activate it, or action='load' to load and activate it."
        } else {
            "❌ Error: Skill '$skillName' not found. Use action='list' to see available skills."
        }
    }

    private fun enableSkill(skillName: String): String {
        if (skillName.isBlank()) {
            return "Error: skill_name is required for 'enable' action."
        }

        val skill = skillManager.getSkillByName(skillName)
        if (skill == null) {
            return "Error: Skill '$skillName' not found. Use action='list' to see available skills."
        }

        skillManager.updateSkillSelection(skillName, true)

        return "✅ Skill '$skillName' has been enabled. You can now use action='load' to get its instructions."
    }

    private fun disableSkill(skillName: String): String {
        if (skillName.isBlank()) {
            return "Error: skill_name is required for 'disable' action."
        }

        val skill = skillManager.getSkillByName(skillName)
        if (skill == null) {
            return "Error: Skill '$skillName' not found."
        }

        skillManager.updateSkillSelection(skillName, false)

        return "⚠️ Skill '$skillName' has been disabled."
    }
}
