package com.icure.codegen.ir.declaration

import com.google.devtools.ksp.symbol.KSTypeParameter
import com.icure.codegen.ir.entity.toIRTypeReference


fun KSTypeParameter.toIRTypeParameter() = IRTypeParameter(
	name = name.asString(),
	bounds = bounds.map { bound ->
		bound.toIRTypeReference()
	}.toList(),
)