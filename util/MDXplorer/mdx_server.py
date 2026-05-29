#!/usr/bin/env python3.12
"""Minimal Markdown-rendering HTTP server."""

import argparse
import datetime
import email.utils
import hashlib
import html
import mimetypes
import os
import posixpath
import sys
import urllib.parse
from http.server import HTTPServer, SimpleHTTPRequestHandler

from markdown_it import MarkdownIt
from mdit_py_plugins.gfm import gfm_plugin
from mdit_py_plugins.tasklists import tasklists_plugin
from pygments import highlight as _hl
from pygments.formatters import HtmlFormatter
from pygments.lexer import RegexLexer, bygroups
from pygments.lexers import get_lexer_by_name, get_lexer_for_filename, TextLexer
from pygments.token import Comment, Keyword, Name, Operator, Text
from pygments.util import ClassNotFound

class _MakefileLexer(RegexLexer):
    name = "Makefile"
    aliases = ["makefile", "make"]
    filenames = ["Makefile", "makefile"]

    tokens = {
        "root": [
            (r"#.*$", Comment),
            (r"^\s*(include|sinclude|ifdef|ifndef|ifeq|ifneq|else|endif)\b", Keyword),
            (
                r"^\s*(\.(PHONY|SUFFIXES|DEFAULT|PRECIOUS|INTERMEDIATE|SECONDARY"
                r"|SECONDEXPANSION|DELETE_ON_ERROR|EXPORT_ALL_VARIABLES"
                r"|NOTPARALLEL|ONESHELL))(:)",
                bygroups(Name.Constant, Name.Constant, Operator),
            ),
            (r"^([^\s:]+)(:)", bygroups(Name.Label, Operator)),
            (r"^\s*([A-Za-z0-9_]+)(\s*)([:+?]?=)", bygroups(Name.Variable, Text, Operator)),
            (r"\$[@<\^\?\*]", Name.Variable),
            (r"\$\([A-Za-z0-9_\-]+(?: [^)]*)?\)", Name.Variable),
            (r"\$\$", Text),
            (r"^\t.*$", Keyword),
            (r".", Text),
        ]
    }


_MAKEFILE_FILENAMES = frozenset({
    "Makefile", "makefile", "GNUmakefile", "BSDmakefile",
})
_MAKEFILE_LANGS = frozenset({"makefile", "make", "mk"})

_formatter = HtmlFormatter(style="default", nowrap=True)
_src_formatter = HtmlFormatter(style="default", linenos="table")

# Pygments CSS (includes linenos td rules), brace-escaped for str.format().
_PYGMENTS_CSS = (
    _src_formatter.get_style_defs(".highlight")
    .replace("{", "{{")
    .replace("}", "}}")
)
# Dark-mode token CSS scoped under .dark, using monokai palette.
_DARK_PYGMENTS_CSS = (
    HtmlFormatter(style="monokai").get_style_defs(".dark .highlight")
    .replace("{", "{{")
    .replace("}", "}}")
)

_TOGGLE_HTML = (
    '<button class="theme-toggle" title="Toggle dark mode"'
    " onclick=\"(function(){var d=document.documentElement.classList.toggle('dark');"
    "localStorage.setItem('theme',d?'dark':'light')})()\"></button>"
)


def _md_highlight(code: str, lang: str, attrs: str) -> str:
    if not lang:
        return ""
    if lang.lower() in _MAKEFILE_LANGS:
        lexer = _MakefileLexer(stripall=True)
    else:
        try:
            lexer = get_lexer_by_name(lang, stripall=True)
        except ClassNotFound:
            return ""
    return f'<pre class="highlight"><code>{_hl(code, lexer, _formatter)}</code></pre>'


_md = (
    MarkdownIt(options_update={"highlight": _md_highlight})
    .use(gfm_plugin)
    .use(tasklists_plugin)
)

_TEMPLATE = (
    """\
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{title}</title>
<script>
(function(){{
  var t = localStorage.getItem('theme');
  if (t === 'dark' || (!t && window.matchMedia('(prefers-color-scheme: dark)').matches))
    document.documentElement.classList.add('dark');
}})();
</script>
<style>
"""
    + _PYGMENTS_CSS
    + "\n"
    + _DARK_PYGMENTS_CSS
    + """\

:root {{
  --bg: #fff; --text: #1f2328; --border: #d0d7de; --muted: #57606a;
  --link: #0969da; --code-bg: #f6f8fa;
  --ln-bg: #f0f2f4; --ln-border: #d0d7de; --ln-text: #8c959f;
}}
html.dark {{
  --bg: #0d1117; --text: #e6edf3; --border: #30363d; --muted: #8b949e;
  --link: #58a6ff; --code-bg: #161b22;
  --ln-bg: #1c2128; --ln-border: #30363d; --ln-text: #6e7681;
}}
body {{
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  max-width: 900px; margin: 2rem auto; padding: 0 1rem;
  line-height: 1.6; background: var(--bg); color: var(--text);
}}
nav.breadcrumb {{
  display: flex; align-items: center; justify-content: space-between; gap: 1em;
  font-size: 1rem; color: var(--muted); margin-bottom: 1.5rem;
  padding: 0.4em 0.8em; background: var(--code-bg); border-radius: 6px;
}}
nav.breadcrumb a {{ color: var(--link); }}
pre {{ background: var(--code-bg); padding: 1em; overflow-x: auto; border-radius: 6px; }}
code {{ background: var(--code-bg); padding: 0.2em 0.4em; border-radius: 3px; font-size: 0.875em; }}
pre > code {{ background: none; padding: 0; font-size: 1em; }}
table {{ border-collapse: collapse; width: 100%; margin: 1em 0; }}
th, td {{ border: 1px solid var(--border); padding: 0.4em 0.8em; text-align: left; }}
th {{ background: var(--code-bg); }}
li.task-list-item {{ list-style: none; margin-left: -1.5em; }}
a {{ color: var(--link); text-decoration: none; }}
a:hover {{ text-decoration: underline; }}
h1, h2, h3 {{ border-bottom: 1px solid var(--border); padding-bottom: 0.3em; margin-top: 1.5em; }}
img {{ max-width: 100%; height: auto; }}
blockquote {{ border-left: 4px solid var(--border); margin: 0; padding: 0 1em; color: var(--muted); }}
.highlighttable {{ width: 100%; border-spacing: 0; }}
.highlighttable td.linenos {{
  width: 1%; white-space: nowrap; vertical-align: top; user-select: none;
  background: var(--ln-bg); color: var(--ln-text);
  border-right: 1px solid var(--ln-border); padding: 0.8em 1em;
}}
.highlighttable td.linenos .linenodiv pre {{ margin: 0; padding: 0; }}
.highlighttable td.code {{ padding: 0; }}
.highlighttable td.code .highlight pre {{ margin: 0; padding: 0.8em 1em; }}
.theme-toggle {{
  flex-shrink: 0; background: transparent; border: 1px solid var(--border);
  border-radius: 6px; cursor: pointer; font-size: 0.875em;
  padding: 0.2em 0.5em; color: var(--text); line-height: 1;
}}
.theme-toggle::before {{ content: "🌙"; }}
html.dark .theme-toggle::before {{ content: "☀️"; }}
</style>
</head>
<body>
{body}
</body>
</html>
"""
)


class MDRHandler(SimpleHTTPRequestHandler):

    def translate_path(self, path: str) -> str:
        path = urllib.parse.unquote(path).split("?", 1)[0].split("#", 1)[0]
        parts = [p for p in posixpath.normpath(path).split("/") if p and p != ".."]
        return os.path.join(self.server.web_root, *parts) if parts else self.server.web_root  # type: ignore[attr-defined]

    def _safe_path(self) -> str | None:
        fs = self.translate_path(self.path)
        real = os.path.realpath(fs)
        root = os.path.realpath(self.server.web_root)  # type: ignore[attr-defined]
        if real != root and not real.startswith(root + os.sep):
            return None
        return fs

    _FAVICON_SVG = (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16">'
        '<rect width="12" height="14" x="2" y="1" rx="1" fill="#0969da"/>'
        '<rect width="6" height="1.5" x="4" y="4" rx="0.5" fill="white"/>'
        '<rect width="6" height="1.5" x="4" y="7" rx="0.5" fill="white"/>'
        '<rect width="4" height="1.5" x="4" y="10" rx="0.5" fill="white"/>'
        '</svg>'
    ).encode()

    def do_GET(self) -> None:
        if self.path == "/favicon.ico":
            self.send_response(200)
            self.send_header("Content-Type", "image/svg+xml")
            self.send_header("Content-Length", str(len(self._FAVICON_SVG)))
            self.send_header("Cache-Control", "max-age=86400")
            self.end_headers()
            self.wfile.write(self._FAVICON_SVG)
            return

        fs_path = self._safe_path()
        if fs_path is None:
            self.send_error(403, "Forbidden")
            return

        if os.path.isdir(fs_path):
            url_part, _, qs = self.path.partition("?")
            if not url_part.endswith("/"):
                self.send_response(301)
                self.send_header("Location", url_part + "/")
                self.end_headers()
                return
            if "listing" not in qs.split("&"):
                for idx_name in ("index.md", "README.md"):
                    idx = os.path.join(fs_path, idx_name)
                    if os.path.isfile(idx):
                        self._serve_markdown(idx, dir_url=url_part + "?listing")
                        return
            self._serve_directory(fs_path)
            return

        if os.path.isfile(fs_path):
            if fs_path.endswith(".md"):
                self._serve_markdown(fs_path)
                return
            lexer = self._lexer_for_file(fs_path)
            if lexer is not None:
                self._serve_source(fs_path, lexer)
                return

        super().do_GET()

    def do_HEAD(self) -> None:
        fs_path = self._safe_path()
        if fs_path is None:
            self.send_error(403, "Forbidden")
            return
        if os.path.isdir(fs_path):
            self.do_GET()
            return
        if os.path.isfile(fs_path):
            if fs_path.endswith(".md") or self._lexer_for_file(fs_path) is not None:
                self.do_GET()  # _send_html omits body for HEAD
                return
        super().do_HEAD()

    def _serve_directory(self, dir_path: str) -> None:
        try:
            entries = sorted(
                os.listdir(dir_path),
                key=lambda n: (not os.path.isdir(os.path.join(dir_path, n)), n.lower()),
            )
            st = os.stat(dir_path)
        except OSError:
            self.send_error(403, "Permission denied")
            return

        etag = self._etag(st)
        if self._check_not_modified(etag, st.st_mtime):
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        rows: list[str] = []

        if url_path not in ("/", ""):
            rows.append('<tr><td><a href="../">../</a></td><td></td><td></td></tr>')

        for name in entries:
            href = urllib.parse.quote(name)
            safe = html.escape(name)
            full = os.path.join(dir_path, name)
            try:
                entry_st = os.stat(full)
                mod = datetime.datetime.fromtimestamp(entry_st.st_mtime).strftime("%Y/%m/%d %H:%M")
            except OSError:
                entry_st = None
                mod = ""
            if os.path.isdir(full):
                rows.append(
                    f'<tr><td><a href="{href}/">{safe}/</a></td>'
                    f'<td style="text-align:right">—</td>'
                    f'<td>{mod}</td></tr>'
                )
            else:
                size = f'{entry_st.st_size:,}' if entry_st is not None else ""
                rows.append(
                    f'<tr><td><a href="{href}">{safe}</a></td>'
                    f'<td style="text-align:right">{size}</td>'
                    f'<td>{mod}</td></tr>'
                )

        safe_url = html.escape(url_path)
        crumb = self._breadcrumb(url_path)
        body = (
            self._nav_bar(crumb) + "\n"
            '<table>\n'
            '<thead><tr><th>Name</th>'
            '<th style="text-align:right">Size (bytes)</th>'
            '<th>Modified</th></tr></thead>\n'
            '<tbody>\n' + "\n".join(rows) + '\n</tbody>\n</table>'
        )
        self._send_html(_TEMPLATE.format(title=f"Index of {safe_url}", body=body), mtime=st.st_mtime, etag=etag)

    def _serve_markdown(self, file_path: str, dir_url: str | None = None) -> None:
        try:
            st = os.stat(file_path)
        except OSError:
            self.send_error(404, "File not found")
            return

        etag = self._etag(st)
        if self._check_not_modified(etag, st.st_mtime):
            return

        try:
            with open(file_path, encoding="utf-8") as f:
                text = f.read()
        except OSError:
            self.send_error(404, "File not found")
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        rendered = _md.render(text)
        crumb = self._breadcrumb(url_path)
        if dir_url:
            safe_url = html.escape(dir_url)
            crumb += f' &nbsp;•&nbsp; <a href="{safe_url}">[directory listing]</a>'
        body = f'{self._nav_bar(crumb)}\n{rendered}'
        title = html.escape(os.path.basename(file_path))
        self._send_html(_TEMPLATE.format(title=title, body=body), mtime=st.st_mtime, etag=etag)

    def _serve_source(self, file_path: str, lexer) -> None:
        try:
            st = os.stat(file_path)
        except OSError:
            self.send_error(404, "File not found")
            return

        etag = self._etag(st)
        if self._check_not_modified(etag, st.st_mtime):
            return

        try:
            with open(file_path, encoding="utf-8", errors="replace") as f:
                text = f.read()
        except OSError:
            self.send_error(404, "File not found")
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        crumb = self._breadcrumb(url_path)
        inner = _hl(text, lexer, _src_formatter)
        body = f'{self._nav_bar(crumb)}\n{inner}'
        title = html.escape(os.path.basename(file_path))
        self._send_html(_TEMPLATE.format(title=title, body=body), mtime=st.st_mtime, etag=etag)

    def _lexer_for_file(self, path: str):
        name = os.path.basename(path)
        if name in _MAKEFILE_FILENAMES or name.endswith((".mk", ".mak")):
            return _MakefileLexer(stripall=True)
        try:
            lexer = get_lexer_for_filename(name, stripall=True)
        except ClassNotFound:
            return None
        return None if isinstance(lexer, TextLexer) else lexer

    def _breadcrumb(self, url_path: str) -> str:
        parts = [p for p in url_path.strip("/").split("/") if p]
        crumbs = ['<a href="/">~</a>']
        for i, part in enumerate(parts):
            href = "/" + "/".join(urllib.parse.quote(p) for p in parts[: i + 1])
            safe = html.escape(part)
            if i == len(parts) - 1:
                crumbs.append(safe)
            else:
                crumbs.append(f'<a href="{href}/">{safe}</a>')
        return " / ".join(crumbs)

    @staticmethod
    def _nav_bar(crumb_html: str, right_html: str = "") -> str:
        right = (f"{right_html} " if right_html else "") + _TOGGLE_HTML
        return (
            f'<nav class="breadcrumb">'
            f'<span>{crumb_html}</span>'
            f'<span style="display:flex;align-items:center;gap:0.6em">{right}</span>'
            f'</nav>'
        )

    @staticmethod
    def _etag(st: os.stat_result) -> str:
        return f'"{int(st.st_mtime * 1_000_000):x}-{st.st_size:x}"'

    def _check_not_modified(self, etag: str, mtime: float) -> bool:
        """Send 304 and return True when the client's cached copy is still fresh."""
        matched = self.headers.get("If-None-Match") == etag
        if not matched:
            ims = self.headers.get("If-Modified-Since")
            if ims:
                try:
                    matched = mtime <= email.utils.parsedate_to_datetime(ims).timestamp() + 1
                except Exception:
                    pass
        if matched:
            self.send_response(304)
            self.send_header("Cache-Control", "no-cache")
            self.send_header("ETag", etag)
            self.end_headers()
        return matched

    def _send_html(self, html_text: str, mtime: float | None = None, status: int = 200, etag: str | None = None) -> None:
        data = html_text.encode("utf-8")
        if etag is None:
            etag = f'"{hashlib.md5(data).hexdigest()}"'
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-cache")
        if mtime is not None:
            self.send_header("Last-Modified", email.utils.formatdate(mtime, usegmt=True))
        self.send_header("ETag", etag)
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(data)

    _INLINE_MIME = frozenset({
        "application/pdf",
        "application/json",
        "application/javascript",
        "application/xhtml+xml",
    })

    def guess_type(self, path: str) -> str:  # type: ignore[override]
        mime, _ = mimetypes.guess_type(path)
        if mime and (
            mime.startswith("text/")
            or mime.startswith("image/")
            or mime in self._INLINE_MIME
        ):
            return mime
        try:
            with open(path, "rb") as f:
                sample = f.read(8192)
        except OSError:
            return mime or "application/octet-stream"
        if b"\x00" not in sample:
            try:
                sample.decode("utf-8")
                return "text/plain; charset=utf-8"
            except UnicodeDecodeError:
                pass
        return mime or "application/octet-stream"

    def end_headers(self) -> None:
        self.send_header("X-Content-Type-Options", "nosniff")
        super().end_headers()

    def send_error(self, code: int, message: str | None = None, explain: str | None = None) -> None:
        self.log_error("%s %s", code, message or "")
        title = f"{code} {message or 'Error'}"
        try:
            url_path = urllib.parse.unquote(getattr(self, "path", "/").split("?", 1)[0])
            crumb = self._breadcrumb(url_path)
        except Exception:
            crumb = '<a href="/">~</a>'
        body_parts = [
            self._nav_bar(crumb),
            f"<h1>{html.escape(title)}</h1>",
        ]
        if explain:
            body_parts.append(f"<p>{html.escape(explain)}</p>")
        self._send_html(
            _TEMPLATE.format(title=html.escape(title), body="\n".join(body_parts)),
            status=code,
        )

    def log_message(self, fmt: str, *args) -> None:
        print(f"[{self.address_string()}] {fmt % args}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Markdown-rendering HTTP server")
    parser.add_argument(
        "-C", "--directory",
        default=os.getcwd(),
        metavar="DIR",
        help="web root directory (default: current working directory)",
    )
    parser.add_argument(
        "-p", "--port",
        type=int,
        default=8080,
        help="port to listen on (default: 8080)",
    )
    parser.add_argument(
        "-b", "--bind",
        default="127.0.0.1",
        metavar="ADDR",
        help="address to bind to (default: 127.0.0.1)",
    )
    args = parser.parse_args()

    web_root = os.path.realpath(args.directory)
    if not os.path.isdir(web_root):
        sys.exit(f"error: {web_root!r} is not a directory")

    server = HTTPServer((args.bind, args.port), MDRHandler)
    server.web_root = web_root  # type: ignore[attr-defined]

    print(f"Serving {web_root}")
    print(f"Listening on http://{args.bind}:{args.port}/")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down.")


if __name__ == "__main__":
    main()
