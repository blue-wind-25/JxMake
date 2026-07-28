/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
fun IrClass.addFunction(
    name: String,
    isStatic: Boolean = false
): IrSimpleFunction = addFunction {
    this.name = Name.identifier(name)
}.apply {
    if (!isStatic) {
        val thisReceiver = parentAsClass.thisReceiver!!
        parameters = listOf(thisReceiver.copyTo(this, type = thisReceiver.type))
    }
}
