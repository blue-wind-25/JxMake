/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
function f(configFileName: string, host: any) {
    const getCanonicalFileName = createGetCanonicalFileName(host.useCaseSensitiveFileNames);
    const files = map(configParseResult.fileNames, (f) => getRelativePathFromFile(f));
    const pathOptions = { configFilePath: getNormalizedAbsolutePath(configFileName, host.getCurrentDirectory()), useCaseSensitiveFileNames: host.useCaseSensitiveFileNames };
    const optionMap = serializeCompilerOptions(configParseResult.options, pathOptions);
    const watchOptionMap = configParseResult.watchOptions && serializeWatchOptions(configParseResult.watchOptions);
}
