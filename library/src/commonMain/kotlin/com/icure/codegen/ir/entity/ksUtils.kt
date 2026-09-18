package com.icure.codegen.ir.entity

import com.icure.codegen.ir.IREntity
import com.icure.codegen.ir.IREntityReference
import com.icure.codegen.ir.IRPlainEntityReference
import com.icure.codegen.ir.annotation.IRAnnotated
import com.icure.codegen.utils.KRAKEN_DTO_BASE_PATH
import com.icure.codegen.utils.toSdkDtoPackage
import kotlin.collections.orEmpty

fun <T : IREntity> T.mapToModelType(): IREntityReference = when {
	packageName?.startsWith(KRAKEN_DTO_BASE_PATH) == true -> IRPlainEntityReference(
		packageName = packageName?.toSdkDtoPackage(),
		simpleName =  simpleName.replace("Dto", ""),
		parentDeclarations = parentDeclarations,
		isNullable = false,
		isEncryptable = false,
		superTypes = emptyList(),
		annotations = (this as? IRAnnotated)?.annotations.orEmpty(),
	)
	else -> IRPlainEntityReference(
		packageName = packageName,
		simpleName =  simpleName,
		parentDeclarations = parentDeclarations,
		isNullable = false,
		isEncryptable = false,
		superTypes = emptyList(),
		annotations = (this as? IRAnnotated)?.annotations.orEmpty(),
	)
}
