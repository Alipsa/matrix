"""Fix unnecessary cast expressions in Groovy files."""
import re

RULE_NAME = "UnnecessaryCast"


def fix(file_path, violations, file_content):
    """Remove unnecessary type casts.

    Returns FixResult dict.
    """
    lines = file_content.split("\n")
    changes = []

    for v in violations:
        if v.get("ruleName") != RULE_NAME:
            continue
        ln = int(v.get("lineNumber", 0))
        if 1 <= ln <= len(lines):
            line = lines[ln - 1]
            # Pattern: (Type)expr -> expr when cast is unnecessary
            # Common patterns: (String)x, (Integer)y, (List)z
            new_line = re.sub(r'\(\s*\w+\s*\)\s*', '', line, count=1)
            if new_line != line:
                changes.append((ln - 1, new_line))

    if not changes:
        return {"applied": False, "new_content": None, "reason": "no cast patterns matched"}

    for idx, new_line in changes:
        lines[idx] = new_line

    return {
        "applied": True,
        "new_content": "\n".join(lines),
        "reason": f"removed {len(changes)} unnecessary cast(s)",
    }
