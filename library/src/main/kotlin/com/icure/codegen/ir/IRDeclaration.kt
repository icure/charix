package com.icure.codegen.ir

import com.icure.codegen.ir.annotation.IRAnnotated
import com.icure.codegen.ir.declaration.IRTypeParameter
import com.icure.codegen.utils.isEncryptableOrSomethingSecure
import kotlinx.serialization.Serializable

@Serializable
sealed interface IRDeclaration : IREntity, IRAnnotated {
	val modifiers: Set<IRModifier>
	val docString: String?
}

@Serializable
sealed interface IRClassDeclaration : IRDeclaration {
	val superTypes: List<IREntityReference>
	val isEncryptable: Boolean
	val properties: List<IRProperty>
	val declarations: List<IRDeclaration>
	val typeParameters: List<IRTypeParameter>
}

val IRClassDeclaration.isFlavoured get() = (
	simpleName.startsWith("Decrypted") || simpleName.startsWith("Encrypted")
) && simpleName.drop("xxcrypted".length).let { noncryptedName ->
	superTypes.any { noncryptedName == it.simpleName }
}

val IRClassDeclaration.supertypesIgnoringAny get() = superTypes.filter { it.canonicalName != "kotlin.Any" }
val IRClassDeclaration.constructorProperties get() = properties.filter { it.constructor }

val IRClassDeclaration.jsonDiscriminatorName get() = annotations
	.find { it.simpleName == "JsonDiscriminator" }?.arguments
	?.firstOrNull { it.name == "value" }?.value?.let { it as? IRText }?.value
	?: declarations.firstOrNull { it.simpleName == "includeDiscriminator" }?.let { discriminatorProperty ->
		discriminatorProperty.annotations.firstOrNull { it.simpleName == "JsonProperty" }?.let { ann ->
			(ann.arguments.first { it.name == "value" }.value as? IRText)?.value
		}
	}

@Serializable
sealed interface IRExtendable : IRClassDeclaration

@Serializable
data class IRClass(
	override val packageName: String?,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	override val annotations: List<IRAnnotation>,
	override val modifiers: Set<IRModifier>,
	override val properties: List<IRProperty>,
	override val declarations: List<IRDeclaration>,
	override val superTypes: List<IREntityReference>,
	override val typeParameters: List<IRTypeParameter>,
	override val docString: String?
) : IRExtendable {

	override val isEncryptable = this.isEncryptableOrSomethingSecure()

}

@Serializable
data class IRInterface(
	override val packageName: String?,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	override val annotations: List<IRAnnotation>,
	override val modifiers: Set<IRModifier>,
	override val properties: List<IRProperty>,
	override val declarations: List<IRDeclaration>,
	override val superTypes: List<IREntityReference>,
	override val typeParameters: List<IRTypeParameter>,
	override val docString: String?
) : IRExtendable {

	override val isEncryptable = this.isEncryptableOrSomethingSecure()

}

@Serializable
data class IRObject(
	override val packageName: String?,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	override val annotations: List<IRAnnotation>,
	override val modifiers: Set<IRModifier>,
	override val properties: List<IRProperty>,
	override val declarations: List<IRDeclaration>,
	override val superTypes: List<IREntityReference>,
	override val typeParameters: List<IRTypeParameter>,
	override val docString: String?
) : IRClassDeclaration {
	override val isEncryptable = false
}

/**
 * @return if the object is used as a namespace and not as a singleton.
 */
val IRObject.isNamespaceObject get() =
	IRModifier.DATA !in modifiers && superTypes.all { it.canonicalName == "kotlin.Any" } && declarations.all {
		it is IRObject || it is IRInterface || it is IRClass || it is IREnum || (
			it is IRFunction && it.simpleName == "<init>"
		)
	}

@Serializable
data class IREnum(
	override val packageName: String?,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	override val annotations: List<IRAnnotation>,
	override val modifiers: Set<IRModifier>,
	override val properties: List<IRProperty>,
	override val declarations: List<IRDeclaration>,
	override val docString: String?
) : IRClassDeclaration {
	override val isEncryptable = false
	override val typeParameters: List<IRTypeParameter> = emptyList()
	override val superTypes: List<IREntityReference> = emptyList()
}


@Serializable
data class IREnumEntry(
	override val packageName: String?,
	override val simpleName: String,
	override val parentDeclarations: List<String>,
	override val annotations: List<IRAnnotation>,
	val properties: List<IRProperty>,
	override val docString: String?
) : IREntity, IRDeclaration {
	override val modifiers: Set<IRModifier> = emptySet()
}
