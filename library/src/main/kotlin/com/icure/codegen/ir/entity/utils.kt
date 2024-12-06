package com.icure.codegen.ir.entity

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance
import com.icure.codegen.ir.IRDecryptedEntityReference
import com.icure.codegen.ir.IREncryptedEntityReference
import com.icure.codegen.ir.IREntity
import com.icure.codegen.ir.IREntityReference
import com.icure.codegen.ir.IRFunctionTypeReference
import com.icure.codegen.ir.IRGenericReference
import com.icure.codegen.ir.IRPlainEntityReference
import com.icure.codegen.ir.IRStar
import com.icure.codegen.ir.declaration.IRTypeParameter
import com.icure.codegen.ir.declaration.isEncryptableOrSomethingSecure
import com.icure.codegen.ir.declaration.toIRTypeReference
import com.icure.codegen.ir.parameter.toIRTypeParameter
import com.icure.codegen.ir.utils.KRAKEN_DTO_BASE_PATH
import com.icure.codegen.ir.utils.toSdkDtoPackage

fun getParentDeclarations(declaration: KSDeclaration): List<String> {
	val parent = declaration.parentDeclaration?.takeIf { parent ->
		parent.typeParameters.none { it.name.asString() == declaration.simpleName.asString() }
	}
	val parentName: String? = parent?.simpleName?.asString()
	return if (parentName != null) getParentDeclarations(parent) + parentName else emptyList()
}

fun KSTypeReference?.toIRTypeReference(): IREntityReference = this?.resolve()?.let { type ->
	val typeArguments = type.arguments.map {
		if(it.variance == Variance.STAR) {
			IRTypeParameter(name = "*", bounds = emptyList(), value = IRStar)
		} else {
			it.toIRTypeParameter()
		}
	}

	val isEncryptable = type.isEncryptableOrSomethingSecure()
	when {
		type.declaration is KSTypeParameter -> IRGenericReference(
			packageName = type.declaration.packageName.asString(),
			simpleName = type.declaration.simpleName.asString(),
			isNullable = type.isMarkedNullable,
			parentDeclarations = getParentDeclarations(type.declaration)
		)
		type.isFunctionType || type.isSuspendFunctionType -> IRFunctionTypeReference(
			packageName = type.declaration.packageName.asString(),
			simpleName = type.declaration.simpleName.asString(),
			isNullable = type.isMarkedNullable,
			isSuspend = type.isSuspendFunctionType,
			parameters = type.arguments.dropLast(1).map { it.type.toIRTypeReference() },
			returnType = type.arguments.last().type.toIRTypeReference()
		)
		isEncryptable && type.declaration.simpleName.asString().startsWith("Decrypted") -> IRDecryptedEntityReference(
			packageName = type.declaration.packageName.asString(),
			simpleName = type.declaration.simpleName.asString(),
			parentDeclarations = getParentDeclarations(type.declaration),
			isNullable = type.isMarkedNullable,
			superTypes = (type as? KSClassDeclaration)?.superTypes?.map { it.toIRTypeReference() }?.toList() ?: emptyList(),
		)
		isEncryptable && type.declaration.simpleName.asString().startsWith("Encrypted") -> IREncryptedEntityReference(
			packageName = type.declaration.packageName.asString(),
			simpleName = type.declaration.simpleName.asString(),
			parentDeclarations = getParentDeclarations(type.declaration),
			isNullable = type.isMarkedNullable,
			superTypes = (type as? KSClassDeclaration)?.superTypes?.map { it.toIRTypeReference() }?.toList() ?: emptyList(),
		)
		else -> IRPlainEntityReference(
			packageName = type.declaration.packageName.asString(),
			simpleName = type.declaration.simpleName.asString(),
			parentDeclarations = getParentDeclarations(type.declaration),
			isNullable = type.isMarkedNullable,
			superTypes = (type as? KSClassDeclaration)?.superTypes?.map { it.toIRTypeReference() }?.toList() ?: emptyList(),
			isEncryptable = isEncryptable
		).parametrizedBy(typeArguments)
	}
} ?: IRPlainEntityReference(
	packageName = "",
	simpleName = "Unit",
	parentDeclarations = emptyList(),
	isNullable = false,
	superTypes = emptyList(),
	isEncryptable = false
)

fun <T : IREntity> T.mapToModelType(): IREntityReference = when {
	packageName?.startsWith(KRAKEN_DTO_BASE_PATH) == true -> IRPlainEntityReference(
		packageName = packageName?.toSdkDtoPackage(),
		simpleName =  simpleName.replace("Dto", ""),
		parentDeclarations = parentDeclarations,
		isNullable = false,
		isEncryptable = false,
		superTypes = emptyList()
	)
	else -> IRPlainEntityReference(
		packageName = packageName,
		simpleName =  simpleName,
		parentDeclarations = parentDeclarations,
		isNullable = false,
		isEncryptable = false,
		superTypes = emptyList()
	)
}

fun KSTypeAlias.toIRTypeReference() = IRPlainEntityReference(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	isNullable = false,
	isEncryptable = false,
	superTypes = emptyList()
).mapToModelType()

fun KSTypeParameter.toIRTypeReference() = IRPlainEntityReference(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	isNullable = false,
	isEncryptable = false,
	superTypes = emptyList()
).mapToModelType()

fun KSType.toIRTypeReference(): IREntityReference = when(val decl = this.declaration) {
	is KSClassDeclaration -> decl.toIRTypeReference(isMarkedNullable, null)
	is KSTypeAlias -> decl.toIRTypeReference()
	is KSTypeParameter -> decl.toIRTypeReference()
	else -> IRPlainEntityReference(
		packageName = decl.packageName.asString(),
		simpleName = decl.simpleName.asString(),
		parentDeclarations = getParentDeclarations(decl),
		isNullable = false,
		superTypes = emptyList(),
		isEncryptable = this.isEncryptableOrSomethingSecure()
	)
}
