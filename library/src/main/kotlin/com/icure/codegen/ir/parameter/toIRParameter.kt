package com.icure.codegen.ir.parameter

import com.google.devtools.ksp.symbol.KSValueParameter
import com.icure.codegen.functions.toPascalCase
import com.icure.codegen.ir.IRCodeBlock
import com.icure.codegen.ir.IREntityReference
import com.icure.codegen.ir.IRNull
import com.icure.codegen.ir.IRParameter
import com.icure.codegen.ir.IRText
import com.icure.codegen.ir.annotation.extractDefaultValueFromAnnotation
import com.icure.codegen.ir.entity.toIRTypeReference
import com.icure.codegen.ir.extractSourceAndNameOrThrow

fun KSValueParameter.toIRParameter(): IRParameter {
		val name = requireNotNull(name?.asString()) { "Parameter name cannot be null" }
		val type = type.toIRTypeReference()
		val sourceAndUrlName = extractSourceAndNameOrThrow()
		val source = sourceAndUrlName?.first
		val defaultValue = if (source != null) {
			getRestApiParameterDefaultValue(type, source)
		} else {
			extractDefaultValueFromAnnotation(type, annotations.toList())
		}
		return IRParameter(name, type, source, sourceAndUrlName?.second, defaultValue, null)
	}


private fun KSValueParameter.getRestApiParameterDefaultValue(
	type: IREntityReference,
	source: IRParameter.ParameterSource
): IRCodeBlock? =
	when {
		hasDefault && type.isNullable ->
			IRCodeBlock.of("%L", IRNull)
		source == IRParameter.ParameterSource.REQUEST || source == IRParameter.ParameterSource.HEADER ->
			extractRestApiParameterDefaultValueFromAnnotation(source)
		else ->
			null
	}

private fun KSValueParameter.extractRestApiParameterDefaultValueFromAnnotation(
	source: IRParameter.ParameterSource
): IRCodeBlock? = annotations
	.firstOrNull { it.shortName.asString() == source.annotationName }
	?.arguments
	?.takeIf { args ->
		(args.firstOrNull { it.name?.asString() == "required" }?.value as? Boolean) == false
	}?.let { args ->
		if(type.resolve().isMarkedNullable) IRCodeBlock.of("%L", IRNull)
		else args.firstOrNull { it.name?.asString() == "defaultValue" }?.value?.let {
			when (type.resolve().declaration.simpleName.asString()) {
				"String" -> IRCodeBlock.of("%S", IRText(it.toString())) // On cloud api annotations we don't wrap strings in quotes
				"SortDirectionDto" -> IRCodeBlock.of("SortDirection.%L", IRText(it.toString().toPascalCase()))
				else -> IRCodeBlock.of("%L", IRText(it.toString()))
			}
		}
	}
