package com.phuocpham.pumiah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuocpham.pumiah.data.model.Goal
import com.phuocpham.pumiah.data.model.UiState
import com.phuocpham.pumiah.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _goals = MutableStateFlow<UiState<List<Goal>>>(UiState.Idle)
    val goals: StateFlow<UiState<List<Goal>>> = _goals

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState

    init { loadGoals() }

    fun loadGoals() {
        viewModelScope.launch {
            _goals.value = UiState.Loading
            goalRepository.getGoals()
                .onSuccess { _goals.value = UiState.Success(it) }
                .onFailure { _goals.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun createGoal(name: String, targetAmount: Double, targetDate: String) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            goalRepository.createGoal(name, targetAmount, targetDate)
                .onSuccess { _saveState.value = UiState.Success(Unit); loadGoals() }
                .onFailure { _saveState.value = UiState.Error(it.message ?: "Lỗi") }
        }
    }

    fun addContribution(goal: Goal, contribution: Double) {
        viewModelScope.launch {
            val newTotal = goal.currentAmount + contribution
            goalRepository.addContribution(goal.id, goal.currentAmount, contribution)
                .onSuccess {
                    if (newTotal >= goal.targetAmount) {
                        goalRepository.updateGoalStatus(goal.id, "achieved")
                    }
                    loadGoals()
                }
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            goalRepository.deleteGoal(id)
                .onSuccess { loadGoals() }
        }
    }

    fun abandonGoal(id: String) {
        viewModelScope.launch {
            goalRepository.updateGoalStatus(id, "abandoned")
                .onSuccess { loadGoals() }
        }
    }

    fun resetSaveState() { _saveState.value = UiState.Idle }
}
