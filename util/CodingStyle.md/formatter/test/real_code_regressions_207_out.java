/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

import java.util.Arrays;
import java.util.List;

class AlignmentPaddingArrayLiteralArgs {

    static void f()
    {
        // Pairs of (url, specifier); exactly one may be non-empty
        List<String[]> urlSpecs    = Arrays.asList(
            new String[] { null,
            "" },
            new String[] { "https://example.com/packagename.zip",
            "" },
            new String[] { "ssh://user:pass%20word@example.com/packagename.zip",
            "" },
            new String[] { "https://example.com/name;v=1.1/?query=foo&bar=baz#blah",
            "" },
            new String[] { "git+ssh://git.example.com/MyProject",
            "" },
            new String[] { "git+ssh://git@github.com:pypa/packaging.git",
            "" },
            new String[] { "git+https://git.example.com/MyProject.git@master",
            "" },
            new String[] { "git+https://git.example.com/MyProject.git@v1.0",
            "" },
            new String[] { "git+https://git.example.com/MyProject.git@refs/pull/123/head",
            "" },
            new String[] { "gopher:/foo/com",
            "" },
            new String[] { null,
            "==={ws}arbitrarystring" },
            new String[] { null,
            "({ws}==={ws}arbitrarystring{ws})" },
            new String[] { null,
            "=={ws}1.0" },
            new String[] { null,
            "({ws}=={ws}1.0{ws})" },
            new String[] { null,
            "=={ws}1.0-alpha" },
            new String[] { null,
            "<={ws}1!3.0.0.rc2" },
            new String[] { null,
            ">{ws}2.2{ws},{ws}<{ws}3" },
            new String[] { null,
            "(>{ws}2.2{ws},{ws}<{ws}3)" }
        );
        List<String>   markers     = Arrays.asList(
            null,
            "python_version{ws}>={ws}'3.3'",
            "({ws}python_version{ws}>={ws}\"3.4\"{ws}){ws}and extra{ws}=={ws}\"oursql\"",
            "sys_platform{ws}!={ws}'linux' and(os_name{ws}=={ws}'linux' or python_version{ws}>={ws}'3.3'{ws}){ws}"
        );
        List<String>   whitespaces = Arrays.asList("", " ", "\t");
    }

} // class AlignmentPaddingArrayLiteralArgs
