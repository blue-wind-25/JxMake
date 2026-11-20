# src/ — working notes

JxMake application sources. Paths are relative to this directory unless stated otherwise.

## Build

- Build the app JAR (`jxmake.jar`) with both JDKs, in this order:
  `make jar JDK_VER=8 && make jar JDK_VER=25`. Use `make clean` first when a change adds or removes
  classes or constants. The JDK paths are `JDK_PATH_*` in `Makefile` (`/opt/...`).
- `make dist` builds a release into `../dist_build/` (zip + `jxmake`/`jxmake.bat` launchers).
- The JAR mixes Java 8 classes with Java 25-only classes: the FFM Windows driver installer and the
  USB boot loader programmers (`ExcludeFFM` in `Makefile`). Code that loads optional classes at
  runtime must treat **`LinkageError`** (e.g. `UnsupportedClassVersionError` on Java 8) like
  `ClassNotFoundException`, as `jxm/ugc/ProgExec_RW.java` does.
- `*.java.in` files are the sources; PCPP generates the matching `*.java` (git-ignored; deleted by
  `make clean`). **Edit the `.java.in`, never the generated `.java`.** A stray `*.java.tmp` file is
  a PCPP intermediate.
- Everything under `jxm/`, `org/`, `com/` (including `.txt`/`.md` files) and the `res/` copy of
  `0-JxMake/` and `../docs/` is packed into `jxmake.jar`. Put internal notes in `src/` itself.
- Leave the build outputs `JXM_Main.class`, `jxmake.jar` and `../dist_build/` untracked.

## Run and test

- Run from the classes: `java -cp ".:$(ls ../3rd_party/jars/*.jar | tr '\n' ':')" JXM_Main ...`.
  On Java 22+ add `--enable-native-access=ALL-UNNAMED`. `--en-all-exception-st` prints full
  stack traces.
- Test on **both** Java 8 (degraded: no Pty4J, USB boot loaders or FFM) and Java 25 (full).
- Many `make <test>` targets (e.g. `make xmftest`, the `jse*` targets) open GUI windows on the
  user's desktop and wait for input. Ask before running them, or test with a small script instead.
- JxMake script gotchas: a target block ends with `endtarget`; `echoln` cannot take a function call
  directly, so assign the result to a `local` variable first.

## Conventions

- Shipped code and text (Java, `.jxm`, launchers, `../docs/`, vendored READMEs): at most 100
  columns. Internal `.md` notes may be longer.
- Keep code portable to Linux, macOS, the BSDs and Windows: e.g. `stty -F` is GNU-only (`-f` on
  macOS/BSD), and some commands are shell builtins with no binary (`umask`).
- The GNU Make here is 4.4+: `X +=` with an empty value no longer adds a space.
- When removing an implementation that is not yet proven, comment it out instead of deleting it.

## Commit

The tree is in both git and svn (git history starts at "Initial import from SVN"; older history
is only in `svn log`).

1. `git add` only the files you changed. Commit with a descriptive message ending with the
   `Co-Authored-By: Claude ...` trailer, then `git push`.
2. `svn commit -m 'Follow GIT changes' <same files>` (new files need `svn add` first).

## Related guides

- `UPDATE_3RD_PARTY_JARS.md`: updating the prebuilt JARs in `../3rd_party/jars/`, the launchers'
  `DEP_CP` lists and native-access flags.
- `UPDATE_VENDORED_3RD_PARTY.md`: checking and backporting upstream changes into the vendored
  sources under `org/` and `com/`.
- `../util/CodingStyle.md/formatter/CLAUDE.md`: the code formatter (separate rules).
