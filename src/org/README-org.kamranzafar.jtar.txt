====================================================================================================
JTar
====================================================================================================

The Java source code files located in the directory 'org/kamranzafar/jtar' and its subdirectories
were downloaded from the GitHub repository:

    https://github.com/kamranzafar/jtar
    https://github.com/kamranzafar/jtar/tags
    https://github.com/kamranzafar/jtar/archive/refs/tags/jtar-2.3.tar.gz

~~~ Last accessed & checked on 2026-01-21            ~~~
~~~ Last GIT commit         on 2020-08-19            ~~~
~~~ Last GIT release tag    on 2015-09-14 (jtar-2.3) ~~~

----------------------------------------------------------------------------------------------------

CHANGES:

    A marker has been added at the top of each Java source file to indicate that this program has
    been modified by the JxMake project.

    Some files and directories that are not strictly required for the running of the program are not
    included here to save space.

    Some '@SuppressWarnings' annotations have been modified and/or added.

    Added features:
        - Implemented detection of the 'ustar' magic field in TarInputStream.
        - Updated Octal and TarHeader to use Base-256 binary encoding for file sizes exceeding 8GB.
        - Implemented GNU @LongLink support. TarOutputStream now automatically writes @LongLink
          entries for names or link names exceeding 100 bytes, and TarInputStream transparently
          restores the full path.
        - Switched filename and path handling to UTF-8 in TarHeader, ensuring modern filesystem
          compatibility.
        - Added an overloaded TarHeader.createHeader() to support LF_LINK (hard links) and
          LF_SYMLINK (symbolic links).

    All Javadoc-style comments have been converted to normal Java comments.

    Some minor coding style changes.

----------------------------------------------------------------------------------------------------

JTar is a simple Java Tar library, that provides an easy way to create and read tar files using IO
streams. The API is very simple to use and similar to the java.util.zip package and also supports
UStar format.

Copyright (C) 2012-2020 Kamran Zafar.

Licensed under the Apache License version 2.0. Please refer to the source code for more details.

For the full license text, see the '../../3rd_party_library_licenses' folder.

====================================================================================================
