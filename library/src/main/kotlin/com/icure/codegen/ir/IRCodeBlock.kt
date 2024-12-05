package com.icure.codegen.ir

import kotlinx.serialization.Serializable

@Serializable
data class IRCodeBlock(
    val format: String,
    val parameters: List<IRNode> = emptyList()
) : IRNode {

    companion object {

        fun of(format: String, vararg parameters: IRNode): IRCodeBlock = IRCodeBlock(format, parameters.toList())

    }

}

