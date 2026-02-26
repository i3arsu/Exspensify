package com.example.exspensify.ui.statistics

import com.example.exspensify.domain.model.StatisticsType

sealed class StatisticsEvent {
    data class MonthChanged(val month: Int) : StatisticsEvent()
    data class YearChanged(val year: Int) : StatisticsEvent()
    data class TypeChanged(val type: StatisticsType) : StatisticsEvent()
    object Refresh : StatisticsEvent()
}