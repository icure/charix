package com.icure.codegen.ir.declaration

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.icure.codegen.generator.DtoGeneratorsOptions.ENCRYPTABLE_DTO_SIMPLE_NAMES
import com.icure.codegen.generator.DtoGeneratorsOptions.ENCRYPTABLE_SIMPLE_NAMES
import com.icure.codegen.ir.IRClass
import com.icure.codegen.ir.IRDeclaration
import com.icure.codegen.ir.IREntity
import com.icure.codegen.ir.IREntityReference
import com.icure.codegen.ir.IREnum
import com.icure.codegen.ir.IREnumEntry
import com.icure.codegen.ir.IRFunction
import com.icure.codegen.ir.IRInterface
import com.icure.codegen.ir.IRModifier
import com.icure.codegen.ir.IRObject
import com.icure.codegen.ir.IRPlainEntityReference
import com.icure.codegen.ir.IRProperty
import com.icure.codegen.ir.annotation.toIRAnnotation
import com.icure.codegen.ir.entity.getParentDeclarations
import com.icure.codegen.ir.entity.toIRTypeReference
import com.icure.codegen.ir.parameter.toIRParameter
import com.icure.codegen.ir.parameter.toIRTypeParameter
import com.icure.codegen.ir.property.toIRProperty

val javaSuperTypes = listOf("Any", "Serializable", "Cloneable", "Comparable", "PrincipalDto")

fun <T : IREntity> Sequence<T>.ignoreJavaSupertypes(): Sequence<T> = filterNot { javaSuperTypes.contains(it.simpleName) }

fun <T : IREntity> List<T>.ignoreJavaSupertypes() = asSequence().ignoreJavaSupertypes()

fun KSDeclaration.toIRDeclaration(): IRDeclaration = when {
	this is KSClassDeclaration && this.classKind == ClassKind.CLASS-> this.toIRClass()
	this is KSClassDeclaration && this.classKind == ClassKind.INTERFACE -> this.toIRInterface()
	this is KSClassDeclaration && this.classKind == ClassKind.OBJECT -> this.toIRObject()
	this is KSClassDeclaration && this.classKind == ClassKind.ENUM_ENTRY -> this.toIREnumEntry()
	this is KSClassDeclaration && this.classKind == ClassKind.ENUM_CLASS -> this.toIREnum()
	this is KSFunctionDeclaration -> this.toIRFunction()
	this is KSPropertyDeclaration -> this.toIRProperty(false)
	else -> throw IllegalStateException("Declaration not supported: ${this::class.qualifiedName}")
}

fun KSFunctionDeclaration.toIRFunction(): IRFunction = IRFunction(
		packageName = packageName.asString(),
		simpleName = simpleName.asString(),
		parentDeclarations = getParentDeclarations(this),
		enclosingClass = (parent as? KSDeclaration)?.simpleName?.asString(),
		annotations = annotations.map { it.toIRAnnotation() }.toList(),
		parameters = parameters.map { it.toIRParameter() },
		returnType = returnType?.toIRTypeReference() ?: IRPlainEntityReference(
			packageName = "kotlin",
			simpleName = "Unit",
			parentDeclarations = emptyList(),
			isNullable = false,
			superTypes = emptyList(),
			isEncryptable = false
		),
		modifiers = modifiers.map { IRModifier.fromModifier(it) }.toSet(),
		docString = docString
	)


fun KSClassDeclaration.toIRClass(): IRClass = IRClass(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	annotations = annotations.map { it.toIRAnnotation() }.toList(),
	modifiers = modifiers.map { IRModifier.fromModifier(it) }.toSet(),
	properties = propertiesToIRProperties(),
	declarations = declarations.filterNot { it is KSPropertyDeclaration }.map { it.toIRDeclaration() }.toList(),
	superTypes = superTypes.mapNotNull {
		val resolved = it.resolve().declaration
		if(resolved is KSClassDeclaration) {
			resolved.toIRTypeReference(false, it.element)
		} else null
	}.toList(),
	typeParameters = typeParameters.map { it.toIRTypeParameter() },
	docString = docString
)

fun KSClassDeclaration.toIRInterface(): IRInterface = IRInterface(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	annotations = annotations.map { it.toIRAnnotation() }.toList(),
	modifiers = modifiers.map { IRModifier.fromModifier(it) }.toSet(),
	properties = getDeclaredProperties().toList().map { it.toIRProperty(false) },
	declarations = declarations.map { it.toIRDeclaration() }.toList(),
	superTypes = superTypes.mapNotNull {
		val resolved = it.resolve().declaration
		if(resolved is KSClassDeclaration) {
			resolved.toIRTypeReference(false, it.element)
		} else null
	}.toList(),
	typeParameters = typeParameters.map { it.toIRTypeParameter() },
	docString = docString
)

fun KSClassDeclaration.toIRObject(): IRObject = IRObject(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	annotations = annotations.map { it.toIRAnnotation() }.toList(),
	modifiers = modifiers.map { IRModifier.fromModifier(it) }.toSet(),
	properties = getDeclaredProperties().toList().map { it.toIRProperty(false) },
	declarations = declarations.map { it.toIRDeclaration() }.toList(),
	superTypes = superTypes.mapNotNull {
		val resolved = it.resolve().declaration
		if(resolved is KSClassDeclaration) {
			resolved.toIRTypeReference(false, it.element)
		} else null
	}.toList(),
	typeParameters = typeParameters.map { it.toIRTypeParameter() },
	docString = docString
)

fun KSClassDeclaration.toIREnum(): IREnum = IREnum(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	annotations = annotations.map { it.toIRAnnotation() }.toList(),
	modifiers = modifiers.map { IRModifier.fromModifier(it) }.toSet(),
	properties = primaryConstructor?.parameters?.map { it.toIRProperty() } ?: emptyList(),
	declarations = declarations.map { it.toIRDeclaration() }.toList(),
	docString = docString
)

fun KSClassDeclaration.toIREnumEntry(): IREnumEntry = IREnumEntry(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	annotations = annotations.map { it.toIRAnnotation() }.toList(),
	properties = emptyList(),
	docString = docString
)

fun KSClassDeclaration.toIRTypeReference(isNullable: Boolean, element: KSReferenceElement?): IREntityReference = IRPlainEntityReference(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	typeParameters = element?.typeArguments.let { arguments ->
		typeParameters.mapIndexed { idx, it ->
			it.toIRTypeParameter(arguments?.getOrNull(idx)?.type)
		}
	},
	isNullable = isNullable,
	superTypes = superTypes.map { it.toIRTypeReference() }.toList(),
	isEncryptable = isEncryptableOrSomethingSecure()
)

fun KSClassDeclaration.propertiesToIRProperties(): List<IRProperty> {
	val constructorParams = primaryConstructor?.parameters?.map {
		it.toIRProperty()
	} ?: emptyList()

	return getDeclaredProperties().filter { property ->
		property.annotations.none {
			it.shortName.asString() == "JsonIgnore"
		} && property.getter?.annotations?.none {
			it.shortName.asString() == "JsonIgnore"
		} != false
	}.map { classProperty ->
		val constructorParam = constructorParams.firstOrNull { it.simpleName == classProperty.simpleName.asString() }
		val irProperty = classProperty.toIRProperty(constructorParam == null)

		irProperty.copy(
			defaultValue = constructorParam?.defaultValue ?: irProperty.defaultValue,
			constructor = constructorParam != null,
			annotations = irProperty.annotations + (constructorParam?.annotations ?: emptyList())
		)
	}.toList()
}

fun KSClassDeclaration.isEncryptableOrSomethingSecure(): Boolean =
	simpleName.asString() == "ContentDto" || simpleName.asString() == "Content" || superTypes.any {
	(ENCRYPTABLE_DTO_SIMPLE_NAMES + ENCRYPTABLE_SIMPLE_NAMES).contains(it.resolve().declaration.simpleName.asString())
} || superTypes.any { (it.resolve().declaration as? KSClassDeclaration)?.isEncryptableOrSomethingSecure() ?: false }

fun KSType.isEncryptableOrSomethingSecure(): Boolean = (this.declaration as? KSClassDeclaration)?.isEncryptableOrSomethingSecure() == true
