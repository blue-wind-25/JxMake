// Regression: 'typeof' was missing from GENERIC_SAFE_KEYWORDS -- a
// type-query operand inside a generic argument list invalidated the whole
// <...> open-stack tracking before the matching '>' was reached.
function instrumentArr(rawTarget: any[])
{
    const mutableTarget = rawTarget as Record<
        (typeof identityMethods)[number],
        any
    >;
    identityMethods.forEach( (key) => {
        mutableTarget[key] = key;
    } );
} // instrumentArr

// Same root cause, single-line form: losing the '<...>' tracking left the
// closing '>' a plain OP token, which then defeated statement-boundary
// detection entirely and merged the following statement onto the same line.
let server: ReturnType<typeof createServer>;
beforeAll( async () => {
    server = createServer( (req, res) => {
        return 1;
    } );
} );
