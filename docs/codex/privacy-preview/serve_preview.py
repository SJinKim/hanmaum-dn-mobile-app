#!/usr/bin/env python3
"""Serve the local privacy preview on loopback with the policy site's CSP."""
import argparse
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


class Handler(SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=18088)
    parser.add_argument("--directory", type=Path, default=Path(__file__).resolve().parents[3] / "build/privacy-preview")
    args = parser.parse_args()
    if not (args.directory / "index.html").is_file():
        parser.error("Generate the preview first")
    ThreadingHTTPServer(("127.0.0.1", args.port), partial(Handler, directory=str(args.directory))).serve_forever()
