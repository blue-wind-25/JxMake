====================================================================================================
jSerialComm
====================================================================================================

The Java and C source code files as well as the compiled JNI library files located in the directory
'org/fazecast/jSerialComm' and its subdirectories were downloaded from the GitHub repository:

    https://fazecast.github.io/jSerialComm

    https://github.com/Fazecast/jSerialComm
    https://github.com/Fazecast/jSerialComm/releases
    https://github.com/Fazecast/jSerialComm/releases/download/v2.11.4/jSerialComm-2.11.4.jar
    https://github.com/Fazecast/jSerialComm/archive/refs/tags/v2.11.4.tar.gz

~~~ Last accessed & checked on 2026-01-21           ~~~
~~~ Last GIT commit         on 2025-11-07           ~~~
~~~ Last GIT release tag    on 2025-11-04 (v2.11.4) ~~~

----------------------------------------------------------------------------------------------------

Some files and directories that are not strictly required for the running of the program are not
included here to save space.

Some '@SuppressWarnings' annotations have been modified and/or added.

The SerialPort class has been modified:
    1. When the program is executed from a JAR file, it will read from the system property
       'jSerialComm.jar.library.path' when loading the native JNI library.
    2. A new method, 'removeShutdownHook()', has been added.

----------------------------------------------------------------------------------------------------

A platform-independent serial port access library for Java.

Copyright (C) 2012-2025 Fazecast, Inc.

Dual-licensed under:
    # The Apache License version 2; or
    # The GNU Lesser General Public License (LGPL) version 3 or later.
Please refer to the source code for more details.

For the full license texts, see the '../../3rd_party_library_licenses' folder.

----------------------------------------------------------------------------------------------------

If required, please read the Android SDK License from this URL:
    https://developer.android.com/studio/terms

The 'android.jar' file included here should be the same as the one downloaded from:
    https://github.com/Sable/android-platforms/tree/master/android-22
    https://github.com/Sable/android-platforms/raw/master/android-22/android.jar

The 'android.jar' file is only used by the Java compiler before deployment on an Android device. It
is not bundled with the resulting application. Once an application is deployed on the device it will
use the 'android.jar' file on the Android device. Please refer to these URLs for more details:
    https://stackoverflow.com/a/55213479
    https://www.cnblogs.com/larack/p/4501078.html

It means the 'android.jar' file is NOT BUNDLED with the JxMake application JAR file 'jxm.jar'.

====================================================================================================
