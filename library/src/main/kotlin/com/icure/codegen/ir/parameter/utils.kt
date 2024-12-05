package com.icure.codegen.ir.parameter

import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.icure.codegen.ir.declaration.IRTypeParameter
import com.icure.codegen.ir.entity.toIRTypeReference

fun KSTypeArgument.toIRTypeParameter() = IRTypeParameter(
	name = "",
	bounds = emptyList(),
	value = type.toIRTypeReference()
)

fun KSTypeParameter.toIRTypeParameter(type: KSTypeReference?) = IRTypeParameter(
	name = simpleName.asString(),
	bounds = bounds.map { it.toIRTypeReference() }.toList(),
	value = type?.toIRTypeReference()
)
