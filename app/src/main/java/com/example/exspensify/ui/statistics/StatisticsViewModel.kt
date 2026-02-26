package com.example.exspensify.ui.statistics

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exspensify.core.util.Resource
import com.example.exspensify.domain.model.StatisticsFilter
import com.example.exspensify.domain.model.StatisticsType
import com.example.exspensify.domain.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _uiState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        MutableStateFlow(StatisticsUiState())
    } else {
        TODO("VERSION.SDK_INT < O")
    }
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            is StatisticsEvent.MonthChanged -> {
                _uiState.update { 
                    it.copy(filter = it.filter.copy(month = event.month))
                }
                loadStatistics()
            }
            is StatisticsEvent.YearChanged -> {
                _uiState.update { 
                    it.copy(filter = it.filter.copy(year = event.year))
                }
                loadStatistics()
            }
            is StatisticsEvent.TypeChanged -> {
                _uiState.update { 
                    it.copy(filter = it.filter.copy(type = event.type))
                }
                loadStatistics()
            }
            StatisticsEvent.Refresh -> loadStatistics()
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val filter = _uiState.value.filter

            // Combine all statistics flows
            combine(
                statisticsRepository.getCategoryStatistics(filter),
                statisticsRepository.getDailyStatistics(filter),
                statisticsRepository.getMonthlyStatistics(filter)
            ) { categoryResult, dailyResult, monthlyResult ->
                Triple(categoryResult, dailyResult, monthlyResult)
            }.collect { (categoryResult, dailyResult, monthlyResult) ->
                
                var hasError = false
                var errorMessage: String? = null

                val categoryStats = when (categoryResult) {
                    is Resource.Success -> categoryResult.data ?: emptyList()
                    is Resource.Error -> {
                        hasError = true
                        errorMessage = categoryResult.message
                        emptyList()
                    }
                    else -> emptyList()
                }

                val dailyStats = when (dailyResult) {
                    is Resource.Success -> dailyResult.data ?: emptyList()
                    is Resource.Error -> {
                        hasError = true
                        errorMessage = dailyResult.message
                        emptyList()
                    }
                    else -> emptyList()
                }

                val monthlyStats = when (monthlyResult) {
                    is Resource.Success -> monthlyResult.data ?: emptyList()
                    is Resource.Error -> {
                        hasError = true
                        errorMessage = monthlyResult.message
                        emptyList()
                    }
                    else -> emptyList()
                }

                val totalAmount = categoryStats.sumOf { it.amount }

                _uiState.update {
                    it.copy(
                        categoryStatistics = categoryStats,
                        dailyStatistics = dailyStats,
                        monthlyStatistics = monthlyStats,
                        totalAmount = totalAmount,
                        isLoading = false,
                        error = if (hasError) errorMessage else null
                    )
                }
            }
        }
    }
}