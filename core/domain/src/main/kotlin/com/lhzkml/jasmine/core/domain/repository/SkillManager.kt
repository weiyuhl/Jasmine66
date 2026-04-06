package com.lhzkml.jasmine.core.domain.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.lhzkml.jasmine.core.model.Skill
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SkillManager"

/**
 * Skill 管理器 - 管理应用的 Skills
 */
@Singleton
class SkillManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val skills = mutableListOf<Skill>()
    private var isLoaded = false

    /**
     * 加载所有可用的 Skills
     */
    suspend fun loadSkills(): List<Skill> = withContext(Dispatchers.IO) {
        if (isLoaded) {
            return@withContext skills.toList()
        }

        val loadedSkills = mutableListOf<Skill>()
        val assetManager = context.assets
        
        try {
            // 从 assets 加载内置 Skills
            val skillAssetDirs = assetManager.list("skills") ?: emptyArray()
            for (dirName in skillAssetDirs) {
                val skillMdPath = "skills/$dirName/SKILL.md"
                try {
                    assetManager.open(skillMdPath).use { inputStream ->
                        val mdContent = inputStream.bufferedReader().use { it.readText() }
                        val (skillProto, errors) = convertSkillMdToProto(
                            mdContent,
                            builtIn = true,
                            selected = true,
                            importDir = "skills/$dirName",
                        )
                        if (errors.isEmpty() && skillProto != null) {
                            loadedSkills.add(skillProto)
                            Log.d(TAG, "已加载内置技能：${skillProto.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "读取内置技能失败 $dirName", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "列出技能目录失败", e)
        }

        skills.clear()
        skills.addAll(loadedSkills)
        isLoaded = true
        
        return@withContext skills.toList()
    }

    /**
     * 获取所有可用的 Skills
     */
    fun getAllSkills(): List<Skill> {
        return skills.toList()
    }

    /**
     * 根据名称获取 Skill
     */
    fun getSkillByName(name: String): Skill? {
        return skills.find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * 获取所有已激活的 Skills
     */
    fun getSelectedSkills(): List<Skill> {
        return skills.filter { it.selected }
    }

    /**
     * 获取已激活 Skills 的 instructions
     */
    fun getSelectedSkillsInstructions(): String {
        val selectedSkills = getSelectedSkills()
        if (selectedSkills.isEmpty()) {
            return "No active skills."
        }
        
        return selectedSkills.joinToString("\n\n") { skill ->
            """
            |=== Skill: ${skill.name} ===
            |Description: ${skill.description}
            |Instructions:
            |${skill.instructions}
            """.trimMargin()
        }
    }

    /**
     * 更新 Skill 的选择状态
     */
    fun updateSkillSelection(name: String, selected: Boolean): Boolean {
        val index = skills.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index >= 0) {
            skills[index] = skills[index].copy(selected = selected)
            Log.d(TAG, "技能 '${skills[index].name}' 已${if (selected) "启用" else "禁用"}")
            return true
        }
        return false
    }

    /**
     * 解析 Skill Markdown 内容
     */
    private fun convertSkillMdToProto(
        mdContent: String,
        builtIn: Boolean,
        selected: Boolean,
        skillUrl: String = "",
        importDir: String = "",
    ): Pair<Skill?, List<String>> {
        val parts = mdContent.split("---")
        val errors = mutableListOf<String>()

        if (parts.size < 3) {
            errors.add("格式错误：需要至少两个 '---' 分隔符")
            return Pair(null, errors)
        }

        val header = parts[1].trim()
        var name: String? = null
        var description: String? = null
        var requireSecret = false
        var requireSecretDescription = ""
        var homepage: String? = null

        var startMetadata = false
        for (line in header.lines()) {
            val trimmedLine = line.trim()
            if (trimmedLine == "metadata:") {
                startMetadata = true
                continue
            }
            if (!startMetadata) {
                when {
                    trimmedLine.startsWith("name:") -> name = trimmedLine.substringAfter("name:").trim()
                    trimmedLine.startsWith("description:") ->
                        description = trimmedLine.substringAfter("description:").trim()
                }
            } else {
                when {
                    trimmedLine.startsWith("require-secret:") ->
                        requireSecret = trimmedLine.substringAfter("require-secret:").trim().toBoolean()
                    trimmedLine.startsWith("require-secret-description:") ->
                        requireSecretDescription = trimmedLine.substringAfter("require-secret-description:").trim()
                    trimmedLine.startsWith("homepage:") ->
                        homepage = trimmedLine.substringAfter("homepage:").trim()
                }
            }
        }

        if (name.isNullOrEmpty()) {
            errors.add("头部缺少 'name' 字段")
        }
        if (description.isNullOrEmpty()) {
            errors.add("头部缺少 'description' 字段")
        }

        val instructions = parts.drop(2).joinToString("---").trim()

        if (errors.isNotEmpty()) {
            return Pair(null, errors)
        }

        val skill = Skill(
            name = name!!,
            description = description!!,
            instructions = instructions,
            builtIn = builtIn,
            selected = selected,
            skillUrl = skillUrl,
            requireSecret = requireSecret,
            requireSecretDescription = requireSecretDescription,
            homepage = homepage ?: "",
            importDirName = importDir,
        )

        return Pair(skill, emptyList())
    }
}
