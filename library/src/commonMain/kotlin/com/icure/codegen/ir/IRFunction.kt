package com.icure.codegen.ir

import kotlinx.serialization.Serializable

@Serializable
data class IRFunction(
	val enclosingClass: String?,
	override val packageName: String?,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	val returnType: IREntityReference?,
	val parameters: List<IRParameter>,
	override val annotations: List<IRAnnotation>,
	override val modifiers: Set<IRModifier>,
	override val docString: String?
): IRDeclaration
