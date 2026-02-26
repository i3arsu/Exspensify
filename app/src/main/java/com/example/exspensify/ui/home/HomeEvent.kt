package com.example.exspensify.ui.home

sealed class HomeEvent {
    object Refresh : HomeEvent()
    data class TransactionClick(val transactionId: String) : HomeEvent()
}