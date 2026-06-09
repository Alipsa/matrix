"""Fix missing space after opening brace in Groovy files."""
import re

RULE_NAME = "SpaceAfterOpeningBrace"


def fix(file_path, violations, file_content):
    """Add space after opening brace where missing.

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
            # Add space after { if missing (but not for {{ which is double-brace init)
            new_line = re.sub(r'\{([^ ])', r'{ \1', line, count=1)
            if new_line != line:
                lines[ln - 1] = new_line
                changes += 1

    if not changes:
        return {"applied": False, "new_content": None, "reason": "no whitespace fixes needed"}

    return {
        "applied": True,
        "new_content": "\n".join(lines),
        "reason": f"fixed {changes} missing space(s) after opening brace",
    }
