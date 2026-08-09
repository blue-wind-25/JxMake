class Foo {

    readonly configFileExistenceInfoCache : Map<string, number> = new Map();
    /** @internal */
    readonly throttledOperations          : ThrottledOperations;

} // class Foo
