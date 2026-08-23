/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Status values
type Status = "active"|"inactive"|"pending";

/* Long union, author broke after each operator */
type LongUnion = FirstOptionName |
SecondOptionName | // middle option
ThirdOptionName;

function identity<T /* the value's type */>(value:T):T {
    return value
}

interface Props {
    id:string // unique identifier
    // display label
    label:string
}

enum Color {
    Red, // primary
    Green,
    Blue
}

enum Status2 {
    Active=1,
    // paused state
    Inactive=2,
    Pending=3 // terminal state
}

class Widget extends Base {
    // ambient max count
    declare public static readonly MAX_COUNT:number
    protected override readonly cache:Map<string,number> // lookup cache
}

class MetricsHost {
    @LogPerformanceMetricsAndReportDetailedTimingInformation({threshold: 500, unit: "ms", verbose: true}) // heavy metrics decorator
    process(): void {}
}

class LegacyService {
    @ /* keep spaced pending removal */ Deprecated()
    legacy(): void {}
}
