package com.lhzkml.jasmine.feature.skills.impl

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.common.network.Dispatcher
import com.lhzkml.jasmine.core.common.network.JasmineDispatchers
import com.lhzkml.jasmine.feature.skills.api.Skill
import com.lhzkml.jasmine.feature.skills.api.SkillManagerUiState
import com.lhzkml.jasmine.feature.skills.api.SkillState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject

private const val TAG = "SkillManagerVM"

@HiltViewModel
class SkillManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(JasmineDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SkillManagerUiState())
    val uiState = _uiState.asStateFlow()
    var skillLoaded = false

    fun loadSkills(onDone: () -> Unit = {}) {
        if (!skillLoaded) {
            setLoading(true)
            viewModelScope.launch(ioDispatcher) {
                Log.d(TAG, "加载技能列表...")

                val builtInSkills = mutableListOf<Skill>()
                try {
                    val skillAssetDirs = context.assets.list("skills") ?: emptyArray()
                    for (dirName in skillAssetDirs) {
                        val skillMdPath = "skills/$dirName/SKILL.md"
                        try {
                            context.assets.open(skillMdPath).use { inputStream ->
                                val mdContent = inputStream.bufferedReader().use { it.readText() }
                                val (skillProto, errors) = convertSkillMdToProto(
                                    mdContent,
                                    builtIn = true,
                                    selected = true,
                                    importDir = "skills/$dirName",
                                )
                                if (errors.isEmpty() && skillProto != null) {
                                    builtInSkills.add(skillProto)
                                    Log.d(TAG, "已添加内置技能: ${skillProto.name}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "读取内置技能失败 $dirName", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "列出技能目录失败", e)
                }

                _uiState.update { currentState ->
                    currentState.copy(skills = builtInSkills.map { SkillState(skill = it) })
                }

                setLoading(false)
                skillLoaded = true
                withContext(ioDispatcher) { onDone() }
            }
        } else {
            onDone()
        }
    }

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(loading = loading) }
    }

    fun setSkillSelected(skillState: SkillState, selected: Boolean) {
        val updatedSkill = skillState.skill.copy(selected = selected)
        val updatedSkills = _uiState.value.skills.map {
            if (it.skill.name == skillState.skill.name) {
                SkillState(skill = updatedSkill)
            } else {
                it
            }
        }
        _uiState.update { it.copy(skills = updatedSkills) }
    }

    fun setAllSkillsSelected(selected: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                skills = currentState.skills.map { skillState ->
                    SkillState(skill = skillState.skill.copy(selected = selected))
                }
            )
        }
    }

    fun getSelectedSkills(): List<Skill> {
        return _uiState.value.skills.filter { it.skill.selected }.map { it.skill }
    }

    fun getSelectedSkillsNamesAndDescriptions(): String {
        return getSelectedSkills().joinToString("\n") { skill ->
            "- ${skill.name}: ${skill.description}"
        }
    }

    fun convertSkillMdToProto(
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
                        requireSecretDescription =
                            trimmedLine.substringAfter("require-secret-description:").trim()
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
