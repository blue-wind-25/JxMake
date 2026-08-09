function f(options: any, features: number)
{
         if(options.resolvePackageJsonExports)           features |= NodeResolutionFeatures.Exports;
    else if(options.resolvePackageJsonExports === false) features &= ~NodeResolutionFeatures.Exports;
}
