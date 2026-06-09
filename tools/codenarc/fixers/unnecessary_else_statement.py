"""Remove unnecessary else statements when if-branch returns."""
import re

RULE_NAME = "UnnecessaryElseStatement"


def _skip_string(text, i):
    """Advance i past a string literal starting at text[i] == '"'. Returns new i."""
    i += 1
    while i < len(text) and text[i] != '"':
        if text[i] == '\\':
            i += 1
        i += 1
    return i


def _find_brace_end(lines, line_0, start_col):
    """Find the '}' matching '{' at (line_0, start_col). Returns (l, col) or None."""
    depth = 0
    for li in range(line_0, len(lines)):
        sc = start_col if li == line_0 else 0
        t = lines[li]
        i = sc
        while i < len(t):
            c = t[i]
            if c == '"':
                i = _skip_string(t, i)
            elif c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    return (li, i + 1)
            i += 1
    return None


def _extract_body_lines(lines, s_l, s_c, e_l, e_c):
    """Extract body lines inside a braced block, excluding the braces themselves."""
    body = []
    if e_l == s_l:
        inner = lines[s_l][s_c:e_c - 1].strip()
        if inner:
            body.append(inner)
    else:
        first = lines[s_l][s_c:].rstrip()
        if first:
            body.append(first)
        for i in range(s_l + 1, e_l):
            body.append(lines[i])
        last = lines[e_l][:e_c - 1].rstrip()
        if last:
            body.append(last)
    return body


def _strip_indent(body_lines):
    """Strip common leading whitespace. Returns (lines, indent)."""
    if not body_lines:
        return (body_lines, 0)
    indent = min(len(l) - len(l.lstrip()) for l in body_lines if l.strip()) or 0
    stripped = [l[indent:] if l.strip() else "" for l in body_lines]
    return (stripped, indent)


def fix(file_path, violations, file_content):
    """Remove unnecessary else after if-return/throw/break/continue."""
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
    for vl in sorted(violation_lines, reverse=True):
        if _remove_else(lines, vl):
            count += 1

    if count == 0:
        return {"applied": False, "new_content": None, "reason": "no else patterns matched"}

    return {"applied": True, "new_content": "\n".join(lines),
            "reason": "removed %d unnecessary else(s)" % count}


def _remove_else(lines, else_line_0):
    """Remove the else at else_line_0, keeping its body. Returns True if changed."""
    if else_line_0 >= len(lines):
        return False

    line = lines[else_line_0]
    stripped = line.lstrip()
    if not (stripped.startswith("else ") or stripped.startswith("else{") or stripped == "else"):
        return False

    ei = len(line) - len(stripped)
    after = stripped.lstrip("else").strip()

    if after.startswith('{'):
        # Body on same line: else { ... }
        bc = len(line) - len(after) + 1  # position of '{'
        be = _find_brace_end(lines, else_line_0, bc + 1)
        if be is None:
            return False
        bl, bc_val = be
        body = _extract_body_lines(lines, bl, bc_val if bl != else_line_0 else bc + 1,
                                   bl, bc_val)
    elif after == '':
        # Body on next line(s): else\n{ ... } or else\nstatement
        nl = else_line_0 + 1
        if nl >= len(lines):
            return False
        nt = lines[nl].lstrip()
        nc = nl + len(lines[nl]) - len(nt)
        if nt.startswith('{'):
            be = _find_brace_end(lines, nl, nc + 1)
            if be is None:
                return False
            bl, bc_val = be
            body = _extract_body_lines(lines, bl, nc + 1, bl, bc_val)
        else:
            # Single statement - also wrap it
            body_text = nt.strip()
            body = [body_text] if body_text else []
            bl = nl
            bc_val = len(lines[nl])
    else:
        return False

    if not body:
        return False

    deindented, _ = _strip_indent(body)
    prefix = " " * ei
    new_lines = [prefix + b for b in deindented]

    # Determine the range to replace
    if after.startswith('{'):
        end = bl + 1 if bl != else_line_0 else else_line_0 + 1
    else:
        end = else_line_0 + 2

    del lines[else_line_0:end]
    for i, nl in enumerate(new_lines):
        lines.insert(else_line_0 + i, nl)

    return True
