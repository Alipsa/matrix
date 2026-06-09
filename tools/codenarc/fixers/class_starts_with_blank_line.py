"""Fix missing blank line after class opening brace in Groovy files."""

RULE_NAME = "ClassStartsWithBlankLine"


def fix(file_path, violations, file_content):
    """Add blank line after class opening { where missing.

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
            # If opening brace is followed immediately by content on next line
            if "{" in stripped and ln < len(lines):
                next_line = lines[ln]
                if next_line.strip() and not next_line.strip().startswith("@"):
                    lines.insert(ln + 1, "")
                    changes += 1

    if not changes:
        return {"applied": False, "new_content": None, "reason": "no blank line fixes needed"}

    return {
        "applied": True,
        "new_content": "\n".join(lines),
        "reason": f"added {changes} blank line(s) after class opening brace",
    }
