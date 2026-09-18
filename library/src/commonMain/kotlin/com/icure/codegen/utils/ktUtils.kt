package com.icure.codegen.utils

import com.icure.codegen.ir.IRClassDeclaration
import com.icure.codegen.ir.IREntityReference

fun IREntityReference.isEncryptableOrSomethingSecure(): Boolean =
	simpleName == "ContentDto" || simpleName == "Content" || superTypes.any {
		(ENCRYPTABLE_SIMPLE_NAMES + ENCRYPTABLE_DTO_SIMPLE_NAMES).contains(it.simpleName)
	} || superTypes.any { it.isEncryptableOrSomethingSecure() }

fun IRClassDeclaration.isEncryptableOrSomethingSecure(): Boolean =
	simpleName == "ContentDto" || simpleName == "Content" || superTypes.any {
		(ENCRYPTABLE_SIMPLE_NAMES + ENCRYPTABLE_DTO_SIMPLE_NAMES).contains(it.simpleName)
	} || superTypes.any { it.isEncryptableOrSomethingSecure() }

fun String.toSdkDtoPackage() = replace(KRAKEN_DTO_BASE_PATH, SDK_MODEL_PACKAGE)
