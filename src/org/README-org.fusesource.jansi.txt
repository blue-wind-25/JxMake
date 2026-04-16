====================================================================================================
Jansi
====================================================================================================

The Java and C source code files as well as the compiled JNI library files located in the directory
'org/fusesource/jansi' and its subdirectories were downloaded from the GitHub repository:

    https://fusesource.github.io/jansi

    https://github.com/fusesource/jansi
    https://github.com/fusesource/jansi/releases
    https://github.com/fusesource/jansi/archive/refs/tags/jansi-2.4.3.tar.gz

~~~ Last accessed & checked on 2026-03-29               ~~~
~~~ Last GIT commit         on 2026-03-27               ~~~
~~~ Last GIT release tag    on 2026-03-27 (jansi-2.4.3) ~~~

NOTE : # Jansi has been merged into JLine 3.x; however, there are currently no plans to embed JLine
         3.x in JxMake.
       # In the near future, as all modern versions of Linux, MacOS, and Windows natively support
         ANSI escape codes for basic text coloring, the use of Jansi may be removed entirely from
         JxMake.

----------------------------------------------------------------------------------------------------

Some files and directories that are not strictly required for the running of the program are not
included here to save space.

Some '@SuppressWarnings' annotations have been modified and/or added.

----------------------------------------------------------------------------------------------------

Jansi is a small Java library that allows you to use ANSI escape codes to format your console output
which works even on Windows.

Copyright (C) 2009-2023 FuseSource, Corp. All rights reserved.

Licensed under the Apache License version 2.0. Please refer to the source code for more details.

For the full license text, see the '../../3rd_party_library_licenses' folder.

----------------------------------------------------------------------------------------------------

Currently, Jansi is only used by JxMake to support ANSI escape codes on Windows.

====================================================================================================
