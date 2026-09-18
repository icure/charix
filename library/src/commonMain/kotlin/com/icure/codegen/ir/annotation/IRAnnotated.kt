package com.icure.codegen.ir.annotation

import com.icure.codegen.ir.IRAnnotation

interface IRAnnotated {
	val annotations: List<IRAnnotation>
}