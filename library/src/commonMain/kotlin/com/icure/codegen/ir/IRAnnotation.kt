package com.icure.codegen.ir

import com.icure.codegen.ir.annotation.IRValueArgument
import kotlinx.serialization.Serializable

@Serializable
data class
IRAnnotation(
	override val packageName: String,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	val arguments: List<IRValueArgument> = emptyList()
) : IREntity, IRAnnotationValue

