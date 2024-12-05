package com.icure.codegen.ir.annotation

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.icure.codegen.ir.IRAnnotation
import com.icure.codegen.ir.IRBoolean
import com.icure.codegen.ir.IRCodeBlock
import com.icure.codegen.ir.IREntityReference
import com.icure.codegen.ir.IRList
import com.icure.codegen.ir.IRMemberReference
import com.icure.codegen.ir.IRNull
import com.icure.codegen.ir.IRText
import com.icure.codegen.ir.canonicalName
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

fun defaultDefaultValueForType(
	type: IREntityReference
) = when {
	type.isNullable -> IRCodeBlock("%L", listOf(IRNull))
	type.canonicalName == "kotlin.collections.Set" -> IRCodeBlock("%L", listOf(IRText("emptySet()")))
	type.canonicalName == "kotlin.collections.List" -> IRCodeBlock("%L", listOf(IRText("emptyList()")))
	type.canonicalName == "kotlin.collections.Map" -> IRCodeBlock("%L", listOf(IRText("emptyMap()")))
	type.canonicalName == "kotlin.Boolean" -> IRCodeBlock("%L", listOf(IRBoolean(false)))
	else -> null
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

/**
 * Finds the annotation the contains the request path (e.g. RestController, PostMapping) in the iterable and returns
 * the request path.
 *
 * @receiver an [Sequence] of [KSAnnotation].
 * @param annotationName the simple name of the annotation where to extract the path.
 * @return the request path.
 * @throws IllegalArgumentException if the annotation is not found in the [Sequence].
 */
fun Sequence<IRAnnotation>.findRequestPathOrThrow(annotationName: String): String =
	firstOrNull { it.simpleName == annotationName }
		?.arguments
		?.firstOrNull { it.name == "value" }?.let { valueArg ->
			when {
				valueArg.value is IRText -> valueArg.value.value
				valueArg.value is IRList && valueArg.value.value.isNotEmpty() -> {
					val element = valueArg.value.value.first()
					if(element is IRText) element.value else ""
				}
				else -> ""
			}
		} ?: throw IllegalArgumentException("No request path found for annotation $annotationName")

fun List<IRAnnotation>.findRequestPathOrThrow(annotationName: String): String = asSequence().findRequestPathOrThrow(annotationName)

/**
 * @see [findRequestPathOrThrow].
 */
fun IRAnnotation.findRequestPathOrThrow(): String =
	sequenceOf(this).findRequestPathOrThrow(simpleName)

fun IRAnnotated.getJacksonSerialNameOrNull(): String?  = annotations.toList().firstOrNull { it.simpleName == "JsonProperty" }?.arguments?.first()?.value?.let { it as? IRText }?.value
fun IRAnnotated.getKotlinxSerialNameOrNull(): String?  = annotations.toList().firstOrNull { it.simpleName == "SerialName" }?.arguments?.first()?.value?.let { it as? IRText }?.value

fun IRAnnotated.getJsonDiscriminatedOrNull(): String? = annotations.find { it.simpleName == "JsonDiscriminated" }?.arguments?.firstOrNull { it.name == "value" }?.value?.let {
	(it as IRText).value
}

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
