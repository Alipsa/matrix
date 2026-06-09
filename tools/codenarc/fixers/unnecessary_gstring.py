"""Fix unnecessary GString expressions in Groovy files."""
import re

RULE_NAME = "UnnecessaryGString"

_GSTRING_PATTERN = re.compile(r'"\$\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}"')


def fix(file_path, violations, file_content):
    """Replace unnecessary GStrings with simple variable references.

    CodeNarc flags "${x}" when x is the only interpolation.
    Replaces with x.toString() to preserve type safety.
    """
    violation_lines = set()
    for v in violations:
        if v.get("ruleName") != RULE_NAME:
            continue
        ln = int(v.get("lineNumber", 0))
        if ln > 0:
            violation_lines.add(ln - 1)

    if not violation_lines:
        return {"applied": False, "new_content": None, "reason": "no violations found"}

    lines = file_content.split("\n")
    count = 0

    for vl in sorted(violation_lines):
        if vl >= len(lines):
            continue
        new_line, n = _fix_line(lines[vl])
        if n > 0:
            lines[vl] = new_line
            count += n

    if count == 0:
        return {"applied": False, "new_content": None, "reason": "no GString patterns matched"}

    return {
        "applied": True,
        "new_content": "\n".join(lines),
        "reason": f"simplified {count} unnecessary GString(s)",
    }


def _fix_line(line):
    """Replace "${x}" patterns on a single line.

    Handles simple variable references and property accesses.
    Skips expressions containing operators. Returns (new_line, count).
    """
    parts = []
    count = 0
    pos = 0

    while True:
        m = _GSTRING_PATTERN.search(line, pos)
        if not m:
            parts.append(line[pos:])
            break

        expr = m.group(1).strip()

        if any(ch in expr for ch in '+-*/?:&|<>'):
            parts.append(line[pos:m.end()])
            pos = m.end()
            continue

        parts.append(line[pos:m.start()])
        parts.append(f"{expr}.toString()")
        pos = m.end()
        count += 1

    return ("".join(parts), count)
