"""Rule -> Fixer mapping with ordering constraints and phase metadata."""

# Phase classification for fixers
PHASE_1 = 1  # Safe/mechanical, behavior-preserving
PHASE_2 = 2  # Style with moderate complexity, AST-aware
PHASE_3 = 3  # Deferred, needs design doc

# Rule definitions: (rule_name, phase, risk_level, depends_on)
# depends_on is a list of rule names that should be fixed before this one
RULES = {
    # Phase 1: Safe/mechanical
    "UnusedImport": {"phase": PHASE_1, "risk": "none", "depends_on": []},
    "UnnecessaryCast": {"phase": PHASE_1, "risk": "none", "depends_on": []},
    "SpaceAfterOpeningBrace": {"phase": PHASE_1, "risk": "none", "depends_on": []},
    "SpaceBeforeClosingBrace": {"phase": PHASE_1, "risk": "none", "depends_on": []},
    "ClassEndsWithBlankLine": {"phase": PHASE_1, "risk": "low", "depends_on": []},
    "UnnecessaryObjectReference": {"phase": PHASE_1, "risk": "low", "depends_on": []},

    # Phase 2: Style with moderate complexity
    "IfStatementBraces": {"phase": PHASE_2, "risk": "medium", "depends_on": []},
    "UnnecessaryGString": {"phase": PHASE_2, "risk": "low-medium", "depends_on": []},
    "UnnecessaryElseStatement": {"phase": PHASE_2, "risk": "low", "depends_on": []},

    # Phase 3: Deferred
    "DuplicateNumberLiteral": {"phase": PHASE_3, "risk": "high", "depends_on": []},
    "DuplicateStringLiteral": {"phase": PHASE_3, "risk": "high", "depends_on": []},
    "DuplicateMapLiteral": {"phase": PHASE_3, "risk": "high", "depends_on": []},
    "UnusedMethodParameter": {"phase": PHASE_3, "risk": "high", "depends_on": []},

    # Excluded (suggestion only)
    "ReturnsNullInsteadOfEmptyCollection": {"phase": -1, "risk": "behavior-changing", "depends_on": []},
}


def get_rules_for_phase(phase):
    """Return rule names for the given phase, ordered by dependency constraints."""
    rules = {name: info for name, info in RULES.items() if info["phase"] == phase}
    return _topological_sort(rules)


def _topological_sort(rules):
    """Sort rules respecting depends_on ordering."""
    sorted_rules = []
    visited = set()

    def visit(name):
        if name in visited:
            return
        visited.add(name)
        info = rules.get(name, RULES.get(name))
        if info:
            for dep in info.get("depends_on", []):
                visit(dep)
        sorted_rules.append(name)

    for name in rules:
        visit(name)
    return sorted_rules


def is_safe_fixer(rule_name):
    """Check if a rule is in Phase 1 (safe/mechanical)."""
    info = RULES.get(rule_name)
    return info and info["phase"] == PHASE_1


def get_phase(rule_name):
    """Get the phase for a rule name."""
    info = RULES.get(rule_name)
    return info["phase"] if info else None
