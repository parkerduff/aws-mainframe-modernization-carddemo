"""Scan the repository for plaintext secrets that should never be committed.

This linter is the last line of defence.  Anything matching one of the
patterns below should either be an example value (placed inside a
specifically-allow-listed file) or removed entirely.
"""
from __future__ import annotations

import logging
import pathlib
import re
import sys
from typing import Iterable, List

LOGGER = logging.getLogger("scan_secrets")

PATTERNS = (
    # Specific historical credentials we want to make sure never come back.
    re.compile(r"172\.31\.21\.124"),
    re.compile(r"\bcarddemousr\b"),
    re.compile(r"\bftpdemo1\b"),
    # Generic high-confidence patterns.
    re.compile(r"AKIA[0-9A-Z]{16}"),
    re.compile(r"ASIA[0-9A-Z]{16}"),
    re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    re.compile(
        r"\b(password|passwd|pwd|secret)\s*[:=]\s*[\"']?[A-Za-z0-9_!@#$%^&*-]{4,}",
        re.IGNORECASE,
    ),
)

# Anything that legitimately needs the literal text in source control.
ALLOW_LIST = {
    pathlib.Path("tests/lint/scan_secrets.py"),
    pathlib.Path("tests/lint/lint_jcl.py"),
    pathlib.Path("config/secrets-config.md"),
    pathlib.Path("CONTRIBUTING.md"),
    pathlib.Path("MODERNIZATION.md"),
    pathlib.Path("db/migrate/init_user_security.sh"),
    pathlib.Path("db/migrate/README.md"),
    pathlib.Path("README.md"),
    pathlib.Path("infrastructure/cloudformation/secrets.yaml"),
    pathlib.Path("tests/integration/test_user_security.py"),
    pathlib.Path("tests/unit/test_signon.py"),
    pathlib.Path(".github/workflows/ci.yml"),
}

EXCLUDE_DIRS = {".git", "node_modules", "sample", "venv", ".venv", "artifacts"}


def iter_text_files(root: pathlib.Path) -> Iterable[pathlib.Path]:
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        if any(part in EXCLUDE_DIRS for part in path.parts):
            continue
        try:
            path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        yield path


def main(argv: List[str]) -> int:
    logging.basicConfig(level=logging.INFO, format="%(message)s")
    root = pathlib.Path.cwd()
    issues: List[str] = []
    scanned = 0
    for path in iter_text_files(root):
        rel = path.relative_to(root)
        if rel in ALLOW_LIST:
            continue
        scanned += 1
        text = path.read_text(encoding="utf-8", errors="ignore")
        for pattern in PATTERNS:
            for match in pattern.finditer(text):
                line_no = text.count("\n", 0, match.start()) + 1
                issues.append(
                    f"{rel}:{line_no}: matches secret pattern "
                    f"{pattern.pattern!r}: {match.group(0)!r}"
                )
    for issue in issues:
        LOGGER.error(issue)
    LOGGER.info("Scanned %d files; found %d potential secrets", scanned, len(issues))
    return 1 if issues else 0


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main(sys.argv[1:]))
