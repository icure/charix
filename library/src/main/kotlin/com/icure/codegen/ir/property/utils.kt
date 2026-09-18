package com.icure.codegen.ir.property

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.icure.codegen.ir.declaration.extractDocString
import com.icure.codegen.ir.IRCodeBlock
import com.icure.codegen.ir.IRModifier
import com.icure.codegen.ir.IRProperty
import com.icure.codegen.ir.annotation.defaultDefaultValueForType
import com.icure.codegen.ir.annotation.extractDefaultValueFromAnnotation
import com.icure.codegen.ir.annotation.toIRAnnotation
import com.icure.codegen.ir.entity.toIRTypeReference
import com.icure.codegen.ir.fromModifier

fun KSValueParameter.toIRProperty(): IRProperty {
	val name = requireNotNull(name?.asString()) { "Parameter name cannot be null" }
	val type = type.toIRTypeReference()

	return IRProperty(
		packageName = (parent as? KSDeclaration)?.packageName?.asString(),
		simpleName = name,
		irType = type,
		modifiers = emptySet(),
		annotations = annotations.map { it.toIRAnnotation() }.toList(),
		getter = null,
		setter = null,
		defaultValue = if (hasDefault) extractDefaultValueFromAnnotation(type, annotations.toList()) ?: defaultDefaultValueForType(type) else null,
		isConstructor = true,
		docString = null
	)
}

fun KSPropertyDeclaration.toIRProperty(withDefaultValue: Boolean): IRProperty {
	val name = simpleName.asString()
	val type = type.toIRTypeReference()

	val defaultValue = if (withDefaultValue) {
		extractDefaultValueFromAnnotation(type, annotations.toList())
	} else null

	return IRProperty(
		packageName = (parent as? KSDeclaration)?.packageName?.asString(),
		simpleName = name,
		irType = type,
		modifiers = modifiers.map { IRModifier.fromModifier(it) }.toSet(),
		annotations = annotations.map { it.toIRAnnotation() }.toList(),
		getter = null,
		setter = null,
		defaultValue = defaultValue,
		isConstructor = false,
		docString = extractDocString()
	)
}

