# CodeNarc Auto-Fix Tool

## Purpose
Auto-fix CodeNarc style and lint violations in a single module, run post-fix verification (spotlessApply + tests), and report remaining issues.

## When to Use
- Before merging PRs that touch Groovy/Java source files
- After feature work that may introduce style violations
- As part of the verification order from AGENTS.md

## How to Invoke
```bash
./bin/fix-codenarc <module-name>
./bin/fix-codenarc matrix-charts
./bin/fix-codenarc --dry-run matrix-core
./bin/fix-codenarc --rules UnnecessaryGString,UnusedImport matrix-ggplot
```

### CLI Flags
| Flag | Meaning |
|------|---------|
| `--dry-run` | Parse and report violations but do not modify any files |
| `--rules R1,R2` | Only apply fixers for the listed rules; CodeNarc still runs in full so all violations are reported, but only matching rules are auto-fixed |

## What It Does
1. Runs `./gradlew :<module>:codenarcMain` and `:<module>:codenarcTest` via shell
2. Locates XML reports at `<module>/build/reports/codenarc/main.xml` and `<module>/build/reports/codenarc/test.xml`
3. Parses violations and dispatches to registered fixers, one rule at a time
4. Runs `./gradlew :<module>:spotlessApply` and `:<module>:test` for verification
5. Prints summary: violations fixed, remaining, test results

### Arrow-Switch Exclusion
The build automatically excludes files containing arrow switch syntax from CodeNarc (CodeNarc 3.7 parser limitation — see `build.gradle`). The tool must:
- Detect which files were excluded by checking for the `CodeNarc: skipping` log line or by running the same arrow-switch detection
- Report excluded files in the summary so the user knows they were not analysed
- Never claim a clean result for a module that had excluded files without noting it

### Per-Module Config Overrides
Several modules have their own `config/codenarc/` directories (e.g. matrix-stats, matrix-sql, matrix-parquet, matrix-json). The tool must respect these — it should not assume the root `config/codenarc/ruleset.groovy` applies uniformly. In practice this is handled by Gradle (each module's `codenarcMain`/`codenarcTest` task uses its own config), so the tool delegates report generation to Gradle and only parses the resulting XML.

## Design

### Components
- **CLI wrapper**: `bin/fix-codenarc` — argument parsing, progress output, exit codes
- **Core engine**: `tools/codenarc/fix_codenarc.py` — pipeline: parse XML → dispatch fixers → verify
- **Registry**: `tools/codenarc/registry.py` — maps CodeNarc rule names to fixer functions
- **Fixers**: One file per rule under `tools/codenarc/fixers/` (e.g. `unnecessary_gstring.py`)

### Fixer Contract
Every fixer is a function with this signature:

```python
def fix(file_path: str, violations: list[Violation]) -> FixResult:
    """
    Apply fixes for a single rule to a single file.

    Args:
        file_path: Absolute path to the source file.
        violations: List of Violation(line, column, rule, message) for this
                    rule in this file, sorted by line descending (so fixes
                    don't shift line numbers of subsequent violations).

    Returns:
        FixResult(fixed=int, skipped=int, modified_content=str | None)
        - modified_content is the full file text after fixes, or None if
          no changes were made (all violations skipped).
        - skipped > 0 signals violations the fixer could not handle; these
          are reported as "needs manual fix".
    """
```

The engine writes `modified_content` back to the file only when it is not None. A fixer must never perform I/O itself.

### Fixer Priority and Ordering
Fixers run in registry-defined order. Order matters because some fixes interact:
1. **UnusedImport** — remove unused imports first, since other fixes may make imports unused
2. **UnnecessaryGString** — safe to run after import cleanup
3. All other fixers — alphabetical by rule name unless a dependency is documented in the registry

The registry must document ordering constraints as comments next to each entry.

### Rollback Strategy
The engine operates on one file at a time with rollback on failure:

1. Before applying fixers to a file, read and store the original content in memory
2. Apply all applicable fixers for that file, producing new content
3. Write new content and run a quick compile check (`./gradlew :<module>:compileGroovy`)
4. If compilation fails, restore the original content and log the file as "fixer broke compilation — rolled back"
5. After all files are processed, run `spotlessApply` and `test` as final verification

This ensures a bad fixer never leaves the working tree with broken code.

### Skill Integration
- **Skill doc**: `.claude/skills/fix-codenarc.md` for Claude/Codex discovery
- The skill should invoke `bin/fix-codenarc` and interpret its output, not re-implement the logic

## Runtime Requirements
- **Python**: 3.11+ (uses `tomllib` and modern type hints)
- **Dependencies**: stdlib only — XML parsing via `xml.etree.ElementTree`, no external packages
- **Gradle**: Invoked via `./gradlew` shell-out; JDK 21 must be available (same as the project)

## Limitations
- Only fixes auto-fixable style violations
- Reports compilation/static analysis issues that need manual intervention
- Does not perform git operations (branching, committing, PRs)
- Files excluded from CodeNarc due to arrow-switch syntax are not analysed

## Alternatives Considered

### Gradle Task Instead of Python Script
A Gradle task could access report paths, module configs, and source sets natively. However:
- Fixer logic is text manipulation (regex, line-level edits) which is more natural in Python
- Keeping fixers outside the build avoids coupling tool code to the project's compilation
- Python is easier to iterate on (no recompilation) and test independently

The tradeoff is acceptable as long as the Python script delegates all Gradle concerns (report generation, compilation, test execution) to `./gradlew` and only handles file parsing and text transformation.
