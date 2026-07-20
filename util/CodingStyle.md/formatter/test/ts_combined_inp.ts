/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

type Status = "active"|"inactive"|"pending";
type Combined = Base&Extra;

type LongUnion = FirstOptionName |
SecondOptionName |
ThirdOptionName;

type AnotherLongUnion = FirstOptionName
| SecondOptionName
| ThirdOptionName;

function identity<T>(value:T):T {
    return value
}

class Container<T extends Comparable<T> = DefaultItem> {}

interface BaseProps {
    id:string
}

interface Props extends BaseProps {
    label:string
    onSelect?:(id:string)=>void
    tags:readonly string[]
}

type Point = {
    x:number
    y:number
}

type Keys = keyof Point;

enum Color {
    Red,
    Green,
    Blue
}

enum Status2 {
    Active=1,
    Inactive=2,
    Pending=3
}

class Widget extends Base {
    declare public static readonly MAX_COUNT:number
    protected override readonly cache:Map<string,number>
    private static instance:Widget
}

class Config {
    private static readonly DEFAULT:string="en"
    private locale:string
    protected count:number
}

@Injectable() export class UserAuthenticationAndAuditLoggingServiceForEnterpriseApplications {}

class MetricsHost {
    @LogPerformanceMetricsAndReportDetailedTimingInformation({threshold: 500, unit: "ms", verbose: true}) process(): void {}
}
