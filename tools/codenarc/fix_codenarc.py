"""Core engine: pipeline orchestrator for codenarc auto-fix."""
import subprocess
import sys
import xml.etree.ElementTree as ET
import pathlib
import shutil
import argparse
import json
import time
from collections import defaultdict

# Resolve project root (two levels up from this file)
PROJECT_ROOT = pathlib.Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from tools.codenarc.registry import RULES, get_rules_for_phase, PHASE_1, PHASE_2
from tools.codenarc.fixers import discover_fixers


def run_gradle(args_list, check=True):
    """Run ./gradlew with given arguments. Return (returncode, stdout)."""
    cmd = ["./gradlew"] + list(args_list)
    result = subprocess.run(cmd, capture_output=True, text=True, cwd=str(PROJECT_ROOT))
    if check and result.returncode != 0:
        return result.returncode, result.stderr
    return result.returncode, result.stdout


def run_codenarc():
    """Run codenarcMain and return XML report path."""
    # Find all modules with codenarcMain
    code = out = run_gradle(["projects"], check=False)
    modules = []
    for line in (out or "").split("\n"):
        if "project" in line.lower() or ":" in line:
            pass  # we try common module names instead

    # Try running codenarc on all known source sets
    report_dir = PROJECT_ROOT / "build" / "reports" / "codenarc"

    # Run codenarcMain - try at root level first
    code, _ = run_gradle(["codenarcMain"], check=False)
    if code == 0:
        # Find the generated XML report
        xml_files = list(report_dir.rglob("*.xml")) if report_dir.exists() else []
        if xml_files:
            return str(xml_files[0])

    # Fallback: try all modules
    for module in ["pipeline", "ingest", "transform", "common", "shared"]:
        code, _ = run_gradle([f":{module}:codenarcMain"], check=False)
        if code == 0:
            xml_files = list(report_dir.rglob("*.xml"))
            if xml_files:
                return str(xml_files[-1])

    return None


def parse_codenarc_xml(xml_path):
    """Parse codenarc XML report and return list of violation dicts."""
    violations = []
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()

        # Standard checkstyle-like format
        for file_elem in root.findall("file"):
            filename = file_elem.get("name")
            for error in file_elem.findall("error"):
                violations.append({
                    "ruleName": error.get("source", ""),
                    "fileName": filename,
                    "lineNumber": int(error.get("line", 0)),
                    "message": error.get("message", ""),
                })
    except ET.ParseError as e:
        print(f"XML parse error: {e}", file=sys.stderr)
    return violations


def group_violations_by_file(violations):
    """Group violations by file path. Returns {file_path: [violations]}."""
    grouped = defaultdict(list)
    for v in violations:
        grouped[v["fileName"]].append(v)
    return dict(grouped)


def group_violations_by_rule(violations):
    """Group violations by rule name. Returns {rule_name: count}."""
    counts = defaultdict(int)
    for v in violations:
        counts[v["ruleName"]] += 1
    return dict(counts)


def find_module_for_file(file_path):
    """Determine which gradle module a groovy file belongs to."""
    fp = pathlib.Path(file_path)
    parts = fp.parts
    # Heuristic: look for src/main/groovy pattern
    try:
        idx = parts.index("src")
        module_name = parts[idx - 1]
        return module_name
    except (ValueError, IndexError):
        pass

    # Fallback: check known modules
    known_modules = ["pipeline", "ingest", "transform", "common", "shared", "core", "api"]
    for part in parts:
        if part in known_modules:
            return part
    return None


def compile_check(module):
    """Compile a module to verify fixes didn't break anything. Returns True on success."""
    code, stderr = run_gradle([f":{module}:compileGroovy"], check=False)
    if code != 0:
        # Also try compileJava in case it's a Java-heavy module
        code2, _ = run_gradle([f":{module}:compileJava"], check=False)
        return code2 == 0
    return True


def git_init_branch(branch_name):
    """Create and checkout a new git branch for fixes."""
    subprocess.run(["git", "branch", "-d", branch_name], capture_output=True)
    result = subprocess.run(["git", "checkout", "-b", branch_name], capture_output=True, text=True, cwd=str(PROJECT_ROOT))
    return result.returncode == 0


def git_commit_file(file_path, message):
    """Stage and commit a single file."""
    subprocess.run(["git", "add", str(file_path)], capture_output=True, cwd=str(PROJECT_ROOT))
    result = subprocess.run(
        ["git", "commit", "-m", message],
        capture_output=True,
        text=True,
        cwd=str(PROJECT_ROOT),
    )
    return result.returncode == 0


def git_diff(file_path):
    """Get staged/unstaged diff for a file."""
    result = subprocess.run(
        ["git", "diff", "--", str(file_path)],
        capture_output=True,
        text=True,
        cwd=str(PROJECT_ROOT),
    )
    return result.stdout


def apply_fixes(violations_by_file, fixers, dry_run=False, min_violations=1):
    """Apply fixers to files and handle compilation/rollback.

    Returns (applied_count, rolled_back_count, skipped_count).
    """
    applied = 0
    rolled_back = 0
    skipped = 0

    rule_counts = group_violations_by_rule(
        v for vlist in violations_by_file.values() for v in vlist
    )

    for file_path, file_violations in violations_by_file.items():
        # Filter to only rules we have fixers for
        fixable_rules = set(fixers.keys())
        relevant = [v for v in file_violations if v["ruleName"] in fixable_rules]
        if not relevant:
            skipped += 1
            continue

        # Check min-violations threshold per rule
        filtered = []
        for v in relevant:
            rc = rule_counts.get(v["ruleName"], 0)
            if rc >= min_violations:
                filtered.append(v)
        relevant = filtered

        if not relevant:
            skipped += 1
            continue

        fp = pathlib.Path(file_path)
        if not fp.exists():
            print(f"SKIP (file not found): {file_path}")
            skipped += 1
            continue

        original_content = fp.read_text()

        # Apply each fixer in order
        current_content = original_content
        rules_applied = []

        # Order: apply safe fixers first
        for rule_name in get_rules_for_phase(PHASE_1):
            if rule_name not in fixers:
                continue
            rule_violations = [v for v in relevant if v["ruleName"] == rule_name]
            if not rule_violations:
                continue

            fixer = fixers[rule_name]
            result = fixer(str(fp), rule_violations, current_content)
            if result["applied"] and result["new_content"] is not None:
                current_content = result["new_content"]
                rules_applied.append((rule_name, result["reason"]))

        # Apply phase 2 fixers
        for rule_name in get_rules_for_phase(PHASE_2):
            if rule_name not in fixers:
                continue
            rule_violations = [v for v in relevant if v["ruleName"] == rule_name]
            if not rule_violations:
                continue

            fixer = fixers[rule_name]
            result = fixer(str(fp), rule_violations, current_content)
            if result["applied"] and result["new_content"] is not None:
                current_content = result["new_content"]
                rules_applied.append((rule_name, result["reason"]))

        if not rules_applied:
            skipped += 1
            continue

        if dry_run:
            print(f"[DRY-RUN] {file_path}: {len(rules_applied)} fixer(s) would apply")
            for rule, reason in rules_applied:
                print(f"  - {rule}: {reason}")
            applied += 1
            continue

        # Write fixed content
        fp.write_text(current_content)

        # Compile check
        module = find_module_for_file(file_path)
        if module and compile_check(module):
            msg = f"fix(codenarc): apply {len(rules_applied)} rule(s) to {fp.name}"
            git_commit_file(file_path, msg)
            applied += 1
        else:
            # Rollback
            fp.write_text(original_content)
            rolled_back += 1
            print(f"ROLLED BACK {file_path} (compilation failed)")

    return applied, rolled_back, skipped


def print_summary(violations, rule_counts, applied, rolled_back, skipped):
    """Print summary of fix results."""
    print("\n===== CodeNarc Fix Summary =====")
    print(f"Total violations found: {len(violations)}")
    print(f"Violations by rule:")
    for rule, count in sorted(rule_counts.items(), key=lambda x: -x[1]):
        print(f"  {rule}: {count}")
    print(f"\nFiles fixed: {applied}")
    print(f"Files rolled back: {rolled_back}")
    print(f"Files skipped: {skipped}")
    remaining = len(violations) - (applied * max(1, len(violations) // max(applied, 1)))
    print("============================\n")


def main():
    parser = argparse.ArgumentParser(description="Auto-fix CodeNarc violations")
    parser.add_argument("--dry-run", action="store_true", help="Show what would be fixed without applying")
    parser.add_argument("--phase", type=int, choices=[1, 2], default=1, help="Which phase of fixers to run (default: 1)")
    parser.add_argument("--rules", type=str, default=None, help="Comma-separated list of rules to apply")
    parser.add_argument("--min-violations", type=int, default=1, help="Minimum violation count threshold")
    parser.add_argument("--branch", type=str, default=None, help="Feature branch name (auto-generated if not provided)")
    args = parser.parse_args()

    branch_name = args.branch or f"fix/codenarc-{int(time.time())}"

    # Discover fixers
    all_fixers = discover_fixers()

    # Filter by requested rules
    if args.rules:
        requested = [r.strip() for r in args.rules.split(",")]
        fixers = {k: v for k, v in all_fixers.items() if k in requested}
    else:
        # Use phase-appropriate fixers
        phase_rules = set(get_rules_for_phase(args.phase))
        fixers = {k: v for k, v in all_fixers.items() if k in phase_rules}

    print(f"Using {len(fixers)} fixer(s): {', '.join(sorted(fixers.keys()))}")

    # Run CodeNarc
    print("Running CodeNarc...")
    xml_path = run_codenarc()
    if not xml_path:
        print("ERROR: Could not find CodeNarc XML report", file=sys.stderr)
        sys.exit(2)

    print(f"Found report: {xml_path}")

    # Parse violations
    violations = parse_codenarc_xml(xml_path)
    if not violations:
        print("No violations found. Nothing to fix.")
        sys.exit(0)

    rule_counts = group_violations_by_rule(violations)
    violations_by_file = group_violations_by_file(violations)

    # Git branch setup (skip in dry-run)
    if not args.dry_run:
        print(f"Creating branch {branch_name}...")
        if not git_init_branch(branch_name):
            print("ERROR: Failed to create git branch", file=sys.stderr)
            sys.exit(2)

    # Apply fixes
    applied, rolled_back, skipped = apply_fixes(
        violations_by_file, fixers, dry_run=args.dry_run, min_violations=args.min_violations
    )

    # Summary
    print_summary(violations, rule_counts, applied, rolled_back, skipped)

    # Exit code
    if applied > 0:
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
