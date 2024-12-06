package com.icure.codegen.ir.functions

@JvmInline
value class PascalCaseString(override val value: String): Casing {
    companion object {
        val regex = "^[A-Z][a-z0-9]*(?:[A-Z][a-z0-9]*)*$".toRegex()
    }

    init {
        require(regex.matches(value)) { "Invalid PascalCaseString: $value" }
    }
}

@JvmInline
value class CamelCaseString(override val value: String): Casing, PascalCaseStringConvertible {
    companion object {
        val regex = "^[a-z]+(?:[A-Z0-9][a-z0-9]*)*$".toRegex()
    }

    init {
        require(regex.matches(value)) { "Invalid CamelCaseString: $value" }
    }

    override fun toPascalCase(): PascalCaseString = PascalCaseString(
        value
            .replaceFirstChar { firstCharacter -> firstCharacter.uppercase() }
    )
}

@JvmInline
value class SnakeCaseString(override val value: String): Casing, PascalCaseStringConvertible {
    companion object {
        val regex = "^[a-z]+(?:_[a-z0-9]+)*$".toRegex()
    }

    init {
        require(regex.matches(value)) { "Invalid SnakeCaseString: $value" }
    }

    override fun toPascalCase(): PascalCaseString = PascalCaseString(
        value
            .split("_")
            .joinToString("") { word -> word.replaceFirstChar { firstCharacter -> firstCharacter.uppercase() } }
    )
}

@JvmInline
value class ScreamingSnakeCaseString(override val value: String): Casing, PascalCaseStringConvertible {
    companion object {
        val regex = "^[A-Z]+(?:_*[A-Z0-9]*)*\$".toRegex()
    }

    override fun toPascalCase(): PascalCaseString = PascalCaseString(
        value
            .split("_")
            .joinToString("") { word -> word.lowercase().replaceFirstChar { firstCharacter -> firstCharacter.uppercase() } }
    )
}

@JvmInline
value class AllCapsString(override val value: String): Casing, PascalCaseStringConvertible {
    companion object {
        val regex = "^[A-Z]+(?:([A-Z0-9])+)*$".toRegex()
    }

    override fun toPascalCase(): PascalCaseString = PascalCaseString(
        value.lowercase().replaceFirstChar { firstCharacter -> firstCharacter.uppercase() }
    )
}

@JvmInline
value class AllLowerCaseString(override val value: String): Casing, PascalCaseStringConvertible {
    companion object {
        val regex = "^[a-z]+(?:([a-z0-9])+)*$".toRegex()
    }

    override fun toPascalCase(): PascalCaseString = PascalCaseString(
        value.replaceFirstChar { firstCharacter -> firstCharacter.uppercase() }
    )
}

@JvmInline
value class VersionString(override val value: String): Casing {
    companion object {
        val regex = "^V(?:_*[0-9]*)*\$".toRegex()
    }

    init {
        require(regex.matches(value)) { "Invalid VersionString: $value" }
    }
}

@JvmInline
value class KebabCaseString(override val value: String): Casing, PascalCaseStringConvertible {
    companion object {
        val regex = "^[a-z]+(?:-[a-z0-9]+)*$".toRegex()
    }

    init {
        require(regex.matches(value)) { "Invalid KebabCaseString: $value" }
    }

    override fun toPascalCase(): PascalCaseString = PascalCaseString(
        value
            .split("-")
            .joinToString("") { word -> word.replaceFirstChar { firstCharacter -> firstCharacter.uppercase() } }
    )
}

sealed interface PascalCaseStringConvertible {
    fun toPascalCase(): PascalCaseString
}

sealed interface Casing {
    val value: String
}

/**
 * Converts a string to Casing
 *
 * Order matters in the `when` statement. It should be ordered from the most specific to the most generic.
 */
private fun String.toCasing(): Casing = when {
    VersionString.regex.matches(this) -> VersionString(this)
    AllCapsString.regex.matches(this) -> AllCapsString(this)
    AllLowerCaseString.regex.matches(this) -> AllLowerCaseString(this)
    ScreamingSnakeCaseString.regex.matches(this) -> ScreamingSnakeCaseString(this)
    SnakeCaseString.regex.matches(this) -> SnakeCaseString(this)
    KebabCaseString.regex.matches(this) -> KebabCaseString(this)
    CamelCaseString.regex.matches(this) -> CamelCaseString(this)
    PascalCaseString.regex.matches(this) -> PascalCaseString(this)
    else -> throw IllegalArgumentException("Invalid casing: $this")
}

/**
 * Converts a string to PascalCase cased string
 *
 * Exceptions:
 * - If the string is a version (VX_Y_Z) where X|Y|Z are MAJOR|MINOR|PATCH, it returns the same string. It cannot be converted to PascalCase since it'd be confusing to remove separators between the MAJOR, MINOR, and PATCH numbers
 * - If the string is already PascalCase, it returns the same string
 */
fun String.toPascalCase(): String = when (val casing = toCasing()) {
    is PascalCaseStringConvertible -> casing.toPascalCase()
    is VersionString -> casing
    is PascalCaseString -> casing
}.value