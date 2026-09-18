package com.icure.codegen.ir

import com.google.devtools.ksp.symbol.Modifier

fun IRModifier.Companion.fromModifier(modifier: Modifier): IRModifier = IRModifier.valueOf(modifier.name)