package com.icure.codegen.ir

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.icure.codegen.ir.annotation.toIRAnnotation
import com.icure.codegen.ir.entity.toIRTypeReference
import com.icure.codegen.utils.isEncryptableOrSomethingSecure
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
sealed interface IRAnnotationValue : IRNode {

	companion object {

		fun of(value: Any?): IRAnnotationValue {
			return when (value) {
				is KSAnnotation -> value.toIRAnnotation()
				is List<*> -> IRList(value.map { of(it) })
				is KClass<*> -> IRPlainEntityReference(
					packageName = value.qualifiedName?.replace(".${value.simpleName}", ""),
					simpleName = checkNotNull(value.simpleName) { "Simple name cannot be null" },
					parentDeclarations = emptyList(),
					isNullable = false,
					superTypes = emptyList(),
					isEncryptable = value.isEncryptableOrSomethingSecure(),
					annotations = emptyList()
				)
				is KSType -> value.toIRTypeReference()

				null, is String, is Long, is Int, is Double, is Boolean -> ofPrimitive(value)

				else -> throw IllegalStateException("Unexpected annotation value: ${value::class.qualifiedName}")
			}
		}

		private fun ofPrimitive(value: Any?): IRPrimitive = when (value) {
			null -> IRNull
			is String -> IRText(value)
			is Long -> IRLong(value)
			is Int -> IRInt(value)
			is Double -> IRDouble(value)
			is Boolean -> IRBoolean(value)
			else -> throw IllegalStateException("Unexpected primitive annotation value: ${value::class.qualifiedName}")
		}
	}

}
