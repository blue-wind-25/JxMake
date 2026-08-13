/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function matchCountLabel(searchMatches: SearchMatches, t: (key: string) => string) {
    const matchCount = `${searchMatches.items.length} ${
        searchMatches.items.length === 1
            ? t("search.singleResult")
            : t("search.multipleResults")
    }`;
    return matchCount;
}

function nestedHoleAcrossLines(a: number, b: number) {
    const label = `total: ${
        a + b
    } items`;
    return label;
}
