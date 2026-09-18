package com.icure.codegen.ir.annotation

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueArgument
import com.icure.codegen.ir.IRAnnotation
import com.icure.codegen.ir.IRAnnotationValue
import com.icure.codegen.ir.IRCodeBlock
import com.icure.codegen.ir.IREntityReference
import com.icure.codegen.ir.IRMemberReference
import com.icure.codegen.ir.IRText
import com.icure.codegen.ir.ofDispatched
import com.icure.codegen.ir.simpleNameWithoutDto

/**
 * @receiver an [IREntityReference].
 * @return a [CodeBlock] or null if a default value could not be found.
 */
fun extractDefaultValueFromAnnotation(
    type: IREntityReference,
    annotations: List<KSAnnotation>
): IRCodeBlock? {
	val name = type.simpleNameWithoutDto()

	val defaultValueFromAnnotation =
		annotations.getDefaultValueFromMultiplatformAnnotationOrNull() ?: annotations.getDefaultValueFromSchemaOrNull()

	return if (defaultValueFromAnnotation.isNullOrEmpty()) {
		null
	} else when {
		name == "String" -> IRCodeBlock("%L", listOfNotNull(IRText(defaultValueFromAnnotation))) // On schema and multiplatform default value annotations we wrap strings in quotes
		name == "Duration" && type.packageName == "kotlin.time" -> {
			val split = defaultValueFromAnnotation.split(".")
			check(split.size == 2)
			val amount = split[0]
			val unit = split[1]
			IRCodeBlock.of(
				"$amount.%M",
				IRMemberReference("kotlin.time.Duration.Companion", unit)
			)
		}
		else -> IRCodeBlock(
			"%L",
			listOfNotNull(
				if ("$name\\S+\\(\\)".toRegex().matches(defaultValueFromAnnotation))
					IRText("$name()")
				else
					IRText(defaultValueFromAnnotation)
			)
		)
	}
}

fun KSAnnotation.toIRAnnotation(): IRAnnotation {
	val typeAsDeclaration = annotationType.resolve().declaration
	return IRAnnotation(
		packageName = typeAsDeclaration.packageName.asString(),
		simpleName = typeAsDeclaration.simpleName.asString().split(".").last(),
		parentDeclarations = typeAsDeclaration.simpleName.asString().split(".").dropLast(1),
		arguments = arguments.map { it.toIRValueArgument() }
	)
}

fun Sequence<KSAnnotation>.toIRAnnotations(): List<IRAnnotation> =
	map { it.toIRAnnotation() }.toList()

fun KSAnnotated.getJacksonSerialNameOrNull(): String? {
	return this.annotations.toList().firstOrNull { it.shortName.asString() == "JsonProperty" }
		?.let { it.arguments.first().value.toString() }
}

/**
 * Given a [List] of [KSAnnotation], checks if a swagger `Schema` annotation is present and extract the defaultValue
 * parameter as String, if present.
 *
 * @receiver a [List] of [KSAnnotation]
 * @return the defaultValue specified in the swagger @Schema annotation, if present.
 */
fun List<KSAnnotation>.getDefaultValueFromSchemaOrNull() = this.asSequence().getDefaultValueFromSchemaOrNull()

fun Sequence<KSAnnotation>.getDefaultValueFromSchemaOrNull() = firstOrNull {
	it.shortName.asString() == "Schema"
}?.arguments?.firstOrNull {
	it.name?.asString() == "defaultValue"
}?.value as String?

fun KSAnnotation.urlNameOrNull() =
	arguments.firstOrNull { it.name?.asString() == "name" }?.value?.toString()
		?: arguments.firstOrNull { it.name?.asString() == "value" }?.value?.toString()

fun List<KSAnnotation>.getDefaultValueFromMultiplatformAnnotationOrNull() = firstOrNull {
	it.shortName.asString() == "DefaultValue"
}?.arguments?.singleOrNull()?.value as? String

fun KSValueArgument.toIRValueArgument(): IRValueArgument =
	IRValueArgument(name = name?.asString(), value = IRAnnotationValue.ofDispatched(value))