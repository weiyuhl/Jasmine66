package com.lhzkml.jasmine.core.domain.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.lhzkml.jasmine.core.model.Skill
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SkillManager"
private const val PREFS_NAME = "jasmine_skills"
private const val KEY_SELECTED = "selected_skills"
private const val KEY_SECRET_PREFIX = "skill_secret_"

/**
 * Skill 管理器 - 管理应用的 Skills
 */
@Singleton
class SkillManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

        // Restore persisted selections
        val selectedSet = prefs.getStringSet(KEY_SELECTED, emptySet()) ?: emptySet()
        if (selectedSet.isNotEmpty()) {
            for (i in skills.indices) {
                skills[i] = skills[i].copy(selected = selectedSet.contains(skills[i].name))
            }
        }

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
            persistSelections()
            Log.d(TAG, "技能 '${skills[index].name}' 已${if (selected) "启用" else "禁用"}")
            return true
        }
        return false
    }

    /**
     * 持久化当前技能选择状态到 SharedPreferences
     */
    private fun persistSelections() {
        val selected = skills.filter { it.selected }.map { it.name }.toSet()
        prefs.edit().putStringSet(KEY_SELECTED, selected).apply()
    }

    /**
     * 从 URL 下载并导入技能
     */
    suspend fun importSkillFromUrl(url: String): Result<Skill> = withContext(Dispatchers.IO) {
        try {
            val content = URL(url).readText()
            val (skillProto, errors) = convertSkillMdToProto(
                content, builtIn = false, selected = true, skillUrl = url
            )
            if (errors.isNotEmpty() || skillProto == null) {
                return@withContext Result.failure(Exception("解析失败: ${errors.joinToString()}"))
            }

            // Save to internal storage
            val importDir = File(context.filesDir, "skills/imported/${skillProto.name}")
            importDir.mkdirs()
            File(importDir, "SKILL.md").writeText(content)

            val skill = skillProto.copy(importDirName = importDir.absolutePath)
            skills.add(skill)
            persistSelections()
            Log.d(TAG, "已导入技能: ${skill.name}")
            Result.success(skill)
        } catch (e: Exception) {
            Log.e(TAG, "导入技能失败", e)
            Result.failure(e)
        }
    }

    /**
     * 保存技能的 API 密钥
     */
    fun saveSecret(skillName: String, secret: String) {
        prefs.edit().putString(KEY_SECRET_PREFIX + skillName.lowercase(), secret).apply()
        Log.d(TAG, "已保存密钥: $skillName")
    }

    /**
     * 获取技能的 API 密钥
     */
    fun getSecret(skillName: String): String {
        return prefs.getString(KEY_SECRET_PREFIX + skillName.lowercase(), "") ?: ""
    }

    /**
     * 删除技能
     */
    fun deleteSkill(name: String): Boolean {
        val index = skills.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index < 0) return false
        val skill = skills[index]
        if (skill.builtIn) return false

        skills.removeAt(index)
        persistSelections()
        prefs.edit().remove(KEY_SECRET_PREFIX + name.lowercase()).apply()

        // Delete from internal storage
        if (skill.importDirName.isNotBlank()) {
            try {
                File(skill.importDirName).deleteRecursively()
            } catch (_: Exception) { }
        }

        Log.d(TAG, "已删除技能: $name")
        return true
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
