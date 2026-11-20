# Updating the 3rd Party JARs

Reusable procedure for updating the vendored 3rd party JARs in `3rd_party/jars`. All paths below are
relative to `repo_root`; commands are run from `src/` unless stated otherwise.

This file lives in `src/` on purpose: `make dist` copies `3rd_party/jars/*` wholesale into the
distribution's `libs/` directory, so a file placed there would be shipped.

## 1. Know the build

- `src/Makefile` builds the application JAR (`src/jxmake.jar`). The vendored JARs are listed in
  `ALL_VENDOR_JARS` (`ANSI_JAR`, `SERIALCOMM_JAR`, `ZSTD_JAR`, `RSTEXT_JARS`, `USB_JAR`,
  `PTY_MISC_JARS`) and are all on the `javac` classpath.
- JDK paths are `JDK_PATH_8`, `JDK_PATH_11`, `JDK_PATH_17`, `JDK_PATH_21`, `JDK_PATH_25`. Check that
  each one exists first (`$JDK_PATH_N/bin/javac -version`).
- Build check (must pass both before and after the update):

  ```sh
  make clean && make jar JDK_VER=8 && make jar JDK_VER=25
  ```

  Warning: `make clean` also deletes stray `*.tmp` files in the source tree.

## 2. Rules

1. **Java 8 rule (most important).** A JAR whose current version runs on Java 8 (class-file major
   52 or lower) may only be updated to a version that also runs on Java 8. These are the JARs needed
   to build JxMake with Java 8.
2. JARs that already need Java > 8: if the new version raises the minimum Java version, ask the user
   first. Otherwise, update them.
3. Zstd-jni: strip the native libraries for exotic platforms exactly as described in
   `3rd_party/jars/README.txt`. If the new JAR adds new exotic platforms, ask the user whether to
   remove them too.
4. The application JAR must still build (see §1). If an update breaks the build, do not update that
   JAR. If the 3rd party JARs depend on each other (check the `.pom` `<dependency>` versions), choose
   versions that satisfy those dependencies.
5. Update the URLs and notes in `3rd_party/jars/README.txt`, including the Zstd-jni stripping note.
6. If a license changes, only update when the new license is no stricter than JxMake's (LGPL 3). Add
   any new license file to `3rd_party_library_licenses/` if it is not there yet.
7. Never update to a JAR that needs Java > 25.
8. Download to `/tmp/<whatever>` (for example `/tmp/jxm_jars_update/`).
9. Afterwards, edit `JxMakeAboutStringText()` in `src/jxm/Texts.java` as needed. It lists the
   library names, URLs and licenses, but no versions, so it only changes when a URL or license
   changes or a library is added or removed.
10. Launchers `src/jxmake.sh` and `src/jxmake.bat` (copied into the `make dist` zip): if a JAR is
    added, removed or renamed, update their `DEP_CP` lists. Both pass
    `--enable-native-access=ALL-UNNAMED` on Java 22+. That is enough because every JAR is loaded via
    `-cp` and so belongs to the unnamed module, even though many of them already declare a module
    (`module-info.class` or `Automatic-Module-Name`). Listing module names there only produces
    `WARNING: Unknown module: ...`. **If the launchers (or the JARs) ever move to `--module-path`, the
    native-access JARs' module names must be listed in both launchers.** These are: jansi
    (`org.fusesource.jansi`), jSerialComm (`com.fazecast.jSerialComm`), zstd-jni
    (`com.github.luben.zstd_jni`), java-does-usb (`net.codecrete.usb`), JNA (`com.sun.jna`) and
    JNA-Platform (`com.sun.jna.platform`). Check the actual names in the new JARs first:

    ```sh
    # Works for explicit, multi-release (jansi, jSerialComm) and automatic (JNA) modules alike;
    # `jar --describe-module` fails on the multi-release ones
    /opt/openjdk-25_linux-x64_bin/jdk-25/bin/java --module-path some.jar --list-modules | grep -v '^java\.\|^jdk\.'
    ```

    After `make dist`, run the zip's `jxmake` with `JXMAKE_JAVA=<java 8>` and `JXMAKE_JAVA=<java 25>`.
    Both must work: Java 8 with degraded features (no USB bootloaders, Pty4J or FFM), Java 25 with
    all of them, and no warnings.
11. Ask the user when unsure.

## 3. Handy commands

Latest versions on Maven Central (skip `-alpha`, `-Beta` and `-RC` releases):

```sh
curl -s https://repo1.maven.org/maven2/<group/path>/<artifact>/maven-metadata.xml | grep -oE '<version>[^<]+' | tail
```

Class-file major versions in a JAR (`52` = Java 8, `55` = 11, `61` = 17, `65` = 21, `69` = 25).
`META-INF/versions/*` is ignored, and a single `53` entry is usually just `module-info.class`:

```sh
python3 -I - some.jar <<'EOF'
import sys, zipfile, collections
z = zipfile.ZipFile(sys.argv[1]); c = collections.Counter()
for n in z.namelist():
    if n.endswith('.class') and not n.startswith('META-INF/versions'):
        c[int.from_bytes(z.read(n)[6:8], 'big')] += 1
print(dict(c))
EOF
```

Native libraries in Zstd-jni (to compare against the README's removal list):

```sh
unzip -l zstd-jni-X.jar | grep -E '\.(so|dll|dylib)$' | awk '{print $4}'
```

Runtime smoke test (JDK 25 `jshell` with every vendored JAR on `--class-path`): a Zstd round-trip
(`Zstd.compress`/`Zstd.decompress`) and a Pty4J spawn
(`new com.pty4j.PtyProcessBuilder(new String[]{"/bin/echo","ok"}).start()`).

## 4. Commit

1. `git add` only the files changed by the update. Commit with a descriptive message that ends with
   the `Co-Authored-By: Claude ...` trailer, then `git push`.
2. `svn commit -m 'Follow GIT changes'`.

## 5. History / decisions

### 2026-11-21

- Updated: zstd-jni 1.5.7-11 → 1.5.7-28 (no new exotic platforms; same removal list), pty4j
  0.13.12 → 0.14.0 (Java 11, as before; needs kotlin-stdlib 2.4.20+, slf4j-api 2.0.19+ and JNA
  5.19.1), kotlin-stdlib 2.4.10 → 2.4.21 (Java 8), slf4j-api 2.0.18 → 2.0.20 (Java 8).
- Not updated: RSyntaxTextArea 4.0.2, RSTAUI 4.0.0 and AutoComplete 4.0.0 need Java 11 (class-file
  major 55), so rule 1 blocks them. Stay on 3.6.3 / 3.3.2 / 3.3.3 while JxMake still builds with
  Java 8.
- Already the latest: Jansi 2.4.3, jSerialComm 2.11.4, SpellChecker 3.4.1, Java Does USB 1.3.0
  (Java 25), annotations 26.1.0, JNA / JNA-Platform 5.19.1.
- No license changes, so `Texts.java` was not changed.
