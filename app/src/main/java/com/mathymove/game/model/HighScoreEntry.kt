package com.mathymove.game.model

import kotlinx.serialization.Serializable

@Serializable
data class HighScoreEntry(
    val score: Int,
    val timestamp: Long,
    val formattedDateTime: String
)
