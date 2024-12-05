package com.icure.codegen.ir

import com.icure.codegen.models.EncryptableFlavour
import kotlinx.serialization.Serializable

@Serializable
data class IRProperty(
    override val packageName: String?,
    override val simpleName: String,
    val irType: IREntityReference,
    override val modifiers: Set<IRModifier>,
    override val annotations: List<IRAnnotation>,
    val getter: IRFunction?,
    val setter: IRFunction?,
    val defaultValue: IRCodeBlock?,
    val constructor: Boolean,
    override val docString: String?
) : IRDeclaration {
    override val parentDeclarations: List<String> = emptyList()

    fun toFlavour(flavour: EncryptableFlavour?) = copy(irType = irType.toFlavour(flavour))

}
