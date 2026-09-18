package com.icure.codegen.ir.declaration

import com.icure.codegen.ir.IREntity

val javaSuperTypes = listOf("Any", "Serializable", "Cloneable", "Comparable", "PrincipalDto")

fun <T : IREntity> Sequence<T>.ignoreJavaSupertypes(): Sequence<T> = filterNot { javaSuperTypes.contains(it.simpleName) }

fun <T : IREntity> List<T>.ignoreJavaSupertypes() = asSequence().ignoreJavaSupertypes()