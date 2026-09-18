package com.icure.codegen.ir.declaration

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import java.io.File
import com.icure.codegen.ir.IRClass
import com.icure.codegen.ir.IRDeclaration
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
import com.icure.codegen.ir.annotation.toIRAnnotations
import com.icure.codegen.ir.entity.getParentDeclarations
import com.icure.codegen.ir.entity.toIRTypeReference
import com.icure.codegen.ir.fromModifier
import com.icure.codegen.ir.parameter.toIRParameter
import com.icure.codegen.ir.parameter.toIRTypeParameter
import com.icure.codegen.ir.property.toIRProperty
import com.icure.codegen.utils.ENCRYPTABLE_DTO_SIMPLE_NAMES
import com.icure.codegen.utils.ENCRYPTABLE_SIMPLE_NAMES

/**
 * Extracts the KDoc string for a declaration, falling back to parsing the source file
 * when KSP's [KSDeclaration.docString] returns null (KSP2 on K2 compiler does not support docString).
 */
fun KSDeclaration.extractDocString(): String? {
	docString?.let { return it }

	val fileLocation = location as? FileLocation ?: return null
	val lines = try {
		File(fileLocation.filePath).readLines()
	} catch (_: Exception) {
		return null
	}
	val declarationLine = fileLocation.lineNumber - 1

	if (declarationLine !in lines.indices) return null

	// Scan backwards from the declaration to find the end of a KDoc block (*/).
	// Between a KDoc and its declaration, only annotations, modifiers, and blank lines can appear.
	var endLine = -1
	var parenDepth = 0
	for (i in (declarationLine - 1) downTo maxOf(0, declarationLine - 200)) {
		val line = lines[i]
		// Track parentheses in reverse to skip multi-line annotation arguments
		for (ch in line.reversed()) {
			when (ch) {
				')' -> parenDepth++
				'(' -> parenDepth--
			}
		}
		if (parenDepth > 0) continue

		val trimmed = line.trim()
		when {
			trimmed.isEmpty() -> continue
			trimmed.endsWith("*/") -> { endLine = i; break }
			trimmed.startsWith("@") -> continue
			else -> return null
		}
	}
	if (endLine < 0) return null

	// Scan backwards to find the opening /**, rejecting regular block comments
	for (i in endLine downTo 0) {
		val trimmed = lines[i].trim()
		if (trimmed.startsWith("/**")) {
			return lines.subList(i, endLine + 1)
				.joinToString("\n") { it.trim().removePrefix("/**").removePrefix("*").removeSuffix("*/").trimStart() }
				.trim()
				.ifEmpty { null }
		}
		if (trimmed.startsWith("/*")) return null
	}
	return null
}

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
			isEncryptable = false,
			annotations = emptyList()
		),
		modifiers = modifiers.map { IRModifier.fromModifier(it) }.toSet(),
		docString = extractDocString()
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
	docString = extractDocString()
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
	docString = extractDocString()
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
	docString = extractDocString()
)

fun KSClassDeclaration.toIREnum(): IREnum = IREnum(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	annotations = annotations.map { it.toIRAnnotation() }.toList(),
	modifiers = modifiers.map { IRModifier.fromModifier(it) }.toSet(),
	properties = primaryConstructor?.parameters?.map { it.toIRProperty() } ?: emptyList(),
	declarations = declarations.map { it.toIRDeclaration() }.toList(),
	docString = extractDocString()
)

fun KSClassDeclaration.toIREnumEntry(): IREnumEntry = IREnumEntry(
	packageName = packageName.asString(),
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	annotations = annotations.map { it.toIRAnnotation() }.toList(),
	properties = emptyList(),
	docString = extractDocString()
)

fun KSClassDeclaration.toIRTypeReference(isNullable: Boolean, element: KSReferenceElement?): IREntityReference = IRPlainEntityReference(
	packageName = runCatching { packageName.asString() }.getOrDefault("kotlin"), //Because when the package is kotlin, KSP trips on an NPE
	simpleName = simpleName.asString(),
	parentDeclarations = getParentDeclarations(this),
	typeParameters = element?.typeArguments.let { arguments ->
		typeParameters.mapIndexed { idx, it ->
			it.toIRTypeParameter(arguments?.getOrNull(idx)?.type)
		}
	},
	isNullable = isNullable,
	superTypes = superTypes.map { it.toIRTypeReference() }.toList(),
	isEncryptable = isEncryptableOrSomethingSecure(),
	annotations = annotations.toIRAnnotations()
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
			isConstructor = constructorParam != null,
			annotations = irProperty.annotations + (constructorParam?.annotations ?: emptyList())
		)
	}.toList()
}

fun KSClassDeclaration.isEncryptableOrSomethingSecure(): Boolean =
	simpleName.asString() == "ContentDto" || simpleName.asString() == "Content" || superTypes.any {
	(ENCRYPTABLE_DTO_SIMPLE_NAMES + ENCRYPTABLE_SIMPLE_NAMES).contains(it.resolve().declaration.simpleName.asString())
} || superTypes.any { (it.resolve().declaration as? KSClassDeclaration)?.isEncryptableOrSomethingSecure() ?: false }

fun KSType.isEncryptableOrSomethingSecure(): Boolean = (this.declaration as? KSClassDeclaration)?.isEncryptableOrSomethingSecure() == true
