package com.icure.codegen.ir

import com.google.devtools.ksp.symbol.KSValueParameter
import com.icure.codegen.ir.IRParameter.ParameterSource
import com.icure.codegen.ir.annotation.urlNameOrNull
import com.icure.codegen.models.EncryptableFlavour
import kotlinx.serialization.Serializable

/**
 * Contains all the information parsed from a parameter from the controller method that are needed to convert it to
 * a parameter of a method of the raw SDK.
 *
 * @param name the name of the variable where the parameter is stored. It's the same as the one in the controller.
 * @param type a [IREntityReference] that describes the type of the parameter. Types that are only available on the kraken
 * (e.g. DTOs) must be converted to SDK types.
 * @param source a [ParameterSource] that defines if the parameter is path parameter, a request parameter, or a body
 * parameter. Parameters without this information are to be ignored, as they may be the Request/Response objects from
 * spring.
 * @param urlName the name of the parameter in the url. If null, [name] will be used.
 * @param defaultValue a [IRCodeBlock] that provides the default value for the parameter
 */
@Serializable
data class IRParameter(
    val name: String,
    val type: IREntityReference,
    val source: ParameterSource?,
    val urlName: String?,
    val defaultValue: IRCodeBlock?,
    val serialName: String?,
    val annotations: List<IRAnnotation> = emptyList()
) : IRNode {

    fun toFlavour(flavour: EncryptableFlavour?) = copy(type = type.toFlavour(flavour))

    enum class ParameterSource(val annotationName: String) {
        PATH("PathVariable"),
        REQUEST("RequestParam"),
        HEADER("HeaderParam"),
        BODY("RequestBody"),
        PART("RequestPart");
    }
}

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


