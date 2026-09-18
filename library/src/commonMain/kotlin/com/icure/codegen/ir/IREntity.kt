package com.icure.codegen.ir

import kotlinx.serialization.Serializable

@Serializable
sealed interface IREntity : IRNode {
	val packageName: String?
	val simpleName: String
	val parentDeclarations: List<String>
}

val IREntity.canonicalName: String
	get() = buildString {
		packageName?.also {
			append(packageName)
			append(".")
		}
		parentDeclarations.forEach {
			append(it)
			append(".")
		}
		append(simpleName)
	}

val IREntity.rootDeclarationCanonicalName: String get() = buildString {
	packageName?.also {
		append(packageName)
		append(".")
	}
	append(parentDeclarations.firstOrNull() ?: simpleName)
}
