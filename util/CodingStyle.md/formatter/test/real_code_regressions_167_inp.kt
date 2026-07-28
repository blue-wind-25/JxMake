/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
private fun isReferenced(declarations: List<IrDeclaration>): Boolean =
    declarations.any {
        it.hasAnnotation(RuntimeNames.exportTypeInfoAnnotation)
    } || declarations.any {
        it is IrClass && isReferenced(it.declarations)
    }
