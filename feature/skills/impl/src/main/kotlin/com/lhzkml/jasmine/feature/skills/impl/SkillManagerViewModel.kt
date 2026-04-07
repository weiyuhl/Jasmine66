package com.lhzkml.jasmine.feature.skills.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.jasmine.core.common.network.Dispatcher
import com.lhzkml.jasmine.core.common.network.JasmineDispatchers
import com.lhzkml.jasmine.core.domain.repository.SkillManager
import com.lhzkml.jasmine.feature.skills.api.Skill
import com.lhzkml.jasmine.feature.skills.api.SkillManagerUiState
import com.lhzkml.jasmine.feature.skills.api.SkillState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "SkillManagerVM"

@HiltViewModel
class SkillManagerViewModel @Inject constructor(
    private val skillManager: SkillManager,
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
                
                // Directly fetch from the Singleton manager instead of reinventing local asset parsing
                val skills = skillManager.loadSkills()

                _uiState.update { currentState ->
                    currentState.copy(skills = skills.map { SkillState(skill = it) })
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
        // Sync the globally active instance immediately representing the model context
        skillManager.updateSkillSelection(skillState.skill.name, selected)
        
        // Update Local UI layout changes representing visually what happened
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
        val currentSkills = _uiState.value.skills
        
        // Feed the status to the agent pool uniformly
        currentSkills.forEach { 
            skillManager.updateSkillSelection(it.skill.name, selected) 
        }

        _uiState.update { currentState ->
            currentState.copy(
                skills = currentState.skills.map { skillState ->
                    SkillState(skill = skillState.skill.copy(selected = selected))
                }
            )
        }
    }

    fun getSelectedSkills(): List<Skill> {
        return skillManager.getSelectedSkills()
    }

    fun getSelectedSkillsNamesAndDescriptions(): String {
        return getSelectedSkills().joinToString("\n") { skill ->
            "- ${skill.name}: ${skill.description}"
        }
    }
}
