package com.icure.codegen.ir

import kotlinx.serialization.Serializable

@Serializable
sealed interface IRPrimitive : IRNode, IRAnnotationValue {
	val value: Any?
}

@Serializable
data class IRText(override val value: String) : IRPrimitive, IRAnnotationValue

@Serializable
data class IRLong(override val value: Long) : IRPrimitive, IRAnnotationValue

@Serializable
data class IRInt(override val value: Int) : IRPrimitive, IRAnnotationValue

@Serializable
data class IRDouble(override val value: Double) : IRPrimitive, IRAnnotationValue

@Serializable
data class IRBoolean(override val value: Boolean) : IRPrimitive, IRAnnotationValue

@Serializable
data class IRList(override val value: List<IRAnnotationValue>) : IRPrimitive, IRAnnotationValue

@Serializable
data object IRNull : IRPrimitive, IRAnnotationValue {
	override val value: Nothing?
		get() = null
}
