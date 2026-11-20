====================================================================================================
XZ for Java
====================================================================================================

The Java source code files located in the directory 'org/tukaani/xz' and its subdirectories were
originally downloaded from the GitHub repository:

    https://tukaani.org/xz/java.html

    https://github.com/tukaani-project/xz-java
    https://github.com/tukaani-project/xz-java/tags
    https://github.com/tukaani-project/xz-java/archive/refs/tags/v1.12.tar.gz

The original GitHub repository for the Tukaani Project can be accessed from:

    https://tukaani.org

    https://git.tukaani.org
    https://git.tukaani.org/?p=xz-java.git;a=summary

    git clone --branch v1.12 https://git.tukaani.org/xz-java.git

Regarding the XZ Utils backdoor:

    https://tukaani.org/xz-backdoor
    https://boehs.org/node/everything-i-know-about-the-xz-backdoor
    https://gist.github.com/thesamesam/223949d5a074ebc3dce9ee78baad9e27

As far as we know XZ for Java v1.12 is safe. The Java source code from the GitHub repository exactly
matches the Java source code from the original GitHub repository for the Tukaani Project.

~~~ Last accessed & checked on 2026-11-21         ~~~
~~~ Last GIT commit         on 2026-10-29         ~~~
~~~ Last GIT release tag    on 2026-03-01 (v1.12) ~~~

----------------------------------------------------------------------------------------------------

CHANGES:

    A marker has been added at the top of each Java source file to indicate that this program has
    been modified by the JxMake project.

    Some files and directories that are not strictly required for the running of the program are not
    included here to save space.

    All Javadoc-style comments have been converted to normal Java comments.

    Some minor coding style changes.

    Backported from the upstream 'master' branch (not yet in a release as of v1.12); listed in
    upstream order, oldest first:
        - 6925f812 : Fix sign extension when decoding Backward Size
                     (common/DecoderUtil.java).
        - 551c453c, 788e675f, b648c701, bc3d18aa : Reject invalid BCJ filter start offsets in Block
                     headers (BCJDecoder.java, simple/SimpleFilter.java, simple/*.java; the
                     '*Options.java' parts are not applicable because those files are not
                     included here).
        - a515f8c1 : Reject invalid Unpadded Size and Uncompressed Size values in the Index
                     (index/IndexBase.java).
        - 36709e3d : Don't allocate a new ByteBuf on every call to IndexHash.add
                     (index/IndexHash.java; the 'clear()' call is made via 'java.nio.Buffer' so
                     it also links on Java 8).
    Not backported:
        - e7d1175a : SeekableXZInputStream.read fix (SeekableXZInputStream.java is not included).

----------------------------------------------------------------------------------------------------

XZ for Java aims to be a complete implementation of XZ data compression in pure Java.

All the files in this package have been written by Lasse Collin, Igor Pavlov, Brett Okken, and some
other contributors.

From version 1.10 onwards, XZ for Java is licensed under the BSD Zero Clause License (0BSD).

Versions 1.9 and older were released into the public domain and remain so.

The 0BSD license is an extremely permissive license which does not require retaining or reproducing
copyright or license notices when distributing the code, thus in practice there is extremely little
difference to public domain.

This software is provided "as is", without any warranty.

For the full license text, see the '../../3rd_party_library_licenses' folder.

====================================================================================================
