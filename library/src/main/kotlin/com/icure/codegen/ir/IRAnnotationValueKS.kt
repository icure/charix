package com.icure.codegen.ir

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.icure.codegen.ir.annotation.toIRAnnotation
import com.icure.codegen.ir.declaration.toIRTypeReference
import com.icure.codegen.ir.entity.toIRTypeReference
import com.icure.codegen.utils.isEncryptableOrSomethingSecure
import kotlin.reflect.KClass


fun IRAnnotationValue.Companion.of(value: KClass<*>?): IRAnnotationValue = handlingNull(value) {
	IRPlainEntityReference(
		packageName = it.qualifiedName?.replace(".${it.simpleName}", ""),
		simpleName = checkNotNull(it.simpleName) { "Simple name cannot be null" },
		parentDeclarations = emptyList(),
		isNullable = false,
		superTypes = emptyList(),
		isEncryptable = it.isEncryptableOrSomethingSecure(),
		annotations = emptyList()
	)
}

fun IRAnnotationValue.Companion.of(value: KSAnnotation?): IRAnnotationValue = handlingNull(value) {
	it.toIRAnnotation()
}

fun IRAnnotationValue.Companion.of(value: KSType?): IRAnnotationValue = handlingNull(value) {
	it.toIRTypeReference()
}

fun IRAnnotationValue.Companion.of(value: KSClassDeclaration?): IRAnnotationValue = handlingNull(value) {
	it.toIRTypeReference(false, null)
}

fun IRAnnotationValue.Companion.ofDispatched(value: Any?): IRAnnotationValue =
	when (value) {
		is KSAnnotation -> of(value)
		is List<*> -> of(value, ::ofDispatched)
		is KClass<*> -> of(value)
		is KSType -> of(value)
		is KSClassDeclaration -> of(value)
		null -> of(value)
		is String -> of(value)
		is Long -> of(value)
		is Int -> of(value)
		is Double -> of(value)
		is Boolean -> of(value)
		else -> throw IllegalStateException("Unexpected annotation value: ${value::class.qualifiedName}")
	}