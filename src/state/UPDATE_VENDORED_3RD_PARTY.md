# Updating the Vendored 3rd Party Sources

Reusable procedure for checking and backporting upstream changes into the 3rd party **source code**
vendored under `src/com/` and `src/org/`. All paths are relative to `repo_root`; commands are run
from `src/` unless stated otherwise. For the prebuilt JARs in `3rd_party/jars/`, see
`src/state/UPDATE_3RD_PARTY_JARS.md` instead.

This file lives in `src/` on purpose: everything under `src/com/` and `src/org/` (including the
`README-*.txt`, `LICENSE.txt`, `AUTHORS.md` and `THANKS.md` files) is packed into `jxmake.jar`
(`JAR_SUB_DIRS` in `src/Makefile`), so a file placed there would be shipped.

## 1. What is vendored

Each library has a `README-<package>.txt` next to its top-level directory. It records the upstream
URLs, the `Last accessed & checked`, `Last GIT commit` and `Last GIT release tag` dates, and every
JxMake modification under `CHANGES:`. **The README is the baseline. Always start from it.**

| Package | README | Upstream | License | Used by (`src/jxm/`) |
|---|---|---|---|---|
| `org/tukaani/xz` | `org/README-org.tukaani.xz.txt` | `tukaani-project/xz-java` | 0BSD | `tool/TarXz.java`, `$xz`, `$unxz`, `$txzdir_rec`, `$untxz_rec` |
| `org/kamranzafar/jtar` | `org/README-org.kamranzafar.jtar.txt` | `kamranzafar/jtar` | Apache 2.0 | `tool/Tar*.java` (all `.tar.*` functions, `$untar_rec`) |
| `org/itadaki/bzip2` | `org/README-org.itadaki.bzip2.txt` | gone (see §5) | MIT | `tool/TarBz2.java`, `$bzip2`, `$bunzip2`, `$tbz2dir_rec`, `$untbz2_rec` |
| `com/intellectualsites/http` | `com/README-com.intellectualsites.http.txt` | `IntellectualSites/HTTP4J` | MIT | `tool/HTTPDownloader.java`, `xb/fci/HTTP.java`, `$http_*` |
| `com/j256/simplemagic` | `com/README-com.j256.simplemagic.txt` | `j256/simplemagic` | ISC | `SysUtil.java`, `$file_mime_type` |

The vendored copies are **stripped**: files not needed at runtime were left out (e.g. XZ has no
`*Options.java` and no `SeekableXZInputStream.java`). They are also **modified**: there is a marker
comment at the top of each file, Javadoc is converted to plain comments, the coding style is
adjusted, and there are functional changes listed in the README.

## 2. Rules

1. **Java 8.** This code is compiled into `jxmake.jar` by the Java 8 build, so every change must
   compile and run on Java 8. Do not take upstream code that needs a newer Java version (e.g.
   `var`, records, `List.of`, `InputStream.readAllBytes`, `String.isBlank`). Also watch for
   **covariant return overrides added in Java 9+**. `ByteBuffer.clear()/flip()/position()/limit()`
   etc. only exist as `ByteBuffer` methods from Java 9, so call them through `java.nio.Buffer`
   (e.g. `((java.nio.Buffer) buf).clear()`).
2. **Backport fixes, not features.** Take bug fixes and hardening, especially anything that
   validates untrusted input, because JxMake extracts downloaded archives. Take a refactoring commit
   only when a wanted fix depends on it, and then only its relevant part. Skip commits that only
   touch files not vendored here, as well as tests, build files, docs, formatting and dependency
   bumps.
3. **Respect JxMake's own changes.** Read the README `CHANGES:` first. Upstream patches will not
   apply verbatim (stripped files, converted comments, restyled code, JxMake features such as
   jtar's base-256/`@LongLink`/UTF-8, or bzip2's stream CRC/large files/trailing garbage). Apply
   them by hand and keep the marker header and the local style.
4. **License.** If upstream changed its license, only proceed when the new license is not stricter
   than LGPL 3. Update the README and `3rd_party_library_licenses/` as needed. If a library is
   listed in `JxMakeAboutStringText()` in `src/jxm/Texts.java`, update that too.
5. **New upstream release.** Re-vendoring a whole new release means redoing every JxMake change
   listed in `CHANGES:`. Prefer backporting individual commits unless the release is large and
   clearly worth it. Ask the user before re-vendoring.
6. Download to `/tmp/<whatever>`.
7. Ask the user when unsure.

## 3. Procedure

### 3.1 Find upstream changes since the baseline

Tags, releases and recent commits:

```sh
R=tukaani-project/xz-java
gh api repos/$R --jq '"archived=\(.archived) pushed=\(.pushed_at[0:10])"'
gh api "repos/$R/tags?per_page=5" --jq '.[].name'
gh api "repos/$R/commits?per_page=10" --jq '.[] | "\(.commit.author.date[0:10]) \(.sha[0:8]) \(.commit.message|split("\n")[0])"'
```

Commits since the README's `Last GIT commit` date that touch library sources. `gh --jq` has no
`--arg`, so filter in Python. Source roots: XZ `^src/org/tukaani/xz/`, the others `^src/main/`:

```sh
src_commits() {   # $1 = owner/repo, $2 = since (ISO date), $3 = path regex
  gh api --paginate "repos/$1/commits?since=$2&per_page=100" --jq '.[].sha' | while read s; do
    gh api "repos/$1/commits/$s" | python3 -I -c '
import sys,json,re
d=json.load(sys.stdin); f=[x["filename"] for x in d.get("files",[]) if re.search(sys.argv[1],x["filename"])]
if f: print(d["commit"]["author"]["date"][:10], d["sha"][:8], d["commit"]["message"].split("\n")[0][:80]); print("      "+" ".join(f))' "$3"
  done; }
src_commits tukaani-project/xz-java 2026-10-30T00:00:00Z '^src/org/tukaani/xz/'
```

**JTar caveat:** the README names the `jtar-2.3` tarball, but the code was actually imported (svn
r813, 2022-12-16) from `master` at 4b669cff (2020-08-19), so it also contains the later `master`
fixes (see its README). Compare against `master`, not against the tag.

### 3.2 Triage

For each commit:

1. Does the touched file exist in the vendored copy? (`ls src/org/tukaani/xz/...`)
2. Is the change already there? JxMake may have fixed it independently; `grep` for the new code.
3. Read the patch:

   ```sh
   gh api repos/$R/commits/<sha> | python3 -I -c '
   import sys,json
   d=json.load(sys.stdin); print(d["commit"]["message"])
   for f in d["files"]: print("---", f["filename"]); print(f.get("patch",""))'
   ```

4. Decide: backport / partially backport / skip, using the rules in §2.

### 3.3 Apply, build, test

1. Apply the changes by hand, keeping the marker header and the local style.
2. Do a clean build for both Java versions (incremental builds can hide stale classes):

   ```sh
   make clean && make jar JDK_VER=8 && make jar JDK_VER=25
   ```

3. Test on **both** Java 8 and Java 25. Run from `src/` with the classes on the classpath:

   ```sh
   CP=".:$(ls ../3rd_party/jars/*.jar | tr '\n' ':')"
   /opt/jdk8u432-b06/bin/java -cp "$CP" JXM_Main run -C <test_dir> -f t.jxm
   /opt/openjdk-25_linux-x64_bin/jdk-25/bin/java --enable-native-access=ALL-UNNAMED -cp "$CP" JXM_Main run -C <test_dir> -f t.jxm
   ```

   Useful script checks (compare outputs with `cmp` / `diff -r`):
   - XZ: `$xz`/`$unxz` and `$txzdir_rec`/`$untxz_rec` round-trips. Also decode files made by the
     system `xz`, including every BCJ filter (`xz --x86=start=0`, `--powerpc=start=4`,
     `--ia64=start=16`, `--arm=start=4`, `--armthumb=start=2`, `--arm64=start=4`,
     `--sparc=start=4`, `--riscv=start=2`). For a fix that rejects bad input, craft a bad file (for
     a Block header change, recompute the header CRC32) and check that both `xz -t` and JxMake
     reject it.
   - bzip2: `$bzip2`/`$bunzip2` and `$tbz2dir_rec`/`$untbz2_rec` round-trips. Also decode system
     `bzip2` output, a concatenated file (`cat a.bz2 b.bz2`) and a file with trailing garbage. For
     those edge cases, check that the result matches the build from before the change.
   - JTar: the `.tar.*` round-trips above plus `$untar_rec` on a GNU `tar` archive with long names
     (> 100 bytes) and symlinks.
   - HTTP4J: `$http_get` / `$http_download` against a public URL.
   - simplemagic: `$file_mime_type` on a few known file types.

   Note: `echoln` cannot take a function call directly; assign it to a `local` variable first.

### 3.4 Document and commit

1. Update the library's README:
   - set `Last accessed & checked`, `Last GIT commit` and `Last GIT release tag`;
   - under `CHANGES:`, list each backported commit (short SHA, title, files) and each skipped
     commit that changes code, with the reason. List them in upstream order, **oldest first**
     (the order they were applied upstream; check with
     `gh api repos/$R/commits/<sha> --jq .commit.committer.date`). Do not list commits that
     cannot matter here (formatting, comments/Javadoc only, tests, build files).

   These READMEs are shipped in `jxmake.jar`, so keep their lines at most 100 columns.
2. Refresh the `Last accessed & checked` date in the READMEs of the libraries that had nothing to
   backport too.
3. Add a dated entry to §5 below.
4. Commit with a descriptive message ending with the `Co-Authored-By: Claude ...` trailer, then
   `git push`, then `svn commit -m 'Follow GIT changes'`.

## 4. Fallbacks

- **bzip2:** there is no maintained fork of jbzip2 (see §5). If a serious bug ever shows up,
  the maintained alternative is Apache Commons Compress
  (`org.apache.commons:commons-compress`). Version 1.28.0 is Java 8 and Apache 2.0, and its
  `compressors/bzip2` package has 10 classes. However:
  - those classes depend on Commons Compress' shared utility classes, and the artifact depends on
    `commons-io`, so vendoring only bzip2 is not a clean lift;
  - it does not carry JxMake's jbzip2 additions (stream CRC, large file support, trailing garbage
    handling), which would need re-checking or porting.

## 5. History / decisions

### 2026-11-21

- **XZ for Java** (still v1.12; master moved to 2026-10-29). Backported, oldest first:
  - 6925f812: Backward Size sign extension;
  - 551c453c + 788e675f + b648c701 + bc3d18aa: BCJ start offset validation; only the `simple/*`
    and `BCJDecoder` parts, because `*Options.java` is not vendored;
  - a515f8c1: Index size validation;
  - 36709e3d: `IndexHash` buffer reuse, via the `java.nio.Buffer` cast.

  Skipped e7d1175a (`SeekableXZInputStream` not vendored). d2f82103 (indentation only) is not
  listed in the README.
  Verified on Java 8 and 25: round-trips, all 8 BCJ filters from `xz` 5.8.4, and a crafted ARM
  start offset 2 file that is rejected ("BCJ filter start offset (2) must be a multiple of 4").
- **HTTP4J** (still 1.8): only dependency/build bumps upstream; nothing to do.
- **simplemagic** (still 1.17): the only source change since 2022 is a sync of its bundled
  simplelogging logger (fluent API, extra backends). It is not a fix and the stripped copy does not
  use it; nothing to do.
- **JTar**: no upstream activity since 2020. The original import (svn r813) turned out to be
  `master` at 4b669cff, not the `jtar-2.3` tarball, so it already had #15
  (`equals(Object)`/`hashCode()`) and #29 (zero-padded numeric fields). The tarball has neither.
  The README now says so. #18 (aace0a3c) is Javadoc-only and is not listed.
- **jbzip2**: `volueinsight/bzip2` is gone (archived 2024-02-14, now 404). Searched GitHub for
  forks and successors. None is maintained:
  - `fredcooke/maven-jbzip2`: 2013, no forks;
  - about ten `*/jbzip2` Google Code export mirrors: frozen in 2015;
  - `MateuszBartosiewicz/bzip2`: 2015 snapshot whose newest commit is from 2011 (= 0.9.1);
  - `scp-fs2open/fsoinstaller`: vendors 0.9.1 unchanged, for Java 1.5;
  - `alexgagnon/jbzip2`: an unrelated bzip2 + `jq` CLI tool, not the library.

  JxMake's copy, with its own fixes, is effectively the maintained one. See §4 for the fallback.
