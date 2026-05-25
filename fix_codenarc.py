#!/usr/bin/env python3
"""
Auto-fix CodeNarc violations for matrix-charts.
Processes one rule at a time to avoid line number shifts.
"""

import xml.etree.ElementTree as ET
import os
import re
import shutil
from collections import defaultdict

SRC_MAIN = 'matrix-charts/src/main/groovy'
SRC_TEST = 'matrix-charts/src/test/groovy'

def get_file_path(fname, src_dir):
    path = os.path.join(src_dir, fname)
    if os.path.exists(path):
        return path
    for root_dir, dirs, files in os.walk(src_dir):
        if fname in files:
            return os.path.join(root_dir, fname)
    return None

def read_file(path):
    with open(path, 'r') as f:
        return f.readlines()

def write_file(path, lines):
    with open(path, 'w') as f:
        f.writelines(lines)

def parse_violations(report_type):
    tree = ET.parse(f'matrix-charts/build/reports/codenarc/{report_type}.xml')
    root = tree.getroot()
    violations = defaultdict(list)
    for pkg in root.findall('Package'):
        for f in pkg.findall('File'):
            fname = f.get('name')
            for v in f.findall('Violation'):
                rule = v.get('ruleName')
                line = int(v.get('lineNumber'))
                src = v.find('SourceLine')
                msg = v.find('Message')
                violations[fname].append({
                    'rule': rule,
                    'line': line,
                    'source': src.text if src is not None else '',
                    'message': msg.text if msg is not None else ''
                })
    return violations


def fix_unnecessary_gstring(lines, violations):
    """Replace unnecessary double-quoted strings with single quotes."""
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        msg = v['message']
        # Extract string from message: "The String '...' can be wrapped..."
        m = re.search(r"The String '(.+?)' can be wrapped in single quotes", msg)
        if not m:
            m = re.search(r"The String '(.+?)' can be wrapped", msg)
        if not m:
            continue
        content = m.group(1)
        # Skip if content has problematic escape sequences for single quotes
        # In single quotes, \n is literal backslash-n, not newline
        # But \\ is still backslash. We skip strings with \ followed by anything except \"
        # Actually, let's be conservative: skip if content contains backslash
        if '\\' in content:
            continue
        old_line = lines[idx]
        # Replace "content" with 'content'
        # Need to be careful to match exact occurrence
        pattern = '"' + re.escape(content) + '"'
        new_line = old_line.replace(pattern, "'" + content + "'")
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_unnecessary_dot_class(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        new_line = old_line.replace('.class', '')
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_space_after_comma(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # Add space after commas not followed by space/newline
        # Be careful not to change commas inside strings
        # Simple approach: process character by character, tracking string state
        new_chars = []
        in_double_string = False
        in_single_string = False
        escaped = False
        for i, ch in enumerate(old_line):
            if ch == '\\' and (in_double_string or in_single_string):
                escaped = True
                new_chars.append(ch)
                continue
            if escaped:
                escaped = False
                new_chars.append(ch)
                continue
            if ch == '"' and not in_single_string:
                in_double_string = not in_double_string
                new_chars.append(ch)
                continue
            if ch == "'" and not in_double_string:
                in_single_string = not in_single_string
                new_chars.append(ch)
                continue
            if ch == ',' and not in_double_string and not in_single_string:
                new_chars.append(ch)
                # Add space if next non-space char exists and isn't newline
                if i + 1 < len(old_line) and old_line[i + 1] not in ' \n\r\t':
                    new_chars.append(' ')
                continue
            new_chars.append(ch)
        new_line = ''.join(new_chars)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_unnecessary_to_string(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        new_line = old_line.replace('.toString()', '')
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_explicit_call_to_compare_to(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # Patterns like: assertEquals(0, expr.compareTo(other))
        # assertTrue(expr.compareTo(other) == 0)
        # Replace compareTo with <=>
        new_line = re.sub(r'\bcompareTo\b', '<=>', old_line)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_explicit_call_to_plus(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # base.plus(override) -> base + override
        new_line = re.sub(r'\.plus\(([^)]+)\)', r' + (\1)', old_line)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_unnecessary_package_reference(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        msg = v['message']
        # Extract package from message
        m = re.search(r"The ([\w.]+) class was explicitly imported", msg)
        if m:
            pkg = m.group(1)
            # Remove package prefix from constructor or type references
            new_line = old_line.replace('new ' + pkg + '(', 'new ' + pkg.split('.')[-1] + '(')
            new_line = new_line.replace(pkg + '.', '')
        else:
            # Generic: find java.xxx.Yyy patterns
            new_line = re.sub(r'\bjava\.(?:awt|util|lang|io|net|time|math|nio|security|text|sql|beans|rmi|javax?)\.\w+\.', '', old_line)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_space_after_comment(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # Replace //foo with // foo, but not ///foo or //  foo
        new_line = re.sub(r'//([^\s/])', r'// \1', old_line)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_unnecessary_semicolon(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # Remove trailing semicolons, but not inside strings
        # Simple: strip trailing semicolon and whitespace
        stripped = old_line.rstrip()
        if stripped.endswith(';'):
            # Check it's not part of a for loop
            if not stripped.startswith('for '):
                new_line = stripped[:-1].rstrip() + '\n'
                lines[idx] = new_line
                fixed += 1
    return fixed


def fix_consecutive_blank_lines(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        # Find and remove one blank line from consecutive blank lines
        # Look around the reported line
        for i in range(max(0, idx - 2), min(len(lines), idx + 3)):
            if lines[i].strip() == '' and i + 1 < len(lines) and lines[i + 1].strip() == '':
                del lines[i]
                fixed += 1
                break
    return fixed


def fix_unused_import(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        del lines[idx]
        fixed += 1
    return fixed


def fix_unnecessary_groovy_import(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        del lines[idx]
        fixed += 1
    return fixed


def fix_unnecessary_bigdecimal_instantiation(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # new BigDecimal('0.25') -> 0.25
        new_line = re.sub(r"new\s+BigDecimal\s*\(\s*['\"]([\d.]+)['\"]\s*\)", r"\1", old_line)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_boolean_get_boolean(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # Boolean.getBoolean('headless') -> 'true' == System.getProperty('headless')
        new_line = re.sub(r"Boolean\.getBoolean\((['\"])([^'\"]+)\1\)", r"'true' == System.getProperty(\1\2\1)", old_line)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_class_for_name(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # Class.forName("...") -> getClass().classLoader.loadClass("...")
        new_line = re.sub(r'Class\.forName\(([^)]+)\)', r'getClass().classLoader.loadClass(\1)', old_line)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_closure_as_last_param(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # method(arg1, { ... }) -> method(arg1) { ... }
        # This is tricky to do with regex on a single line. Let's try simple cases.
        new_line = re.sub(r',\s*\{\s*([^}]*)\s*\}\s*\)', r') { \1 }', old_line)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def fix_explicit_hashset(lines, violations):
    fixed = 0
    for v in sorted(violations, key=lambda x: x['line'], reverse=True):
        idx = v['line'] - 1
        if idx >= len(lines):
            continue
        old_line = lines[idx]
        # new LinkedHashSet<>(value as Set) -> (value as Set) as Set or just value as Set
        new_line = re.sub(r'new\s+LinkedHashSet\s*<>\s*\(([^)]+)\)', r'\1', old_line)
        if new_line != old_line:
            lines[idx] = new_line
            fixed += 1
    return fixed


def process_rule(report_type, src_dir, rule_name, fixer):
    violations_by_file = defaultdict(list)
    tree = ET.parse(f'matrix-charts/build/reports/codenarc/{report_type}.xml')
    root = tree.getroot()
    for pkg in root.findall('Package'):
        for f in pkg.findall('File'):
            fname = f.get('name')
            for v in f.findall('Violation'):
                if v.get('ruleName') == rule_name:
                    line = int(v.get('lineNumber'))
                    src = v.find('SourceLine')
                    msg = v.find('Message')
                    violations_by_file[fname].append({
                        'line': line,
                        'source': src.text if src is not None else '',
                        'message': msg.text if msg is not None else ''
                    })

    total = 0
    for fname, violations in violations_by_file.items():
        path = get_file_path(fname, src_dir)
        if not path:
            print(f"  SKIP: {fname} not found")
            continue
        lines = read_file(path)
        fixed = fixer(lines, violations)
        if fixed > 0:
            write_file(path, lines)
            total += fixed
    return total


def main():
    rules_to_fix = [
        ('main', SRC_MAIN, 'UnnecessaryGString', fix_unnecessary_gstring),
        ('test', SRC_TEST, 'UnnecessaryGString', fix_unnecessary_gstring),
        ('test', SRC_TEST, 'UnnecessaryDotClass', fix_unnecessary_dot_class),
        ('test', SRC_TEST, 'SpaceAfterComma', fix_space_after_comma),
        ('main', SRC_MAIN, 'UnnecessaryToString', fix_unnecessary_to_string),
        ('test', SRC_TEST, 'UnnecessaryToString', fix_unnecessary_to_string),
        ('main', SRC_MAIN, 'ExplicitCallToCompareToMethod', fix_explicit_call_to_compare_to),
        ('test', SRC_TEST, 'ExplicitCallToCompareToMethod', fix_explicit_call_to_compare_to),
        ('test', SRC_TEST, 'ExplicitCallToPlusMethod', fix_explicit_call_to_plus),
        ('test', SRC_TEST, 'UnnecessaryPackageReference', fix_unnecessary_package_reference),
        ('test', SRC_TEST, 'SpaceAfterCommentDelimiter', fix_space_after_comment),
        ('main', SRC_MAIN, 'UnnecessarySemicolon', fix_unnecessary_semicolon),
        ('main', SRC_MAIN, 'ConsecutiveBlankLines', fix_consecutive_blank_lines),
        ('main', SRC_MAIN, 'UnusedImport', fix_unused_import),
        ('test', SRC_TEST, 'UnusedImport', fix_unused_import),
        ('main', SRC_MAIN, 'UnnecessaryGroovyImport', fix_unnecessary_groovy_import),
        ('test', SRC_TEST, 'UnnecessaryGroovyImport', fix_unnecessary_groovy_import),
        ('test', SRC_TEST, 'UnnecessaryBigDecimalInstantiation', fix_unnecessary_bigdecimal_instantiation),
        ('test', SRC_TEST, 'BooleanGetBoolean', fix_boolean_get_boolean),
        ('test', SRC_TEST, 'ClassForName', fix_class_for_name),
        ('test', SRC_TEST, 'ClosureAsLastMethodParameter', fix_closure_as_last_param),
        ('main', SRC_MAIN, 'ExplicitHashSetInstantiation', fix_explicit_hashset),
    ]

    for report_type, src_dir, rule_name, fixer in rules_to_fix:
        print(f"Fixing {rule_name} in {report_type}...")
        total = process_rule(report_type, src_dir, rule_name, fixer)
        print(f"  Fixed {total}")


if __name__ == '__main__':
    main()
