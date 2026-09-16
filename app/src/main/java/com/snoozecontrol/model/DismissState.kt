package com.snoozecontrol.model

data class DismissState(
    val equation: String = "",
    val correctAnswer: Int = 0,
    val input: String = "",
    val error: String? = null
)
