package com.icure.codegen.ir.annotation

import com.icure.codegen.ir.IRAnnotationValue
import kotlinx.serialization.Serializable

@Serializable
data class IRValueArgument(
	val name: String?,
	val value: IRAnnotationValue
)
