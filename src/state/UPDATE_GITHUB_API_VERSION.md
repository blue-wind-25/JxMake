# Updating the GitHub REST API version

How to move `0-JxMake/lib/GitHubTool.jxm` (and its JSON extractor `jxm/tool/GitHubUtil.java`) to a
new GitHub REST API version while keeping the old one as a fallback. Paths are relative to `src/`.

## State (2026-11-22)

| Version      | Status                                                       |
|--------------|--------------------------------------------------------------|
| `2026-03-10` | newest; the library default; end of support not yet scheduled |
| `2022-11-28` | fallback (`GITHUB_2022=1`); supported until **2028-03-10**    |

- GitHub has **no LTS version**. A version stays supported for 24 months after the next one is
  released. A request naming an unsupported version gets `410 Gone`.
- A request with **no** `X-GitHub-Api-Version` header gets `2022-11-28` (after that is retired,
  the next oldest supported one). So every call must send the header (`GitHub_API_CHeaders2`),
  or `GITHUB_2022` is silently ignored for that call. `/versions` and `/rate_limit` used to send
  none; fixed in `e2814235`.
- None of the `2026-03-10` breaking changes touch the endpoints or fields the library uses. The
  closest is the removal of the top-level `rate` field of `/rate_limit`; the library reads
  `resources.core`.
- `digest` (assets) and `immutable` (releases) are returned by **both** versions. They were added
  to the API, not introduced by `2026-03-10`. `digest` is `null` for assets uploaded before GitHub
  added it (e.g. `arduino/rp2040tools` 1.0.6), so `GitHubUtil` treats both as optional.

## Steps for a new version

1. List the supported versions: `curl -s https://api.github.com/versions`.
2. Read the breaking changes for the new version:
   <https://docs.github.com/en/rest/about-the-rest-api/breaking-changes> and the support table at
   <https://docs.github.com/en/rest/about-the-rest-api/api-versions>. Check each endpoint and field
   the library uses:
   - `GET /versions`, `GET /rate_limit` (`resources.core.{limit,used,remaining,reset}`)
   - `GET /repos/{o}/{r}/tags` (`name`, `zipball_url`, `tarball_url`, `commit.sha`)
   - `GET /repos/{o}/{r}/releases`, `.../releases/latest`, `.../releases/{id}`,
     `.../releases/tags/{tag}` (`id`, `name`, `tag_name`, `prerelease`, `created_at`,
     `tarball_url`, `zipball_url`, `immutable`, `assets[].{size,updated_at,browser_download_url,
     digest}`)
   - `GET /repos/{o}/{r}/commits?path=...` (`commit.author.date`)
   - `GET /repos/{o}/{r}/git/ref/tags/{tag}` (`object.type`, `object.sha`)
3. Edit the `if $getenv('GITHUB_2022', '')` block in `GitHubTool.jxm`. If the old fallback is
   still supported, keep it; if it is retired, decide with the user whether to rename the switch
   (e.g. `GITHUB_2026`) or drop it. Refresh the version notes in the comment above the block.
4. If a field moved or changed type, update `GitHubUtil.java`. Treat new fields as optional
   (`null` or absent), since the fallback version may not return them.
5. Update `../docs/txt/en_US/13-Loadable-Libraries.txt` (the `GitHubTool` entry names both versions
   and the end-of-support date).
6. Build: `make jar JDK_VER=8 && make jar JDK_VER=25`.
7. Test: `make xghtest` runs `../test/GitHubToolTest.jxm` with both versions (`ghtest` and
   `gh22test`; `j*` variants use the JAR). It needs network access and makes about 14
   requests per run. Without `GITHUB_TOKEN` the limit is 60 requests per hour. If the test
   switches to a new fallback, update the `gh22test` export and `ExpVer` in the test.
8. Update this file's state table.

## Test fixtures that may go stale

The test pins a few real releases. If one of them disappears, pick another and update the test:

- `arduino/rp2040tools`: tag `1.0.5` = release id `68223679`; release id `72014276` = tag `1.0.6`
  (also the current latest release, which has no asset digests).
- `earlephilhower/arduino-pico`: release `6.4.0` (assets with `sha256:` digests), tag `5.2.1`
  (lightweight tag pointing at a commit), path `tools/uf2conv.py`.
- `espressif/esptool`: must have more than 30 tags (checks `per_page=100`).
- The error check expects `404` for `arduino/no-such-repo-jxmake-test`.

## Conventions to keep

- **Error result.** On failure, `__gh_check_rest_api_call__` returns
  `{ '' <status_code> <reason_phrase> <message>... }`. The empty first member is **deliberate**:
  every caller (the `gh_get_*` wrappers, `gh_check_rest_api_mh`, the `Download*.jxm` libraries)
  detects a failure with `if !&${Res}<1>`. `&val` only means `val &!= ''` (it does not test for a
  map handle), so a first member such as `404` would pass as a success. The HTTP status is `<2>`.
  `SourceForgeTool.jxm` uses the same convention.
- **Page size.** `gh_get_tags` and `gh_get_assets_from_all_releases` request `per_page=100`
  (GitHub's maximum) and read one page only, i.e. the newest 100. No download library uses them:
  the `Download*.jxm` libraries fetch one release, via `.../releases/latest` or
  `.../releases/tags/{tag}` (an exact tag is how an older version is pinned). Fetching every page
  would need the `Link` response header.
