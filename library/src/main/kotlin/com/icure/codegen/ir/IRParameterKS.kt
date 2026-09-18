package com.icure.codegen.ir

import com.google.devtools.ksp.symbol.KSValueParameter
import com.icure.codegen.ir.IRParameter.ParameterSource
import com.icure.codegen.ir.annotation.urlNameOrNull

fun KSValueParameter.extractSourceAndNameOrThrow(): Pair<ParameterSource, String?>? =
	annotations.firstNotNullOfOrNull { ann ->
		when (ann.shortName.asString()) {
			"PathVariable" -> ParameterSource.PATH to null
			"RequestParam" -> ParameterSource.REQUEST to ann.urlNameOrNull()?.takeIf { it.isNotEmpty() }
			"RequestHeader" -> ParameterSource.HEADER to ann.urlNameOrNull()?.takeIf { it.isNotEmpty() }
			"RequestBody" -> ParameterSource.BODY to null
			"RequestPart" -> ParameterSource.PART to ann.urlNameOrNull()?.takeIf { it.isNotEmpty() }
			else -> null
		}
	}
