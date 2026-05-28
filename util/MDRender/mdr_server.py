#!/usr/bin/env python3.12
"""Minimal Markdown-rendering HTTP server."""

import argparse
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
from pygments.lexers import get_lexer_by_name, get_lexer_for_filename, TextLexer
from pygments.util import ClassNotFound

# Single formatter instance shared by the MD highlight callback and _serve_source.
_formatter = HtmlFormatter(style="default", nowrap=True)

# Pygments CSS, brace-escaped so it survives str.format() calls on _TEMPLATE.
_PYGMENTS_CSS = (
    _formatter.get_style_defs(".highlight")
    .replace("{", "{{")
    .replace("}", "}}")
)


def _md_highlight(code: str, lang: str, attrs: str) -> str:
    """Highlight callback for markdown-it fenced code blocks."""
    if not lang:
        return ""
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

# Template is built once at import time so the Pygments CSS is inlined.
_TEMPLATE = (
    """\
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{title}</title>
<style>
body {{
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  max-width: 900px;
  margin: 2rem auto;
  padding: 0 1rem;
  line-height: 1.6;
  color: #1f2328;
}}
nav.breadcrumb {{
  font-size: 0.875em;
  color: #57606a;
  margin-bottom: 1.5rem;
  padding: 0.4em 0.8em;
  background: #f6f8fa;
  border-radius: 6px;
}}
nav.breadcrumb a {{ color: #0969da; }}
pre {{
  background: #f6f8fa;
  padding: 1em;
  overflow-x: auto;
  border-radius: 6px;
}}
code {{
  background: #f6f8fa;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 0.875em;
}}
pre > code {{
  background: none;
  padding: 0;
  font-size: 1em;
}}
table {{
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
}}
th, td {{
  border: 1px solid #d0d7de;
  padding: 0.4em 0.8em;
  text-align: left;
}}
th {{ background: #f6f8fa; }}
li.task-list-item {{
  list-style: none;
  margin-left: -1.5em;
}}
a {{ color: #0969da; text-decoration: none; }}
a:hover {{ text-decoration: underline; }}
h1, h2, h3 {{
  border-bottom: 1px solid #d0d7de;
  padding-bottom: 0.3em;
  margin-top: 1.5em;
}}
img {{ max-width: 100%; height: auto; }}
blockquote {{
  border-left: 4px solid #d0d7de;
  margin: 0;
  padding: 0 1em;
  color: #57606a;
}}
"""
    + _PYGMENTS_CSS
    + """
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
        """Resolve URL to FS path; return None if it escapes the web root via a symlink."""
        fs = self.translate_path(self.path)
        real = os.path.realpath(fs)
        root = os.path.realpath(self.server.web_root)  # type: ignore[attr-defined]
        if real != root and not real.startswith(root + os.sep):
            return None
        return fs

    def do_GET(self) -> None:
        fs_path = self._safe_path()
        if fs_path is None:
            self.send_error(403, "Forbidden")
            return

        if os.path.isdir(fs_path):
            url_part = self.path.split("?", 1)[0]
            if not url_part.endswith("/"):
                self.send_response(301)
                self.send_header("Location", url_part + "/")
                self.end_headers()
                return
            index_md = os.path.join(fs_path, "index.md")
            if os.path.isfile(index_md):
                self._serve_markdown(index_md)
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
            mtime = os.stat(dir_path).st_mtime
        except OSError:
            self.send_error(403, "Permission denied")
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        items: list[str] = []

        if url_path not in ("/", ""):
            items.append('<li><a href="../">../</a></li>')

        for name in entries:
            href = urllib.parse.quote(name)
            safe = html.escape(name)
            if os.path.isdir(os.path.join(dir_path, name)):
                items.append(f'<li><a href="{href}/">{safe}/</a></li>')
            else:
                items.append(f'<li><a href="{href}">{safe}</a></li>')

        safe_url = html.escape(url_path)
        body = f"<h1>Index of {safe_url}</h1>\n<ul>\n" + "\n".join(items) + "\n</ul>"
        self._send_html(_TEMPLATE.format(title=f"Index of {safe_url}", body=body), mtime=mtime)

    def _serve_markdown(self, file_path: str) -> None:
        try:
            stat = os.stat(file_path)
            with open(file_path, encoding="utf-8") as f:
                text = f.read()
        except OSError:
            self.send_error(404, "File not found")
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        rendered = _md.render(text)
        crumb = self._breadcrumb(url_path)
        body = f'<nav class="breadcrumb">{crumb}</nav>\n{rendered}'
        title = html.escape(os.path.basename(file_path))
        self._send_html(_TEMPLATE.format(title=title, body=body), mtime=stat.st_mtime)

    def _serve_source(self, file_path: str, lexer) -> None:
        try:
            stat = os.stat(file_path)
            with open(file_path, encoding="utf-8", errors="replace") as f:
                text = f.read()
        except OSError:
            self.send_error(404, "File not found")
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        crumb = self._breadcrumb(url_path)
        inner = _hl(text, lexer, _formatter)
        body = (
            f'<nav class="breadcrumb">{crumb}</nav>\n'
            f'<pre class="highlight"><code>{inner}</code></pre>'
        )
        title = html.escape(os.path.basename(file_path))
        self._send_html(_TEMPLATE.format(title=title, body=body), mtime=stat.st_mtime)

    def _lexer_for_file(self, path: str):
        """Return a Pygments lexer for path, or None if the type is unknown/plain text."""
        try:
            lexer = get_lexer_for_filename(os.path.basename(path), stripall=True)
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

    def _send_html(self, html_text: str, mtime: float | None = None) -> None:
        data = html_text.encode("utf-8")
        etag = f'"{hashlib.md5(data).hexdigest()}"'

        if mtime is not None:
            if self.headers.get("If-None-Match") == etag:
                self.send_response(304)
                self.end_headers()
                return
            ims = self.headers.get("If-Modified-Since")
            if ims:
                try:
                    ims_ts = email.utils.parsedate_to_datetime(ims).timestamp()
                    if mtime <= ims_ts + 1:
                        self.send_response(304)
                        self.end_headers()
                        return
                except Exception:
                    pass

        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
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
