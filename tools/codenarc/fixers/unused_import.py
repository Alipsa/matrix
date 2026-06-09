"""Fix unused import statements in Groovy files."""
import re

RULE_NAME = "UnusedImport"


def fix(file_path, violations, file_content):
    """Remove unused import lines from the source file.

    Args:
        file_path: path to groovy source file
        violations: list of violation dicts from codenarc xml containing ruleName, fileName, lineNumber
        file_content: original file content string

    Returns:
        FixResult dict with applied bool, new_content str or None, reason str
    """
    lines = file_content.split("\n")
    violation_lines = set()

    for v in violations:
        if v.get("ruleName") != RULE_NAME:
            continue
        ln = int(v.get("lineNumber", 0))
        if ln > 0:
            violation_lines.add(ln)

    if not violation_lines:
        return {"applied": False, "new_content": None, "reason": "no violations found"}

    new_lines = []
    skipped = 0
    for i, line in enumerate(lines, 1):
        if i in violation_lines and line.strip().startswith("import "):
            skipped += 1
            continue
        new_lines.append(line)

    return {
        "applied": True,
        "new_content": "\n".join(new_lines),
        "reason": f"removed {skipped} unused import(s)",
    }
