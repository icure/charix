package com.icure.codegen.ir

import com.icure.codegen.utils.isEncryptableOrSomethingSecure
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
sealed interface IRAnnotationValue : IRNode {

	companion object {
		fun of(value: Nothing?): IRAnnotationValue = IRNull

		fun of(value: String?): IRAnnotationValue = handlingNull(value) {
			IRText(it)
		}

		fun of(value: Long?): IRAnnotationValue = handlingNull(value) {
			IRLong(it)
		}

		fun of(value: Int?): IRAnnotationValue = handlingNull(value) {
			IRInt(it)
		}

		fun of(value: Double?): IRAnnotationValue = handlingNull(value) {
			IRDouble(it)
		}

		fun of(value: Boolean?): IRAnnotationValue = handlingNull(value) {
			IRBoolean(it)
		}

		fun <T> of(value: List<T>?, mapElement: (T) -> IRAnnotationValue) = handlingNull(value) {
			IRList(it.map(mapElement))
		}

		internal inline fun <T : Any> handlingNull(value: T?, block: (T) -> IRAnnotationValue) =
			if (value != null) block(value) else IRNull

	}

}
