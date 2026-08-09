function foo()
{
    let staleWatches: Map<Path | undefined, string | undefined> | undefined = new Map(
        [ [undefined, undefined] ]
    );
    let timerToUpdateProgram: any;
}
