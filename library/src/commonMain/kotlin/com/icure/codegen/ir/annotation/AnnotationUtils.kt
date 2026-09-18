package com.icure.codegen.ir.annotation

import com.icure.codegen.ir.IRAnnotation
import com.icure.codegen.ir.IRBoolean
import com.icure.codegen.ir.IRCodeBlock
import com.icure.codegen.ir.IREntityReference
import com.icure.codegen.ir.IRList
import com.icure.codegen.ir.IRNull
import com.icure.codegen.ir.IRText
import com.icure.codegen.ir.canonicalName

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
