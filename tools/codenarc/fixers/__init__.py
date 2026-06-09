"""Auto-discovers fixer modules from this directory."""
import importlib
import pathlib

FIXERS_DIR = pathlib.Path(__file__).parent
DISCOVERED_FIXERS = {}


def discover_fixers():
    """Scan .py files in fixers/ and build {rule_name: fixer_function} map."""
    for py_file in FIXERS_DIR.glob("*.py"):
        if py_file.name.startswith("_"):
            continue
        module_name = f"tools.codenarc.fixers.{py_file.stem}"
        try:
            module = importlib.import_module(module_name)
            if hasattr(module, "fix"):
                rule_name = getattr(module, "RULE_NAME", None)
                if rule_name:
                    DISCOVERED_FIXERS[rule_name] = module.fix
        except ImportError:
            pass
    return DISCOVERED_FIXERS
