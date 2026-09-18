package com.icure.codegen.ir

import com.icure.codegen.ir.annotation.IRAnnotated
import com.icure.codegen.ir.declaration.IRTypeParameter
import com.icure.codegen.utils.KRAKEN_DTO_BASE_PATH
import com.icure.codegen.models.EncryptableFlavour
import kotlinx.serialization.Serializable

@Serializable
sealed interface IREntityReference: IREntity, IRAnnotationValue, IRAnnotated {
	val isNullable: Boolean
	val isEncryptable: Boolean
	val typeParameters: List<IRTypeParameter>
	val superTypes: List<IREntityReference>

	fun toFlavour(flavour: EncryptableFlavour?): IREntityReference
	fun parametrizedBy(typeParameters: List<IRTypeParameter>): IREntityReference
	fun setNullable(nullable: Boolean): IREntityReference
}

@Serializable
object IRStar : IREntityReference {
	override val isNullable = false
	override val packageName = null
	override val parentDeclarations: List<String> = emptyList()
	override val simpleName: String = "*"
	override val typeParameters: List<IRTypeParameter> = emptyList()
	override val superTypes: List<IREntityReference> = emptyList()
	override val annotations: List<IRAnnotation> = emptyList()
	override val isEncryptable = false

	override fun toFlavour(flavour: EncryptableFlavour?) = this
	override fun parametrizedBy(typeParameters: List<IRTypeParameter>) = this
	override fun setNullable(nullable: Boolean) = this
}

@Serializable
data class IRGenericReference(
	override val packageName: String?,
	override val isNullable: Boolean,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	override val annotations: List<IRAnnotation>,
) : IREntityReference {

	override val typeParameters: List<IRTypeParameter> = emptyList()
	override val superTypes: List<IREntityReference> = emptyList()
	override val isEncryptable: Boolean = false

	override fun toFlavour(flavour: EncryptableFlavour?) = this
	override fun parametrizedBy(typeParameters: List<IRTypeParameter>) = this
	override fun setNullable(nullable: Boolean) = if(nullable != isNullable) copy(isNullable = nullable) else this

}

@Serializable
data class IRFunctionTypeReference(
	override val packageName: String?,
	override val isNullable: Boolean,
	override val simpleName: String,
	val isSuspend: Boolean,
	val parameters: List<IREntityReference>,
	val returnType: IREntityReference,
	override val annotations: List<IRAnnotation>,
) : IREntityReference {

	override val isEncryptable: Boolean = false
	override val typeParameters: List<IRTypeParameter> = emptyList()
	override val superTypes: List<IREntityReference> = emptyList()
	override val parentDeclarations: List<String> = emptyList()

	override fun toFlavour(flavour: EncryptableFlavour?): IREntityReference = copy(
		parameters = parameters.map { it.toFlavour(flavour) },
		returnType = returnType.toFlavour(flavour)
	)
	override fun parametrizedBy(typeParameters: List<IRTypeParameter>): IREntityReference = this
	override fun setNullable(nullable: Boolean): IREntityReference = if(nullable != isNullable) copy(isNullable = nullable) else this

}

@Serializable
sealed interface IRClassReference : IREntityReference, IRAnnotationValue

@Serializable
data class IRPlainEntityReference(
	override val packageName: String?,
	/**
	 * The simple name of the entity.
	 */
	override val simpleName: String,
	/**
	 * All parent declarations of the entity, the last element is the direct parent of this. For example if the entity
	 * is class `DeeplyNested`, declared in class `Nested`, which, in turn, is declared in class `Foo`, then the parent
	 * declarations of this class are `["Foo", "Nested"]`.
	 */
	override val parentDeclarations: List<String>,
	override val isNullable: Boolean,
	override val isEncryptable: Boolean,
	override val typeParameters: List<IRTypeParameter> = emptyList(),
	override val superTypes: List<IREntityReference>,
	override val annotations: List<IRAnnotation>,
) : IRClassReference {

	override fun toFlavour(flavour: EncryptableFlavour?) = when {
		isEncryptable && flavour == EncryptableFlavour.DECRYPTED -> IRDecryptedEntityReference(
			packageName = packageName,
			simpleName = "${flavour.value}$simpleName",
			parentDeclarations = parentDeclarations,
			isNullable = isNullable,
			superTypes = superTypes.map { it.toFlavour(flavour) },
			annotations = annotations,
		)
		isEncryptable && flavour == EncryptableFlavour.ENCRYPTED -> IREncryptedEntityReference(
			packageName = packageName,
			simpleName = "${flavour.value}$simpleName",
			parentDeclarations = parentDeclarations,
			isNullable = isNullable,
			superTypes = superTypes.map { it.toFlavour(flavour) },
			annotations = annotations,
		)
		else -> if (simpleName.startsWith("Decrypted") || simpleName.startsWith("Encrypted")) {
			simpleName.drop("xxcrypted".length).takeIf { noncryptedName ->
				superTypes.any { noncryptedName == it.simpleName }
			}?.let { noncryptedName ->
				IRPlainEntityReference(
					packageName = packageName,
					simpleName = noncryptedName,
					parentDeclarations = parentDeclarations,
					isNullable = isNullable,
					isEncryptable = isEncryptable,
					superTypes = superTypes.map { it.toFlavour(null) },
					annotations = annotations,
				)
			} ?: this
		} else this
	}.copyWith(typeParameters = typeParameters.map { it.toFlavour(flavour) })

	override fun parametrizedBy(typeParameters: List<IRTypeParameter>) = if(typeParameters.isNotEmpty()) copy(typeParameters = typeParameters) else this
	override fun setNullable(nullable: Boolean): IREntityReference = if(nullable != isNullable) this.copy(isNullable = nullable) else this
}

@Serializable
data class IRDecryptedEntityReference(
	override val packageName: String?,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	override val isNullable: Boolean,
	override val typeParameters: List<IRTypeParameter> = emptyList(),
	override val superTypes: List<IREntityReference>,
	override val annotations: List<IRAnnotation>,
) : IREntityReference {

	override val isEncryptable: Boolean = true

	override fun toFlavour(flavour: EncryptableFlavour?) = when(flavour) {
		EncryptableFlavour.DECRYPTED -> this
		EncryptableFlavour.ENCRYPTED -> IREncryptedEntityReference(
			packageName = packageName,
			simpleName = simpleName.replace(
				EncryptableFlavour.DECRYPTED.value,
				EncryptableFlavour.ENCRYPTED.value
			),
			parentDeclarations = parentDeclarations,
			isNullable = isNullable,
			superTypes = superTypes.map { it.toFlavour(flavour) },
			annotations = annotations,
		)
		null -> IRPlainEntityReference(
            packageName = packageName,
            simpleName = simpleName.replace(EncryptableFlavour.DECRYPTED.value, ""),
			parentDeclarations = parentDeclarations,
            isNullable = isNullable,
			isEncryptable = true,
			superTypes = superTypes.map { it.toFlavour(null) },
			annotations = annotations,
		)
	}.copyWith(typeParameters = typeParameters.map { it.toFlavour(flavour) })

	override fun parametrizedBy(typeParameters: List<IRTypeParameter>) = if(typeParameters.isNotEmpty()) copy(typeParameters = typeParameters) else this
	override fun setNullable(nullable: Boolean): IREntityReference = if(nullable != isNullable) this.copy(isNullable = nullable) else this
}

@Serializable
data class IREncryptedEntityReference(
	override val packageName: String?,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	override val isNullable: Boolean,
	override val typeParameters: List<IRTypeParameter> = emptyList(),
	override val superTypes: List<IREntityReference>,
	override val annotations: List<IRAnnotation>,
) : IREntityReference {

	override val isEncryptable: Boolean = true

	override fun toFlavour(flavour: EncryptableFlavour?) = when(flavour) {
		EncryptableFlavour.DECRYPTED -> IRDecryptedEntityReference(
			packageName = packageName,
			simpleName = simpleName.replace(
				EncryptableFlavour.ENCRYPTED.value,
				EncryptableFlavour.DECRYPTED.value
			),
			parentDeclarations = parentDeclarations,
			isNullable = isNullable,
			superTypes = superTypes.map { it.toFlavour(flavour) },
			annotations = annotations,
		)
		EncryptableFlavour.ENCRYPTED -> this
		null -> IRPlainEntityReference(
            packageName = packageName,
            simpleName = simpleName.replace(EncryptableFlavour.ENCRYPTED.value, ""),
			parentDeclarations = parentDeclarations,
            isNullable = isNullable,
			isEncryptable = true,
			superTypes = superTypes.map { it.toFlavour(null) },
			annotations = annotations,
		)
	}.copyWith(typeParameters = typeParameters.map { it.toFlavour(flavour) })

	override fun parametrizedBy(typeParameters: List<IRTypeParameter>) = if(typeParameters.isNotEmpty()) copy(typeParameters = typeParameters) else this
	override fun setNullable(nullable: Boolean): IREntityReference = if(nullable != isNullable) this.copy(isNullable = nullable) else this
}

fun IREntityReference.copyWith(
	superTypes: List<IREntityReference>? = null,
	typeParameters: List<IRTypeParameter>? = null,
	packageName: String? = null,
	simpleName: String? = null,
	parentDeclarations: List<String>? = null,
	isNullable: Boolean? = null,
) = when(this) {
	is IRPlainEntityReference -> this.copy(typeParameters = typeParameters ?: this.typeParameters, superTypes = superTypes ?: this.superTypes, packageName = packageName ?: this.packageName, simpleName = simpleName ?: this.simpleName, parentDeclarations = parentDeclarations ?: this.parentDeclarations, isNullable = isNullable ?: this.isNullable)
	is IRDecryptedEntityReference -> this.copy(typeParameters = typeParameters ?: this.typeParameters, superTypes = superTypes ?: this.superTypes, packageName = packageName ?: this.packageName, simpleName = simpleName ?: this.simpleName, parentDeclarations = parentDeclarations ?: this.parentDeclarations, isNullable = isNullable ?: this.isNullable)
	is IREncryptedEntityReference -> this.copy(typeParameters = typeParameters ?: this.typeParameters, superTypes = superTypes ?: this.superTypes, packageName = packageName ?: this.packageName, simpleName = simpleName ?: this.simpleName, parentDeclarations = parentDeclarations ?: this.parentDeclarations, isNullable = isNullable ?: this.isNullable)
	is IRGenericReference -> this.copy(packageName = packageName ?: this.packageName, simpleName = simpleName ?: this.simpleName, parentDeclarations = parentDeclarations ?: this.parentDeclarations, isNullable = isNullable ?: this.isNullable)
	is IRFunctionTypeReference -> this.copy(packageName = packageName ?: this.packageName, simpleName = simpleName ?: this.simpleName, isNullable = isNullable ?: this.isNullable)
	IRStar -> this
}



fun IREntityReference.simpleNameWithoutDto() = when {
	packageName?.startsWith(KRAKEN_DTO_BASE_PATH) == true -> simpleName.replace("Dto", "")
	else -> this.simpleName
}
