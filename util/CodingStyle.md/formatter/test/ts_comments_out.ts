// Status values
type Status = "active" | "inactive" | "pending";

/* Long union, author broke after each operator */
type LongUnion = FirstOptionName |
                 SecondOptionName | // middle option
                 ThirdOptionName;

function identity<T /* The value's type */>(value: T): T
{
    return value;
}

interface Props {

    id : string; // Unique identifier
    // Display label
    label : string;
    
} // interface Props

enum Color {

    Red, // Primary
    Green,
    Blue,

} // enum Color

enum Status2 {

    Active = 1,
    // Paused state
    Inactive = 2,
    Pending  = 3, // Terminal state

} // enum Status2

class Widget extends Base {

    // Ambient max count
    declare public static readonly MAX_COUNT: number;

    protected override readonly cache: Map<string, number>; // Lookup cache
} // class Widget

class MetricsHost {

    @LogPerformanceMetricsAndReportDetailedTimingInformation(
        { threshold: 500, unit: "ms", verbose: true }
    ) // Heavy metrics decorator
    process(): void {}

} // class MetricsHost
