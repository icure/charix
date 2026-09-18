package com.icure.codegen.utils

import kotlin.collections.contains
import kotlin.reflect.KClass

fun KClass<*>.isEncryptableOrSomethingSecure(): Boolean = supertypes.any {
	(ENCRYPTABLE_SIMPLE_NAMES + ENCRYPTABLE_DTO_SIMPLE_NAMES).contains((it.classifier as? KClass<*>)?.simpleName)
} || simpleName == "ContentDto" || simpleName == "Content"