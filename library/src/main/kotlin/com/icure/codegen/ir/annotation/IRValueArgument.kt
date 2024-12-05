package com.icure.codegen.ir.annotation

import com.google.devtools.ksp.symbol.KSValueArgument
import com.icure.codegen.ir.IRAnnotationValue
import kotlinx.serialization.Serializable

@Serializable
data class IRValueArgument(
	val name: String?,
	val value: IRAnnotationValue
)

fun KSValueArgument.toIRValueArgument(): IRValueArgument = IRValueArgument(name = name?.asString(), value = IRAnnotationValue.of(value))
