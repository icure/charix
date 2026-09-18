package com.icure.codegen.ir

import com.icure.codegen.models.EncryptableFlavour
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
    // renamed from `constructor` due to problems on js
    @JsonNames("constructor")
    val isConstructor: Boolean,
    override val docString: String?
) : IRDeclaration {
    override val parentDeclarations: List<String> = emptyList()

    fun toFlavour(flavour: EncryptableFlavour?) = copy(irType = irType.toFlavour(flavour))

}
