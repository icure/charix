package com.icure.codegen.ir

import com.google.devtools.ksp.symbol.Modifier
import kotlinx.serialization.Serializable

@Serializable
enum class IRModifier : IRNode {
	PUBLIC,
	PROTECTED,
	PRIVATE,
	INTERNAL,
	EXPECT,
	ACTUAL,
	FINAL,
	OPEN,
	ABSTRACT,
	SEALED,
	CONST,
	EXTERNAL,
	OVERRIDE,
	LATEINIT,
	TAILREC,
	VARARG,
	SUSPEND,
	INNER,
	ENUM,
	ANNOTATION,
	VALUE,
	FUN,
	COMPANION,
	INLINE,
	NOINLINE,
	CROSSINLINE,
	REIFIED,
	INFIX,
	OPERATOR,
	DATA,
	IN,
	OUT;

	companion object {

		fun fromModifier(modifier: Modifier): IRModifier = IRModifier.valueOf(modifier.name)

	}
}
