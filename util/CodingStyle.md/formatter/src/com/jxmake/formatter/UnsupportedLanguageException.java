/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import java.io.IOException;

/**
 * Thrown when a file's language is recognized ({@link Lang#isScaffoldOnly}) but has no real
 * formatting logic yet -- distinct from "could not infer language from file extension", which
 * stays an ordinary {@link IOException} message as before. Extends {@link IOException} so it
 * flows through every existing per-file error path (`Main.run()`'s per-file `catch
 * (IOException)`, `ServerMode.FormatHandler`'s catch-all-500) without those callers needing a new
 * catch clause -- same disposition STATE_COMMON.md documents for other per-file processing
 * errors (e.g. the `--preserve-tree`/`--root` file-outside-root case).
 */
public final class UnsupportedLanguageException extends IOException {
    private static final long serialVersionUID = 1L;

    public UnsupportedLanguageException(final String language) {
        super("'" + language + "' formatting is not yet implemented (scaffold only -- see the "
                + "job routing table in formatter/CLAUDE.md for the relevant STATE_*.md file)");
    }
}
