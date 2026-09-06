#!/usr/bin/env python3
"""
Guards Android string resources against the escaping mistake that has now
broken this build twice.

A bare apostrophe in a strings.xml value is invalid, and so is a
double-escaped one. Both fail deep inside the resource merger with
"Invalid unicode escape sequence", reported against a line number in a
dependency's merged values.xml — an error message that points nowhere near
the file that caused it. That distance between cause and report is exactly
why this is a check rather than a convention.

The rule: inside a <string> value, every apostrophe has exactly one
backslash in front of it.
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

STRING = re.compile(r'<string name="([^"]+)">(.*?)</string>', re.S)
APOSTROPHE = re.compile(r"(\\*)'")


def main() -> int:
    problems = []
    checked = 0
    for path in sorted(Path(".").rglob("src/main/res/values*/strings.xml")):
        if "build/" in str(path):
            continue
        checked += 1
        text = path.read_text()
        try:
            ET.parse(path)
        except ET.ParseError as error:
            problems.append(f"{path}: not valid XML - {error}")
            continue
        for match in STRING.finditer(text):
            name, body = match.group(1), match.group(2)
            for found in APOSTROPHE.finditer(body):
                count = len(found.group(1))
                if count != 1:
                    kind = "unescaped" if count == 0 else f"escaped {count} times"
                    problems.append(
                        f"{path}: string/{name} has an apostrophe {kind}; "
                        "it needs exactly one backslash"
                    )

    for problem in problems:
        print(f"::error::{problem}")
    if problems:
        print(f"\n{len(problems)} string resource problem(s).")
        return 1
    print(f"String resources correctly escaped ({checked} file(s)).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
