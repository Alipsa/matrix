"""Add braces to if/else statements with single-statement bodies in Groovy files."""
import re

RULE_NAME = "IfStatementBraces"


def _skip_string(text, i):
    """Advance i past a string literal starting at text[i] == '"'. Returns new i."""
    i += 1
    while i < len(text) and text[i] != '"':
        if text[i] == '\\':
            i += 1
        i += 1
    return i


def _find_closing_paren(lines, line_0, start_col):
    """Find the ')' matching the '(' at (line_0, start_col).

    Returns (line_0, col_after_close) or None.
    """
    depth = 0
    for li in range(line_0, len(lines)):
        sc = start_col if li == line_0 else 0
        t = lines[li]
        i = sc
        while i < len(t):
            c = t[i]
            if c == '"':
                i = _skip_string(t, i)
            elif c == '/':
                if i + 1 < len(t) and t[i + 1] == '/':
                    break
            elif c == '(':
                depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0:
                    return (li, i + 1)
            i += 1
    return None


def _find_brace_end(lines, line_0, start_col):
    """Find the '}' matching the '{' at (line_0, start_col).

    Returns (line_0, col_after_close) or None.
    """
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


def _find_stmt_end(lines, line_0, start_col):
    """Find end of a single statement starting at (line_0, start_col).

    The start position should be just after ')' of an if condition.
    If the body is on the next line (same line is empty after '),
    advances to the next non-blank line.

    Returns (end_line_0, end_col, is_block) or None.
    is_block is True if the statement starts with '{'.
    """
    # Skip whitespace on this line, then check next lines if needed
    t = lines[line_0]
    c = start_col
    while c < len(t) and t[c] in ' \t':
        c += 1

    # If rest of this line is empty, go to next non-blank line
    while c >= len(t) and line_0 + 1 < len(lines):
        line_0 += 1
        t = lines[line_0]
        c = 0
        while c < len(t) and t[c] in ' \t':
            c += 1

    if line_0 >= len(lines):
        return None

    # Check for braced block
    if t[c] == '{':
        info = _find_brace_end(lines, line_0, c)
        if info is None:
            return None
        return (info[0], info[1], True)

    # Single statement — find end on this line
    i = c
    while i < len(t):
        ch = t[i]
        if ch == '"':
            i = _skip_string(t, i)
        elif ch == ';':
            return (line_0, i + 1, False)
        elif ch == '/':
            if i + 1 < len(t) and t[i + 1] == '/':
                return (line_0, i, False)
        i += 1
    return (line_0, i, False)


def _extract_body(lines, s_l, s_c, e_l, e_c):
    """Extract non-empty body lines between two positions."""
    body = []
    if e_l == s_l:
        text = lines[s_l][s_c:e_c].strip()
        if text:
            body.append(text)
    else:
        first = lines[s_l][s_c:].rstrip()
        if first:
            body.append(first)
        for i in range(s_l + 1, e_l):
            body.append(lines[i].rstrip())
        last = lines[e_l][:e_c].rstrip()
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


def _wrap_body(lines, s_l, s_c, e_l, e_c, target_indent):
    """Replace statement region with braced version. Returns new end line."""
    body = _extract_body(lines, s_l, s_c, e_l, e_c)
    if not body:
        return e_l
    stripped, _ = _strip_indent(body)
    inner = " " * (target_indent + 2)
    close = " " * target_indent
    wrapped = [inner + b for b in stripped] + [close + "}"]
    del lines[s_l:e_l + 1]
    for i, w in enumerate(wrapped):
        lines.insert(s_l + i, w)
    return s_l + len(wrapped)


def fix(file_path, violations, file_content):
    """Wrap single-statement if/else bodies with braces."""
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
    fixed = 0
    for vl in sorted(violation_lines):
        if _wrap_if_at(lines, vl):
            fixed += 1

    if fixed == 0:
        return {"applied": False, "new_content": None,
                "reason": "no if statements without braces found"}

    return {"applied": True, "new_content": "\n".join(lines),
            "reason": "added braces to %d if/else statement(s)" % fixed}


def _wrap_if_at(lines, if_line_0):
    """Wrap the if statement at if_line_0. Returns True if changed."""
    if if_line_0 >= len(lines):
        return False
    line = lines[if_line_0]
    stripped = line.lstrip()
    if not (stripped.startswith("if ") or stripped.startswith("if(")):
        return False

    base_indent = len(line) - len(stripped)
    try:
        lp = stripped.index('(')
    except ValueError:
        return False
    rp_info = _find_closing_paren(lines, if_line_0, base_indent + lp)
    if rp_info is None:
        return False

    cond_l, body_after_rp = rp_info

    # Find the body statement
    info = _find_stmt_end(lines, cond_l, body_after_rp)
    if info is None:
        return False
    b_l, b_c, is_block = info
    if is_block:
        return False  # already braced

    # Wrap body
    new_end = _wrap_body(lines, b_l, b_c if b_l != cond_l else body_after_rp,
                         b_l, b_c, base_indent + 2)

    # Add " {" to the line with ')'
    cur = lines[cond_l]
    cs = cur.lstrip()
    ci = len(cur) - len(cs)
    try:
        rp_pos = cs.index(')')
    except ValueError:
        return False
    lines[cond_l] = cur[:ci] + cs[:rp_pos + 1] + " {"

    # Handle else clause
    _process_else(lines, new_end + 1, base_indent)
    return True


def _process_else(lines, scan_from, base_indent):
    """Find and wrap an else clause starting from scan_from."""
    for el in range(scan_from, len(lines)):
        s = lines[el].lstrip()
        if not s:
            continue
        if not (s.startswith("else ") or s.startswith("else{") or s == "else"):
            return

        ei = len(lines[el]) - len(s)
        after = s.lstrip("else").strip()

        if after.startswith('{'):
            # Already braced
            ba = len(lines[el]) - len(after) + 1
            be = _find_brace_end(lines, el, ba + 1)
            if be is not None:
                _process_else(lines, be[0] + 1, base_indent)
            return
        else:
            nl = el + 1
            if nl >= len(lines):
                return
            info = _find_stmt_end(lines, nl, 0)
            if info is None:
                return
            bl, bc, ib = info
            if ib:
                # Already braced body on next line
                be = _find_brace_end(lines, bl, bc if bl != nl else 1)
                if be is not None:
                    _process_else(lines, be[0] + 1, base_indent)
                return

            ne = _wrap_body(lines, bl, bc, bl, bc, ei + 2)
            ep = lines[el][:ei]
            lines[el] = ep + "else {"
            _process_else(lines, ne + 1, base_indent)
            return
