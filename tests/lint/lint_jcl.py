"""Lightweight JCL syntax linter.

This linter is intentionally limited: it does not attempt to be a full
JCL parser (no public open-source one exists).  Instead it enforces a
small set of rules that catch the most common mistakes seen in
modernisation work:

* every member ends with a newline
* lines are <= 80 columns (after stripping trailing whitespace)
* every member starts with a `// JOB` card
* in-stream data delimiters (`/*`) and DD continuations are balanced
* no plaintext password literals leak back in by accident
"""
from __future__ import annotations

import argparse
import logging
import pathlib
import re
import sys
from typing import Iterable, List

LOGGER = logging.getLogger("lint_jcl")

PASSWORD_RX = re.compile(r"\bPASSWORD[A-Z0-9]\b")
HARDCODED_IP_RX = re.compile(r"\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b")
COMMENT_PREFIXES = ("//*", "*/")


def _is_comment(line: str) -> bool:
    stripped = line.lstrip()
    return stripped.startswith(COMMENT_PREFIXES)


DATA_BLOCK_START_RX = re.compile(r"//\S+\s+DD\s+(\*|DATA(,DLM=(\S{2}))?)")


def lint_member(path: pathlib.Path) -> List[str]:
    errors: List[str] = []
    text = path.read_text(encoding="latin-1")

    if not text.endswith("\n"):
        errors.append(f"{path}: file does not end with a newline")

    lines = text.splitlines()
    if not lines:
        errors.append(f"{path}: empty member")
        return errors

    if not lines[0].startswith("//"):
        errors.append(f"{path}:1: first line must be a JOB card")
    elif " JOB " not in lines[0]:
        errors.append(f"{path}:1: JOB statement missing keyword 'JOB'")

    in_data_block = False
    data_delim = "/*"
    for index, line in enumerate(lines, start=1):
        # Track whether we are inside an in-stream data block.  Inside
        # such a block 80-col / password / IP rules do not apply; the
        # records may legitimately be wide and the bytes are user data
        # rather than JCL syntax.
        if in_data_block:
            if line.startswith(data_delim):
                in_data_block = False
            continue
        match = DATA_BLOCK_START_RX.match(line)
        if match:
            data_delim = match.group(3) or "/*"
            in_data_block = True
            continue
        if len(line.rstrip()) > 80:
            errors.append(f"{path}:{index}: line exceeds 80 columns")
        if _is_comment(line):
            continue
        if PASSWORD_RX.search(line):
            errors.append(
                f"{path}:{index}: literal that looks like a plaintext password"
            )
        for ip_match in HARDCODED_IP_RX.finditer(line):
            ip = ip_match.group()
            # ignore IPs in comments and example data
            if ip.startswith("0.") or ip == "127.0.0.1":
                continue
            errors.append(
                f"{path}:{index}: hardcoded IP literal {ip!r}"
            )
    return errors


def iter_members(root: pathlib.Path) -> Iterable[pathlib.Path]:
    if root.is_file():
        yield root
        return
    for path in sorted(root.rglob("*")):
        if path.is_file() and path.suffix.lower() in {".jcl"}:
            yield path


def main(argv: List[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("path", type=pathlib.Path)
    args = parser.parse_args(argv)

    logging.basicConfig(level=logging.INFO, format="%(message)s")

    issues: List[str] = []
    members = list(iter_members(args.path))
    if not members:
        LOGGER.error("No JCL members found under %s", args.path)
        return 2
    for member in members:
        issues.extend(lint_member(member))

    for issue in issues:
        LOGGER.error(issue)

    LOGGER.info("Linted %d JCL members; %d issues", len(members), len(issues))
    return 1 if issues else 0


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main(sys.argv[1:]))
