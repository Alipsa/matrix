"""Fix missing blank line before class closing brace in Groovy files."""

RULE_NAME = "ClassEndsWithBlankLine"


def fix(file_path, violations, file_content):
    """Add blank line before class closing } where missing.

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
            stripped = line.strip()
            # If the closing brace doesn't have a blank line before it
            if stripped == "}" and ln > 1:
                prev_line = lines[ln - 2]
                if prev_line.strip() and not prev_line.strip().startswith("//"):
                    lines.insert(ln - 1, "")
                    changes += 1

    if not changes:
        return {"applied": False, "new_content": None, "reason": "no blank line fixes needed"}

    return {
        "applied": True,
        "new_content": "\n".join(lines),
        "reason": f"added {changes} blank line(s) before class closing brace",
    }
