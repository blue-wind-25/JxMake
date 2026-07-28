/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Number2String {
    fun compute(delta: Int, kappa: Int, len: Int): Int {
        var _K = 0
        var tmp = delta
        if (tmp <= delta) {
            _K += kappa
            grisuRound(tmp, delta)
            return len
        }
        return -1
    }

    fun grisuRound(a: Int, b: Int) {}
}

fun ConeClassLikeType.directExpansionType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeClassLikeType? = { alias ->
        alias.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        alias.expandedConeType
    },
): ConeClassLikeType? {
    if (this is ConeErrorType) return null
    return null
}
