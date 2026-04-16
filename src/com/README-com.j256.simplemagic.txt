====================================================================================================
Java Simple Magic
====================================================================================================

The Java source code files located in the directory 'com/j256/simplemagic' and its subdirectories
were downloaded from the GitHub repository:

    https://github.com/j256/simplemagic
    https://github.com/j256/simplemagic/releases
    https://github.com/j256/simplemagic/archive/refs/tags/simplemagic-1.17.tar.gz

~~~ Last accessed & checked on 2026-01-21        ~~~
~~~ Last GIT commit         on 2022-01-01        ~~~
~~~ Last GIT release tag    on 2021-12-30 (1.17) ~~~

----------------------------------------------------------------------------------------------------

Some files and directories that are not strictly required for the running of the program are not
included here to save space.

Some '@SuppressWarnings' annotations have been modified and/or added.

The IanaEntries class has been modified to fix the IANA DB paths.

The class:
    MagicEntry.OffsetInfo
has been modified to remove the redundant cast to Long.

The function:
    LogBackendType.detectFactory()
has been modified to replace 'newInstance()' with 'getDeclaredConstructor().newInstance()'; the
function is deprecated as of Java 9.

The file 'com/j256/simplemagic/resources/magic' has been slightly updated using the contents of the
file '/usr/share/magic' from CentOS Linux release 7.9.2009 (Core). This file can be updated/rebuilt
manually by combining the many smaller files from:
    https://github.com/file/file/tree/master/magic/Magdir

----------------------------------------------------------------------------------------------------

A "magic" number package which allows content-type (mime-type) determination from files and byte
arrays.

Copyright (C) 2021 Gray Watson

Licensed under the ISC License (https://opensource.org/licenses/ISC). Please refer to the source
code for more details.

For the full license text, see the '../../3rd_party_library_licenses' folder.

====================================================================================================
