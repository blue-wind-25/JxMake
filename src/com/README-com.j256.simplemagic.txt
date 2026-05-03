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

CHANGES:

    A marker has been added at the top of each Java source file to indicate that this program has
    been modified by the JxMake project.

    Some files and directories that are not strictly required for the running of the program are not
    included here to save space.

    Some '@SuppressWarnings' annotations have been modified and/or added.

    Added features:
        - Improved robustness of MIME type detection.
        - Updated the IanaEntries class to correct IANA DB paths.
        - Updated the function:
              LogBackendType.detectFactory()
          to replace 'newInstance()' with 'getDeclaredConstructor().newInstance()'; this function
          has been deprecated since Java 9.
        - Modified the class:
              MagicEntry.OffsetInfo
          to remove the redundant cast to Long.
        - Slightly updated the file 'com/j256/simplemagic/resources/magic' using the contents of
          '/usr/share/magic' from CentOS Linux release 7.9.2009 (Core), along with manual edits.
          This file can be rebuilt manually by combining the smaller component files from:
              https://github.com/file/file/tree/master/magic/Magdir

    All Javadoc-style comments have been converted to normal Java comments.

    Some minor coding style changes.

----------------------------------------------------------------------------------------------------

A "magic" number package which allows content-type (mime-type) determination from files and byte
arrays.

Copyright (C) 2021 Gray Watson

Licensed under the ISC License (https://opensource.org/licenses/ISC). Please refer to the source
code for more details.

For the full license text, see the '../../3rd_party_library_licenses' folder.

====================================================================================================
