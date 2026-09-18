package com.icure.codegen.ir.declaration

import com.icure.codegen.ir.IREntityReference
import com.icure.codegen.models.EncryptableFlavour
import kotlinx.serialization.Serializable

@Serializable
data class IRTypeParameter(
    val name: String,
    val bounds: List<IREntityReference>,
	val value: IREntityReference? = null
) {

	fun toFlavour(flavour: EncryptableFlavour?) = when (flavour) {
		EncryptableFlavour.DECRYPTED -> if(value != null) copy(value = value.toFlavour(flavour)) else this
		EncryptableFlavour.ENCRYPTED -> if(value != null) copy(value = value.toFlavour(flavour)) else this
		null -> this
	}

}
