package com.icure.codegen.ir.utils

// region Kraken Constants
const val KRAKEN_DTO_BASE_PATH = "org.taktik.icure.services.external.rest.v2.dto"
// endregion

// region SDK Constants
const val SDK_BASE_PACKAGE = "com.icure.cardinal.sdk"
const val SDK_API_PACKAGE = "$SDK_BASE_PACKAGE.api"
const val SDK_RAW_PACKAGE = "$SDK_BASE_PACKAGE.api.raw"
const val SDK_MODEL_PACKAGE = "$SDK_BASE_PACKAGE.model"
const val SDK_JS_BASE_PACKAGE = "$SDK_BASE_PACKAGE.js"
const val KRYPTOM_PACKAGE = "com.icure.kryptom"
// endregion

// region Encryptable Constants
val ENCRYPTABLE_SIMPLE_NAMES = listOf("Encryptable", "Encrypted")
val ENCRYPTABLE_DTO_SIMPLE_NAMES = ENCRYPTABLE_SIMPLE_NAMES.map { "${it}Dto" }
// endregion
