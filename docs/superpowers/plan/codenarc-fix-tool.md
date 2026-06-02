# CodeNarc Auto-Fix Tool — Implementation Plan

## Architecture

```
bin/fix-codenarc              ← CLI entry point (bash)
  ↓
tools/codenarc/
├── fix_codenarc.py           ← Core engine: pipeline orchestrator
├── registry.py               ← Rule → Fixer mapping with ordering constraints
└── fixers/
    ├── __init__.py           ← Auto-discovers fixer modules
    └── *.py                  ← One file per rule (e.g., unused_import.py)
```

## Key Design Decisions

1. **Python stdlib only** — no external dependencies; XML parsing via `xml.etree.ElementTree`
2. **In-memory rollback** — each file's original content stored before fixers run; compilation failure triggers restore
3. **Fixer contract** — every fixer is a pure function `(file_path, violations) → FixResult`, no I/O
4. **Ordering**: UnusedImport first → UnnecessaryGString → alphabetical rest (documented in registry)

## Tasks

| # | Task | Description |
|---|------|-------------|
| 1 | Core engine | `tools/codenarc/fix_codenarc.py` — Gradle invocation, XML parsing, fixer dispatch, compile check, spotlessApply + test, summary output |
| 2 | Registry | `tools/codenarc/registry.py` — maps rule names to fixers with ordering comments |
| 3 | Fixers | Individual fixer files for top rules by violation count |
| 4 | CLI wrapper | `bin/fix-codenarc` — argument parsing (`--dry-run`, `--rules R1,R2`), progress output, exit codes |
| 5 | Skill doc | `.claude/skills/fix-codenarc.md` for Claude/Codex discovery |
| 6 | End-to-end testing | Run against real modules, verify compilation + tests pass |

## Top Fixers to Implement (by violation count in matrix-core)

1. **IfStatementBraces** (943) — add braces to if statements missing them
2. **DuplicateNumberLiteral** (343) — extract to named constant
3. **DuplicateStringLiteral** (638) — extract to named constant
4. **ClassEndsWithBlankLine** (151) — ensure blank line before class closing
5. **UnnecessaryObjectReference** (78) — remove redundant references
6. **UnnecessaryCast** (76) — remove redundant casts
7. **UnnecessaryGString** (75) — replace `"${x}"` with `x.toString()` where appropriate
8. **DuplicateMapLiteral** (47) — deduplicate map entries
9. **ClassStartsWithBlankLine** (34) — ensure blank line after class opening
10. **UnusedMethodParameter** (21) — remove unused parameters
11. **UnusedImport** (18) — remove unused imports
12. **SpaceAfterOpeningBrace** / **SpaceBeforeClosingBrace** (11 each) — whitespace fixes
13. **UnnecessaryElseStatement** (10) — simplify if-return-else patterns
14. **ReturnsNullInsteadOfEmptyCollection** (10) — return `[]` instead of `null`

## Rollback Strategy

- Before fixing a file: read original content into memory
- Apply all applicable fixers, produce new content
- Run quick compile check (`./gradlew :module:compileGroovy`)
- If compilation fails → restore original content, log as "rolled back"
- After all files processed → final `spotlessApply` + `test`

## Exit Codes

- **0**: All violations fixed or no violations found
- **1**: Some violations remain (dry-run mode or manual fixes needed)
- **2**: Tool error (missing Gradle, invalid args, etc.)
