====================================================================================================
HTTP4J
====================================================================================================

The Java source code files located in the directory 'com/intellectualsites/http' and its
subdirectories were downloaded from the GitHub repository:

    https://github.com/IntellectualSites/HTTP4J
    https://github.com/IntellectualSites/HTTP4J/releases
    https://github.com/IntellectualSites/HTTP4J/archive/refs/tags/1.8.tar.gz

~~~ Last accessed & checked on 2026-01-21       ~~~
~~~ Last GIT commit         on 2026-01-01       ~~~
~~~ Last GIT release tag    on 2025-04-22 (1.8) ~~~

----------------------------------------------------------------------------------------------------

Some files and directories that are not strictly required for the running of the program are not
included here to save space.

Some '@SuppressWarnings' annotations have been modified and/or added.

All uses of '@NotNull' and '@Nullable' from 'org.jetbrains.annotations' have been removed.

These functions have been modified to support custom timeouts:
    HttpClient.execute()
    HttpRequest.executeRequest()

The 'Headers' class has been modified so that these functions are publicly accessible:
    Collection<String> getHeaders()
    List<String> getHeaders(final String key)
    String getHeader(final String key)

----------------------------------------------------------------------------------------------------

HTTP4J is a simple, lightweight and tiny wrapper for Java's HttpURLConnection. It has no external
dependencies and is written for Java 8.

Copyright (C) 2021 IntellectualSites.

Licensed under the MIT License. Please refer to the source code for more details.

For the full license text, see the '../../3rd_party_library_licenses' folder.

====================================================================================================
