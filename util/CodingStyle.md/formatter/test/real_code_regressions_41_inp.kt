/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
internal actual fun systemProp(
    propertyName: String
): String? =
    try {
        System.getProperty(propertyName)
    } catch (e: SecurityException) {
        null
    }
