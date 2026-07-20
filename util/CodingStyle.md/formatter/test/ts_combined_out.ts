type Status   = "active" | "inactive" | "pending";
type Combined = Base & Extra;

type LongUnion = FirstOptionName |
                 SecondOptionName |
                 ThirdOptionName;

type AnotherLongUnion = FirstOptionName
                      | SecondOptionName
                      | ThirdOptionName;

function identity<T>(value: T): T
{
    return value;
}

class Container<T extends Comparable<T> = DefaultItem> {} // class Container

interface BaseProps {

    id : string;

} // interface BaseProps

interface Props extends BaseProps {

    label     : string;
    onSelect? : (id: string) => void;
    tags      : readonly string[];

} // interface Props

type Point = {
    x : number;
    y : number;
}; // type Point

type Keys = keyof Point;

enum Color {

    Red,
    Green,
    Blue,

} // enum Color

enum Status2 {

    Active   = 1,
    Inactive = 2,
    Pending  = 3,

} // enum Status2

class Widget extends Base {

    declare public static readonly MAX_COUNT: number;
    protected override readonly cache: Map<string, number>;
    private static instance: Widget;

} // class Widget

class Config {

    private static readonly DEFAULT : string = "en";
    private                 locale  : string;
    protected               count   : number;

} // class Config

@Injectable()
export class UserAuthenticationAndAuditLoggingServiceForEnterpriseApplications {} // class UserAuthenticationAndAuditLoggingServiceForEnterpriseApplications

class MetricsHost {

    @LogPerformanceMetricsAndReportDetailedTimingInformation(
        { threshold: 500, unit: "ms", verbose: true }
    )
    process(): void {}

} // class MetricsHost
