package com.snoozecontrol.model

enum class MathDifficulty(val displayName: String) {
    EASY("Easy"),
    GENIUS("Genius");

    companion object {
        fun fromString(value: String?): MathDifficulty {
            return when (value?.uppercase()) {
                "EASY" -> EASY
                "GENIUS", "MEDIUM", "HARD" -> GENIUS
                else -> EASY
            }
        }
    }
}
