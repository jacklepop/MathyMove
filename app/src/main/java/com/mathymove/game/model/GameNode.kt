package com.mathymove.game.model

import kotlinx.serialization.Serializable

@Serializable
enum class NodeType {
    NUMBER,
    OPERATOR
}

@Serializable
data class GameNode(
    val id: String,
    val x: Float,
    val y: Float,
    val value: String,
    val type: NodeType,
    val depth: Int,
    val visited: Boolean = false,
    val parentId: String? = null,
    val childrenIds: List<String> = emptyList(),
    val directionAngle: Float = 0f
)
