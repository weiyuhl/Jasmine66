package com.lhzkml.jasmine.core.domain.repository

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.lhzkml.jasmine.core.model.Skill
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SkillManager"
private const val PREFS_NAME = "jasmine_skills_encrypted"
private const val KEY_SELECTED = "selected_skills"
private const val KEY_SECRET_PREFIX = "skill_secret_"

/**
 * Skill 管理器 - 管理应用的 Skills
 * 使用 EncryptedSharedPreferences 保护技能 API 密钥
 */
@Singleton
class SkillManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedPrefs = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        // Migrate from old plaintext SharedPreferences
        val oldPrefs = context.getSharedPreferences("jasmine_skills", Context.MODE_PRIVATE)
        if (oldPrefs.all.isNotEmpty() && encryptedPrefs.all.isEmpty()) {
            val editor = encryptedPrefs.edit()
            for ((key, value) in oldPrefs.all) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is MutableSet<*> -> editor.putString(key, value.map { it.toString() }.joinToString(","))
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value as Long)
                    is Float -> editor.putFloat(key, value as Float)
                    is Boolean -> editor.putBoolean(key, value as Boolean)
                }
            }
            editor.apply()
            oldPrefs.edit().clear().apply()
            Log.d(TAG, "已将技能数据迁移到加密存储")
        }
        encryptedPrefs
    }

    private val skills = mutableListOf<Skill>()
    private var isLoaded = false
    private val mutex = Mutex()

    /**
     * 加载所有可用的 Skills
     * IO 操作在锁外执行，仅临界区（读写 skills / isLoaded）在锁内
     */
    suspend fun loadSkills(): List<Skill> {
        // Fast path: already loaded, return copy under lock
        mutex.withLock {
            if (isLoaded) {
                return skills.toList()
            }
        }

        // Slow path: do IO-heavy work outside the lock
        val loadedSkills = mutableListOf<Skill>()
        val assetManager = context.assets

        try {
            val skillAssetDirs = withContext(Dispatchers.IO) {
                assetManager.list("skills") ?: emptyArray()
            }
            for (dirName in skillAssetDirs) {
                val skillMdPath = "skills/$dirName/SKILL.md"
                try {
                    val mdContent = withContext(Dispatchers.IO) {
                        assetManager.open(skillMdPath).use { inputStream ->
                            inputStream.bufferedReader().use { it.readText() }
                        }
                    }
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
                } catch (e: Exception) {
                    Log.w(TAG, "读取内置技能失败 $dirName", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "列出技能目录失败", e)
        }

        // Critical section: apply loaded data, restore selections
        mutex.withLock {
            // Double-check: another coroutine might have loaded while we were doing IO
            if (isLoaded) {
                return skills.toList()
            }

            skills.clear()
            skills.addAll(loadedSkills)

            val selectedStr = prefs.getString(KEY_SELECTED, "")
            if (!selectedStr.isNullOrBlank()) {
                val selectedSet = selectedStr.split(",").toSet()
                for (i in skills.indices) {
                    skills[i] = skills[i].copy(selected = selectedSet.contains(skills[i].name))
                }
            }

            isLoaded = true
            return skills.toList()
        }
    }

    /**
     * 获取所有可用的 Skills（线程安全）
     */
    suspend fun getAllSkills(): List<Skill> = mutex.withLock {
        skills.toList()
    }

    /**
     * 根据名称获取 Skill（线程安全）
     */
    suspend fun getSkillByName(name: String): Skill? = mutex.withLock {
        skills.find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * 获取所有已激活的 Skills（线程安全）
     */
    suspend fun getSelectedSkills(): List<Skill> = mutex.withLock {
        skills.filter { it.selected }
    }

    /**
     * 获取已激活 Skills 的 instructions（线程安全）
     */
    suspend fun getSelectedSkillsInstructions(): String = mutex.withLock {
        val selectedSkills = skills.filter { it.selected }
        if (selectedSkills.isEmpty()) {
            return NO_ACTIVE_SKILLS
        }

        selectedSkills.joinToString("\n\n") { skill ->
            """
            |=== Skill: ${skill.name} ===
            |Description: ${skill.description}
            |Instructions:
            |${skill.instructions}
            """.trimMargin()
        }
    }

    /**
     * 更新 Skill 的选择状态（线程安全）
     */
    suspend fun updateSkillSelection(name: String, selected: Boolean): Boolean = mutex.withLock {
        val index = skills.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (index >= 0) {
            skills[index] = skills[index].copy(selected = selected)
            persistSelections()
            Log.d(TAG, "技能 '${skills[index].name}' 已${if (selected) "启用" else "禁用"}")
            return true
        }
        false
    }

    /**
     * 持久化当前技能选择状态（调用方必须持有 mutex）
     */
    private fun persistSelections() {
        val selected = skills.filter { it.selected }.joinToString(",") { it.name }
        prefs.edit().putString(KEY_SELECTED, selected).apply()
    }

    /**
     * 从 URL 下载并导入技能
     */
    suspend fun importSkillFromUrl(url: String): Result<Skill> = withContext(Dispatchers.IO) {
        try {
            val uri = java.net.URI(url)
            val scheme = uri.scheme?.lowercase()
            if (scheme !in ALLOWED_IMPORT_SCHEMES) {
                return@withContext Result.failure(
                    Exception("不允许的 URL 协议: $scheme，仅支持 HTTPS")
                )
            }
            val host = uri.host?.lowercase() ?: ""
            if (ALLOWED_IMPORT_HOSTS.none { host == it || host.endsWith(".$it") }) {
                return@withContext Result.failure(
                    Exception("不允许的域名: $host，仅支持 GitHub、GitLab 等可信来源")
                )
            }

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
            mutex.withLock {
                skills.add(skill)
                persistSelections()
            }
            Log.d(TAG, "已导入技能: ${skill.name}")
            Result.success(skill)
        } catch (e: Exception) {
            Log.e(TAG, "导入技能失败", e)
            Result.failure(e)
        }
    }

    /**
     * 保存技能的 API 密钥（加密存储）
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
     * 删除技能（线程安全）
     */
    suspend fun deleteSkill(name: String): Boolean {
        // Critical section: remove from list and persist
        val importDir = mutex.withLock {
            val index = skills.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            if (index < 0) return false
            val skill = skills[index]
            if (skill.builtIn) return false

            skills.removeAt(index)
            persistSelections()
            prefs.edit().remove(KEY_SECRET_PREFIX + name.lowercase()).apply()
            Log.d(TAG, "已删除技能: $name")
            skill.importDirName.takeIf { it.isNotBlank() }
        }

        // File IO outside the lock
        if (importDir != null) {
            try {
                File(importDir).deleteRecursively()
            } catch (_: Exception) { }
        }

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

    companion object {
        const val NO_ACTIVE_SKILLS = "No active skills."

        private val ALLOWED_IMPORT_SCHEMES = setOf("https")
        private val ALLOWED_IMPORT_HOSTS = setOf(
            "github.com",
            "gitlab.com",
            "raw.githubusercontent.com",
            "gitee.com",
        )
    }
}
