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

1. **Python stdlib only** — no external dependencies; XML parsing via `xml.etree.ElementTree`; AST parsing via Python's built-in `ast` module or shell-out to Groovy for Java/Groovy source analysis
2. **In-memory rollback per file** — each file's original content stored before fixers run; compilation failure triggers restore
3. **Fixer contract** — every fixer is a pure function `(file_path, violations, file_content) → FixResult` where `FixResult = {applied: bool, new_content: str | None, reason: str}`
4. **Phased rollout** — safe/mechanical fixers first (Phase 1), semantic/behavior-changing fixers deferred to Phase 2 with separate design docs
5. **Data-driven priority** — violation counts come from actual CodeNarc XML output at runtime, not hardcoded in the plan
6. **Git integration** — tool creates a working branch, commits per-file or per-module, produces diffs for review

## Phased Fixer Priorities

### Phase 1: Safe / Mechanical (behavior-preserving, low risk)

These fixers change formatting or remove dead code without altering runtime behavior:

| # | Rule | Risk | Notes |
|---|------|------|-------|
| 1 | **UnusedImport** | None | Pure removal; violations alone are sufficient |
| 2 | **UnnecessaryCast** | None | Remove redundant type casts |
| 3 | **SpaceAfterOpeningBrace** / **SpaceBeforeClosingBrace** | None | Whitespace only |
| 4 | **ClassEndsWithBlankLine** (151) | Low | Add blank line before class closing `}` |
| 5 | **ClassStartsWithBlankLine** (34) | Low | Add blank line after class opening `{` |
| 6 | **UnnecessaryObjectReference** (78) | Low | Remove redundant `this.` or qualified references where unambiguous |

### Phase 2: Style with moderate complexity (behavior-preserving but AST-aware)

These require source parsing and careful edge-case handling:

| # | Rule | Risk | Notes |
|---|------|------|-------|
| 7 | **IfStatementBraces** (943) | Medium | ⚠️ Large count — verify rule isn't over-broad before enabling. Add `--min-violations` threshold gate. |
| 8 | **UnnecessaryGString** (75) | Low-Medium | Replace `"${x}"` with `x.toString()` where GString is unnecessary |
| 9 | **UnnecessaryElseStatement** (10) | Low | Simplify `if-return-else` patterns; verify logic equivalence |

### Phase 3: Deferred — requires separate design doc

These are non-trivial and need individual design docs before implementation:

| # | Rule | Reason for deferral |
|---|------|---------------------|
| 10 | **DuplicateNumberLiteral** (343) | Extracting to named constants requires naming, scope determination, edge-case handling (magic numbers in loops, array init, etc.) |
| 11 | **DuplicateStringLiteral** (638) | Same as above; also raises i18n concerns for string extraction |
| 12 | **DuplicateMapLiteral** (47) | Deduplication logic is complex; may conflict with spotless formatting |
| 13 | **UnusedMethodParameter** (21) | Needs call-site analysis; removing parameters breaks callers if not exhaustive |

### Excluded from auto-fix (behavior-changing)

| # | Rule | Reason for exclusion |
|---|------|---------------------|
| 14 | **ReturnsNullInsteadOfEmptyCollection** (10) | Changes runtime behavior — callers may rely on null checks. Flag as suggestion only, do not auto-fix. |

## Tasks

| # | Task | Description |
|---|------|-------------|
| 1 | Core engine | `tools/codenarc/fix_codenarc.py` — Gradle invocation (`codenarcMain`), XML parsing, fixer dispatch, compile check per file, git branch management, summary output |
| 2 | Registry | `tools/codenarc/registry.py` — maps rule names to fixers with ordering constraints and phase metadata |
| 3 | Phase 1 fixers | Individual fixer files for safe/mechanical rules (rules #1–#6 above) |
| 4 | CLI wrapper | `bin/fix-codenarc` — argument parsing (`--dry-run`, `--phase PHASE`, `--rules R1,R2`, `--min-violations N`), progress output, exit codes |
| 5 | Git integration | Branch creation, per-file or per-module commits, diff generation |
| 6 | Phase 2 scaffolding | Skeleton for rules #7–#9 (implement as no-ops that log "deferred" until design docs are approved) |
| 7 | Skill doc | `.claude/skills/fix-codenarc.md` for Claude/Codex discovery |
| 8 | End-to-end testing | Run against real modules, verify compilation + tests pass |

## Rollback Strategy

- **Per-file**: Before fixing a file: read original content into memory. Apply all applicable fixers, produce new content. Run `./gradlew :module:compileGroovy` for the affected module.
- If compilation fails → restore original content from memory, log as "rolled back", continue with next file.
- **Post-fix**: After all files processed → final `spotlessApply` to normalize formatting, then `test` on affected modules.
- **Git safety**: All work happens on a feature branch. Original branches untouched. Can `git reset` entire branch if needed.

## Exit Codes

- **0**: All violations fixed or no violations found
- **1**: Some violations remain (dry-run mode, manual fixes needed, or below `--min-violations` threshold)
- **2**: Tool error (missing Gradle, invalid args, git failure, etc.)

## Gate Conditions Before Phase 2+

Before implementing any deferred fixers:
1. Validate violation counts against current CodeNarc XML output (counts are stale from plan creation)
2. For `IfStatementBraces`: confirm with team that auto-bracing aligns with style guide; consider excluding single-line control flow if intentional
3. For duplicate literal extraction: draft design doc covering naming strategy, scope rules, and i18n implications
4. For behavior-changing rules (e.g., `ReturnsNullInsteadOfEmptyCollection`): produce a separate "suggestion-only" mode that flags violations without applying fixes
