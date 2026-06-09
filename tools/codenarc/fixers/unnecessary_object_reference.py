"""Fix unnecessary object reference (this.) in Groovy files."""
import re

RULE_NAME = "UnnecessaryObjectReference"


def fix(file_path, violations, file_content):
    """Remove unnecessary this. or qualified references.

    Returns FixResult dict.
    """
    lines = file_content.split("\n")
    changes = 0

    for v in violations:
        if v.get("ruleName") != RULE_NAME:
            continue
        ln = int(v.get("lineNumber", 0))
        if 1 <= ln <= len(lines):
            line = lines[ln - 1]
            # Remove unnecessary this. prefix
            new_line = re.sub(r'\bthis\.', '', line, count=1)
            if new_line != line:
                lines[ln - 1] = new_line
                changes += 1

    if not changes:
        return {"applied": False, "new_content": None, "reason": "no object reference fixes needed"}

    return {
        "applied": True,
        "new_content": "\n".join(lines),
        "reason": f"removed {changes} unnecessary object reference(s)",
    }
