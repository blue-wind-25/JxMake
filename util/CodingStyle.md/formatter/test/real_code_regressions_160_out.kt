/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
fun render(
    type              : ConeKotlinType,
    nullabilityMarker : String          = if(type !is ConeFlexibleType) "" else "?",
): String
{
    return nullabilityMarker
}
