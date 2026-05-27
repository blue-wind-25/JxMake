#!/usr/bin/env python3.12
"""Minimal Markdown-rendering HTTP server."""

import argparse
import os
import posixpath
import sys
import urllib.parse
from http.server import HTTPServer, SimpleHTTPRequestHandler

from markdown_it import MarkdownIt
from mdit_py_plugins.gfm import gfm_plugin
from mdit_py_plugins.tasklists import tasklists_plugin

# gfm_plugin enables tables and strikethrough (GFM extensions).
_md = (
    MarkdownIt()
    .use(gfm_plugin)
    .use(tasklists_plugin)
)

_TEMPLATE = """\
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
</style>
</head>
<body>
{body}
</body>
</html>
"""


class MDRHandler(SimpleHTTPRequestHandler):

    def translate_path(self, path: str) -> str:
        path = urllib.parse.unquote(path).split("?", 1)[0].split("#", 1)[0]
        # Filter out empty segments and ".." to block directory traversal.
        parts = [p for p in posixpath.normpath(path).split("/") if p and p != ".."]
        return os.path.join(self.server.web_root, *parts) if parts else self.server.web_root  # type: ignore[attr-defined]

    def do_GET(self) -> None:
        fs_path = self.translate_path(self.path)

        if os.path.isdir(fs_path):
            url_part = self.path.split("?", 1)[0]
            if not url_part.endswith("/"):
                self.send_response(301)
                self.send_header("Location", url_part + "/")
                self.end_headers()
                return
            self._serve_directory(fs_path)
            return

        if fs_path.endswith(".md") and os.path.isfile(fs_path):
            self._serve_markdown(fs_path)
            return

        super().do_GET()

    def do_HEAD(self) -> None:
        fs_path = self.translate_path(self.path)
        if os.path.isdir(fs_path) or (fs_path.endswith(".md") and os.path.isfile(fs_path)):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            return
        super().do_HEAD()

    def _serve_directory(self, dir_path: str) -> None:
        try:
            entries = sorted(
                os.listdir(dir_path),
                key=lambda n: (not os.path.isdir(os.path.join(dir_path, n)), n.lower()),
            )
        except OSError:
            self.send_error(403, "Permission denied")
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        items: list[str] = []

        if url_path not in ("/", ""):
            items.append('<li><a href="../">../</a></li>')

        for name in entries:
            href = urllib.parse.quote(name)
            if os.path.isdir(os.path.join(dir_path, name)):
                items.append(f'<li><a href="{href}/">{name}/</a></li>')
            else:
                items.append(f'<li><a href="{href}">{name}</a></li>')

        body = (
            f"<h1>Index of {url_path}</h1>\n"
            "<ul>\n" + "\n".join(items) + "\n</ul>"
        )
        self._send_html(_TEMPLATE.format(title=f"Index of {url_path}", body=body))

    def _serve_markdown(self, file_path: str) -> None:
        try:
            with open(file_path, encoding="utf-8") as f:
                text = f.read()
        except OSError:
            self.send_error(404, "File not found")
            return

        rendered = _md.render(text)
        title = os.path.basename(file_path)
        self._send_html(_TEMPLATE.format(title=title, body=rendered))

    def _send_html(self, html: str) -> None:
        data = html.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

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
    args = parser.parse_args()

    web_root = os.path.realpath(args.directory)
    if not os.path.isdir(web_root):
        sys.exit(f"error: {web_root!r} is not a directory")

    server = HTTPServer(("127.0.0.1", args.port), MDRHandler)
    server.web_root = web_root  # type: ignore[attr-defined]

    print(f"Serving {web_root}")
    print(f"Listening on http://localhost:{args.port}/")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down.")


if __name__ == "__main__":
    main()
