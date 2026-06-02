---
name: fix-codenarc
description: Auto-fix CodeNarc violations across matrix modules
metadata:
  type: user
---

# CodeNarc Auto-Fix Skill

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

## What It Does
1. Runs CodeNarc on the specified module (main + test)
2. Parses XML report and applies registered fixers one rule at a time
3. Runs spotlessApply and tests for verification
4. Prints summary: violations fixed, remaining, test results

## Design
- **Plugin-style architecture**: Each fixer is in its own file under `tools/codenarc/fixers/`
- **Core engine**: `fix_codenarc.py` handles the pipeline (parse → fix → verify)
- **Registry**: `tools/codenarc/registry.py` maps rule names to fixer functions
- **CLI wrapper**: `bin/fix-codenarc` provides CLI UX with progress output
- **Skill doc**: `.claude/skills/fix-codenarc.md` for Claude/Codex discovery

## Limitations
- Only fixes auto-fixable style violations
- Reports compilation/static analysis issues that need manual intervention
- Does not perform git operations (branching, committing, PRs)
