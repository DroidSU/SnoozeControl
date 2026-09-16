package com.snoozecontrol.model

data class DismissState(
    val challengeType: ChallengeType = ChallengeType.NONE,
    val equation: String = "",
    val correctAnswer: Int = 0,
    val input: String = "",
    val error: String? = null,
    val targetBarcode: String? = null,
    val isScanning: Boolean = false
)
