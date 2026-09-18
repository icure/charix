package com.icure.codegen.ir

import kotlinx.serialization.Serializable

@Serializable
data class IRMemberReference(
	val location: String,
	val name: String
) : IRNode