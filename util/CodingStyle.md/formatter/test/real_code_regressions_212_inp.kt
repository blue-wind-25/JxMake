/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class EqualityAndComparisonCallsTransformer {
    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        val symbol = call.symbol
        symbolToTransformer[symbol]?.let {
            return it(call)
        }

        return when (symbol.owner.name) {
            Name.identifier("compareTo") -> if (doNotIntrinsify) call else transformCompareToMethodCall(call)
            Name.identifier("equals") -> transformEqualsMethodCall(call as IrCall)
            else -> call
        }
    }
}
