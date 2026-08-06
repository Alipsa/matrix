# BOM API verification suite (failsafe ITs)

## Context

We want to release **matrix-core 3.8.0 → 3.9.0** and **matrix-charts 0.5.0 → 0.5.1** while every
other module stays at its currently released version (matrix-bigquery, matrix-datasets, matrix-gsheets,
matrix-json and matrix-xchart all have unreleased SNAPSHOT work in the tree that must not ship).
`matrix-bom/bom.xml` already encodes exactly that intent — two managed SNAPSHOT properties, the rest pinned
to released versions, with commented-out SNAPSHOT lines documenting the deferred ones.

The question is whether that *combination* actually works. Today it is verified by
`matrix-bom/preReleaseVerify.sh` running one `MatrixModulesTest` with roughly one assertion per
module — a smoke test. Two problems:

1. **Coverage.** One call per module cannot show that the BOM's dependency set works; it shows that
   one code path per module loads.
2. **The test does not test released artifacts.** Released matrix artifacts in `~/.m2` have been
   overwritten by local `publishToMavenLocal` builds. Direct evidence:
   `~/.m2/repository/se/alipsa/matrix/matrix-ggplot/0.5.0/matrix-ggplot-0.5.0.pom` and the matching
   `matrix-pict/0.5.0` pom both declare `matrix-charts:0.5.1-SNAPSHOT` — impossible for a Central
   release, since Central rejects SNAPSHOT dependencies. A scan of all 21 released-version
   directories finds those two as the only poms carrying a SNAPSHOT dependency, but jar mtimes show
   the overwriting is broader and not uniform: `matrix-ggplot/0.5.0`, `matrix-pict/0.5.0` and
   `matrix-stats/2.5.2` are 2026-08-05, `matrix-core/3.8.0` is 2026-08-02, `matrix-charts/0.5.0` is
   2026-06-25.

   The point is not that every artifact is known-bad — it is that **provenance is no longer
   decidable from `~/.m2`**. `_remote.repositories` cannot settle it: Gradle's `publishToMavenLocal`
   replaces the jar and pom but leaves that file untouched, so `matrix-stats/2.5.2` still reads
   `matrix-stats-2.5.2.jar>central=` for a jar rewritten locally on 2026-08-05. So
   `preReleaseVerify.sh` may be exercising the working tree under released version numbers rather
   than the published set the BOM names, and nothing in `~/.m2` can tell you which.

Structural fact that shapes the design: the downstream modules that consume matrix-core declare it
`compileOnly project(':matrix-core')` (matrix-stats, matrix-csv, matrix-json, matrix-sql,
matrix-spreadsheet, matrix-datasets, matrix-parquet, matrix-avro, matrix-arff, matrix-tablesaw,
matrix-smile, matrix-gsheets, matrix-bigquery, matrix-xchart, matrix-charts, matrix-ggplot,
matrix-pict). Their published poms therefore carry **no** matrix-core dependency — the consumer
supplies it. So matrix-stats 2.5.2's bytecode, compiled against core 3.8.0, is expected to run
against 3.9.0 — 3.9.0's changes are generic-signature changes whose erasures are unchanged (see
`matrix-core/release.md`), so binary compatibility should hold and the source compatibility must
be checked explicitly. This is the assumption the whole
release rests on, and it is *checked* rather than assumed: at runtime by the ITs for every path
they exercise, and across the whole public surface for binary and source compatibility by the
japicmp run in 6.6. The focused Java consumer test in 3.4 remains the executable check for the
documented Java call shapes.
(matrix-core itself declares `api project(':matrix-groovy-ext')`; matrix-groovy-ext and
matrix-logging do not depend on core at all.) Only matrix-ggplot and matrix-pict declare
matrix-charts (`api`), pinned at 0.5.0 and upgraded to 0.5.1 by the BOM's `dependencyManagement`.

**Outcome:** an on-demand integration-test suite in `matrix-bom`, run under maven-failsafe behind a
profile, that exercises the documented public API of all 20 BOM modules against a dependency set
resolved from the BOM — with the modules under release resolved from an isolated repository and
everything else forced from Maven Central.

The infrastructure (sections 1–3, 5–6) is built once and serves every future release. The 20 module
ITs are the long tail: 4.1–4.6 gate *this* release, 4.7–4.20 follow it. See "Release gate" at the
top of section 4 for why the split falls there.

## Approach

Everything lives in `matrix-bom`. The suite resolves its runtime through `matrix-bom/pom.xml`
(`matrix-all`), which already imports `bom.xml` via `dependencyManagement` — so the ITs run against
precisely the version set the BOM declares.

- **Contract:** each module's `readme.md`/`README.md` plus `docs/cookbook/*.md` and
  `docs/tutorial/*.md` define the public API to cover. Groovy's default-public visibility makes raw
  type counts meaningless (matrix-charts has ~244 source types, few of them intended API); the docs
  are the actual contract. Broken documentation becomes a failing test — see "A documented example
  that does not run" below for which side gets fixed when they disagree.
- **Measurement:** a JaCoCo agent run over the IT phase, reported against the resolved matrix
  dependency jars, gives a per-module number for where coverage actually lands. Reported, not gated.
- **Convention:** follow `matrix-ggplot/src/test/groovy/gg/DocExamplesTest.groovy` — hand-written
  tests, one per documented example/API area, each with GroovyDoc naming the doc section it covers.
- **Isolation:** a runner script builds a dedicated Maven repository, publishes into it only the
  modules under release, and lets Maven pull every other module from Central. Nothing can silently
  fall back to the contaminated `~/.m2`.

Surefire keeps running `*Test`: the existing `MatrixModulesTest` smoke test stays as-is, unchanged.
The one `*Test` that goes away is `BiqQueryTest`, absorbed into `BigQueryApiIT` (4.20).
Failsafe runs `*IT`, only under `-Papi-it`.

---

## Conventions everything below depends on

### Skip properties

As the pom stands today, `-DskipTests` skips *both* surefire and failsafe, so it cannot be used to
run ITs alone. Surefire therefore gets `<skipTests>${skipUnitTests}</skipTests>` with a
`skipUnitTests` property defaulting to `false`; failsafe uses its own native `skipITs`. So:

- unit tests only: `mvn verify` (no `api-it` profile)
- ITs only: `mvn -Papi-it -DskipUnitTests=true verify`
- both: `mvn -Papi-it verify`

These show the **profile and skip mechanics only** — they are not runnable verification commands.
Every real run additionally needs `-s`/`-gs`/`-Dmaven.repo.local` against a populated `$REPO`; see
the two sections below. The same caveat applies to the tag example in 1.5.

**After 1.4, `-DskipTests` remains a combined skip and must not be used for selecting one
phase.** The surefire `skipTests` user property still overrides the POM configuration, so
`-DskipTests=true` skips the unit tests; failsafe also honours the same property and skips the
ITs. The supported flags in `matrix-bom` are `-DskipUnitTests=true` for the unit tests,
`-DskipITs=true` for the ITs, and `-Dmaven.test.skip=true` for both plus test compilation. Do
not use `-DskipTests` in this module or in any script under it.

### Zero-tests is a failure, not a pass

A broken `**/*IT.class` include pattern makes failsafe report success having run nothing — a green
release verification that verified nothing. Failsafe therefore gets
`<failIfNoTests>${it.failIfNoTests}</failIfNoTests>` with the property defaulting to `true`. The one
place that legitimately has zero ITs is the section-1 infrastructure check (the verify block under
1.10), which passes `-Dit.failIfNoTests=false` explicitly. Note that a *skipped* test still counts
as completed, so an `@Disabled` method does not trip this check — see 1.2.

### A documented example that does not run

"Broken documentation becomes a failing test" is the contract, and across 20 modules — 18 of them
frozen released jars — some examples will not run on the first attempt. Without a rule this stalls
on the first module, so the rule is fixed in advance:

- **An already-released module** (everything except matrix-core and matrix-charts): the binary is
  frozen and cannot be changed for this release, so the **documentation is what is wrong**. Fix the
  doc to match the released behaviour, write the IT against the corrected text, and note the
  correction in `api-coverage.md` (5.1a/5.1b). Only if the released behaviour is itself a defect does it
  become an issue for a later release of that module — it never blocks this one.
- **matrix-core or matrix-charts** (under release): either side may be wrong. Decide per case, and
  decide it *before* the release rather than deferring — a doc example that the module under release
  no longer satisfies is exactly the regression this suite exists to find.
- **Either way, never delete the test to make the suite green.** Convert it to a documented
  correction or an explicit `@Disabled` carrying the reason and the issue link.

### Every `-SNAPSHOT` in `bom.xml` must be published into `$REPO` before any Maven run

This is the prerequisite that is easiest to forget and fails the most confusingly. `bom.xml` pins
some modules to `-SNAPSHOT` versions — today `matrix-core:3.9.0-SNAPSHOT` and
`matrix-charts:0.5.1-SNAPSHOT`. **No remote repository serves them**: Central rejects SNAPSHOTs, and
`matrix-all` declares no snapshot repository. Because `$REPO` replaces `~/.m2` entirely via
`-Dmaven.repo.local`, a `$REPO` whose `se/alipsa/matrix` subtree has just been wiped cannot resolve
`matrix-all` *at all* until those modules have been published into it from the working tree with
Gradle — and that subtree is wiped on every run, in both wipe modes.

So the order is fixed, and every path through this plan follows it:

1. detect the `-SNAPSHOT` properties in `bom.xml` (2.5)
2. `publishToMavenLocal` **exactly** those modules into `$REPO` (2.6)
3. assert `$REPO` holds exactly that set and nothing else (2.7)
4. `mvn -f bom.xml install`, then `mvn verify` (2.8)

Steps 1–2 are not optional setup — skip them and Maven fails in dependency resolution before a
single test runs. The same applies to the by-hand checks in section 1, which is why its verify block
publishes core and charts before installing the BOM. Whenever a deferred module is uncommented back
to SNAPSHOT in `bom.xml`, detection picks it up and it must be published too; that is precisely why
2.6 forbids hard-coding `:matrix-core` and `:matrix-charts`.

**Zero detected SNAPSHOTs is a legal state, not an error — and it is the normal one.** The moment
matrix-core 3.9.0 and matrix-charts 0.5.1 are released, `bom.xml` pins every module to a released
version and 2.5 returns an empty set. Since 2.10 makes this runner the permanent
`preReleaseVerify.sh`, *every subsequent release starts here*, so the empty case is the common path
and must be handled explicitly rather than falling out of the code by accident:

- 2.6 skips the Gradle publish entirely (an empty task list would otherwise invoke `gradlew` with no
  tasks — a silent no-op that exits 0 — or trip `set -u` on older bash).
- 2.7 expects `$REPO/se/alipsa/matrix` to be **absent or empty**, and must not fail on the missing
  directory. Every matrix artifact then comes from Central, which is the strongest form of the check
  this suite performs: nothing local is involved at all.
- 6.6's japicmp comparison has **no module under release to compare**, so the runner skips it
  (2.12) and says so. It must not be left hard-coded at `3.8.0 -> 3.9.0-SNAPSHOT`: a version that
  exists in no repository makes the japicmp run fail, and 6.6 treats a failed *run* as a hard error,
  which would abort every routine verification from the first post-release run onward. 2.12
  generates the version pair from the detection in 2.5 and skips the comparison entirely when the
  set is empty.

Print which of the two modes the run is in before doing anything else, so the operator can see
whether local artifacts were involved.

### An isolated local repo is not an isolated resolution

`-Dmaven.repo.local="$REPO"` relocates the *cache*. It does not decide where downloads come from —
that is `settings.xml`, and Maven reads **two** of them: the user file (`~/.m2/settings.xml`) and
the global file (`$MAVEN_HOME/conf/settings.xml`), merged. A `<mirror>` with `<mirrorOf>*</mirrorOf>`,
an `<activeProfile>` contributing `<repositories>`, or a corporate proxy in *either* will silently
redirect what the suite believes are Central artifacts, and the verification would attest to a
dependency set it never actually resolved from Central.

Both files exist on the current dev machine. `~/.m2/settings.xml` declares a `<mirror>`, a
`localNexus` server, and an `<activeProfile>project-properties</activeProfile>`;
`/home/per/.sdkman/candidates/maven/current/conf/settings.xml` is the stock SDKMAN template whose
only active element is the standard `maven-default-http-blocker` mirror — benign today, but nothing
keeps it that way, and on CI or another developer's box it is unknown.

**`-s` overrides only the user file.** Suppressing the global one needs `-gs`. So every Maven
invocation in this plan takes *both* flags alongside `-Dmaven.repo.local="$REPO"`:

```bash
MVN_ISOLATED=(-s "$BOM_DIR/verify-settings.xml" -gs "$BOM_DIR/verify-settings.xml"
              -Dmaven.repo.local="$REPO")
mvn "${MVN_ISOLATED[@]}" …
```

Pointing both at the same file is deliberate — it is the only way to be sure nothing outside the
repository contributes to resolution. Note this also drops the `maven-default-http-blocker` mirror;
that is safe here because the file below declares exactly one repository and it is `https`.

The file itself — a committed, minimal settings with exactly one repository, no mirrors, and no
active profiles beyond its own:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <profiles>
    <profile>
      <id>central-only</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo.maven.apache.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <url>https://repo.maven.apache.org/maven2</url>
          <snapshots><enabled>false</enabled></snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles><activeProfile>central-only</activeProfile></activeProfiles>
</settings>
```

`<snapshots><enabled>false</enabled></snapshots>` is deliberate and load-bearing: the modules under
release must come from the local publish into `$REPO` and from nowhere else, so no remote is
permitted to serve a SNAPSHOT. Combined with 2.7's exact-set assertion, a module that fails to
publish becomes an unresolvable-dependency error rather than a silent download of something else.

Do the same for Gradle in 2.6 — it reads `~/.m2/settings.xml` for the local-repo location and its
own `repositories {}` block for downloads. The publish tasks only need to *write* into `$REPO`, so
this matters less there, but the 2.7 assertion is what catches it either way.

### Working directory and paths

The runner resolves absolute paths from its own location and then `cd`s into `$BOM_DIR`, so `-f
bom.xml`, `target/...` and every relative path mean the same thing regardless of where the script was
invoked from:

```bash
set -euo pipefail
BOM_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(dirname "$BOM_DIR")
REPO="${BOM_VERIFY_REPO:-$ROOT_DIR/.bom-verify-repo}"
cd "$BOM_DIR"          # every mvn/target path below is relative to this
```

Gradle is the exception — invoke it as `"$ROOT_DIR/gradlew"` with `-p "$ROOT_DIR"`.

Every Maven invocation passes `-Dmaven.repo.local="$REPO"` — including the `dependency:*` and
reporting steps, otherwise they silently resolve contaminated artifacts from `~/.m2` and defeat the
isolation.

### Repository location and deletion safety

`$REPO` must live outside any `target/` directory, because `mvn clean verify` deletes `target/` in
the same invocation that resolves dependencies.

**Wipe `se/alipsa/matrix`, not the whole repository.** The provenance problem this suite exists to
solve is confined to one subtree: `~/.m2/repository/se/alipsa/matrix` is where
`publishToMavenLocal` overwrote released artifacts. Nothing else in a local repository can be
contaminated by a Gradle publish, so nothing else needs re-downloading. Wiping all of `$REPO` on
every run means re-fetching POI, Hadoop, JavaFX, Smile, testcontainers, gsvg and their transitives —
multiple GB and several minutes — *every time*, including the per-module iteration runs section 4
recommends. That cost buys nothing: 2.7's exact-set assertion is an assertion about
`$REPO/se/alipsa/matrix` alone, and it holds identically either way.

So the default is the scoped wipe:

```bash
rm -rf "$REPO/se/alipsa/matrix"          # default: only the subtree whose provenance matters
```

with the full wipe available for the paranoid case (a corrupted cache, a suspected mirror, or a
first run proving the third-party set also resolves from Central):

```bash
BOM_VERIFY_FULL_WIPE=true ./verifyBomApi.sh    # rm -rf "$REPO" in its entirety
```

6.1 runs the full wipe once, deliberately; routine runs and section-4 iteration use the default.
Print which mode was used in the run header, so a report can never be mistaken for a
fully-from-scratch resolution when it was not.

**Both modes are guarded identically**, because `BOM_VERIFY_REPO` is user-supplied and both delete
recursively — the scoped form still deletes an attacker-chosen `<path>/se/alipsa/matrix`, and the
full form deletes everything. Run `assert_safe_repo_path` before either.

String comparison alone is not enough: `..` components and symlinked parents let a supplied path
resolve somewhere the guard never checked.

An earlier draft of this plan tried to canonicalize by walking up to the nearest existing ancestor
and re-appending the remaining suffix verbatim. **That is broken and must not be used** — the
re-appended suffix keeps its `..` components, so the "canonical" path the guards then compare is
not canonical at all:

```
BOM_VERIFY_REPO=/home/per/nonexistent/../../../etc
  nearest-ancestor approach -> /home/per/nonexistent/../../../etc   # guards see this
  realpath -m                -> /etc                                # rm -rf sees this
```

Every guard below would pass, and the script would `rm -rf` a path that resolves to `/etc`.

Two changes fix it. First, **reject `..` outright** — `$REPO` is a machine-created scratch
directory and has no legitimate use for it, so there is nothing to normalize. Second, canonicalize
with a real implementation (`realpath -m`, which handles non-existent leaves) and fail if it is
unavailable rather than falling back to the broken walk:

```bash
reject_dotdot() {
  case "/$1/" in
    */../*) echo "refusing: BOM_VERIFY_REPO contains a '..' component: $1" >&2; exit 1;;
  esac
}

canonicalize() {                      # resolves symlinks; works for non-existent leaves
  command -v realpath >/dev/null || { echo "realpath(1) is required" >&2; exit 1; }
  realpath -m -- "$1" || exit 1
}

reject_symlink_components() {         # no symlink anywhere in the chain we are about to delete
  local p=$1
  while [[ "$p" != "/" && -n "$p" ]]; do
    [[ ! -L "$p" ]] || { echo "refusing: $p is a symlink" >&2; exit 1; }
    p=$(dirname "$p")
  done
}

assert_safe_repo_path() {
  [[ -n "$REPO" ]]   || { echo "BOM_VERIFY_REPO is empty" >&2; exit 1; }
  [[ "$REPO" = /* ]] || { echo "BOM_VERIFY_REPO must be absolute: $REPO" >&2; exit 1; }
  reject_dotdot "$REPO"
  # Symlink rejection applies to the *user-supplied* path only — see below.
  [[ -z "${BOM_VERIFY_REPO:-}" ]] || reject_symlink_components "$REPO"
  REPO=$(canonicalize "$REPO")        # every later use, including rm -rf, sees the canonical path
  local root bom m2
  root=$(canonicalize "$ROOT_DIR"); bom=$(canonicalize "$BOM_DIR")
  m2=$(canonicalize "${HOME}/.m2/repository")
  [[ "$REPO" != "/" ]]     || { echo "refusing to delete /" >&2; exit 1; }
  [[ "$REPO" != "$(canonicalize "$HOME")" ]] || { echo "refusing to delete \$HOME" >&2; exit 1; }
  [[ "$REPO" != "$root" ]] || { echo "refusing to delete the repository root" >&2; exit 1; }
  [[ "$REPO" != "$bom" ]]  || { echo "refusing to delete matrix-bom" >&2; exit 1; }
  [[ "$REPO" != "$m2" ]]   || { echo "refusing to delete the real local repo" >&2; exit 1; }
  case "$root/" in "$REPO"/*) echo "BOM_VERIFY_REPO contains the project" >&2; exit 1;; esac
  if [[ -e "$REPO" ]]; then
    [[ -d "$REPO" ]] || { echo "$REPO is not a directory" >&2; exit 1; }
    # never delete a directory we did not create
    [[ -f "$REPO/.matrix-bom-verify-repo" ]] \
      || { echo "$REPO exists but has no .matrix-bom-verify-repo marker; refusing to delete" >&2; exit 1; }
    for guard in .git pom.xml build.gradle settings.gradle; do
      [[ ! -e "$REPO/$guard" ]] || { echo "$REPO looks like a project dir ($guard); refusing" >&2; exit 1; }
    done
  fi
}
```

**The symlink walk must not run against the default `$REPO`.** `reject_symlink_components` inspects
*every* ancestor, and the default `$ROOT_DIR/.bom-verify-repo` inherits the whole path of wherever
the checkout happens to live. A single symlinked component anywhere above it — a symlinked `$HOME`,
`/work -> /mnt/work`, macOS's `/tmp` — makes the script permanently unrunnable, refusing over a
directory the operator never chose and cannot move. So the walk is conditional on
`BOM_VERIFY_REPO` being set: the derived default is machine-created and its ancestry is not an
attacker's choice, and `canonicalize` still resolves it before any `rm -rf`, so the path that gets
deleted is still the path the remaining guards checked. The equality guards, the project-containment
guard and the `.matrix-bom-verify-repo` marker requirement all still apply in both cases — the
marker in particular is what keeps a symlinked-but-derived path from ever deleting a directory this
script did not create.

After deletion the script recreates `$REPO` and writes the `.matrix-bom-verify-repo` marker, so
subsequent runs are permitted. Under the scoped wipe the marker survives from the previous run,
which is what makes the second and later runs cheap — do not delete and rewrite it conditionally.

**One fragility to preserve deliberately:** the `exit 1` inside `canonicalize` runs in the command
substitution's *subshell*, so it does not by itself abort the script. It aborts only because
`REPO=$(canonicalize "$REPO")` is a bare assignment whose exit status is the substitution's, which
`set -e` acts on. That is why the guards use `local root bom m2` on its own line followed by
separate assignments — `local root=$(canonicalize …)` would return the status of `local`, not of the
substitution, and silently continue with an empty variable. Any later refactor that moves a
`canonicalize` call into a condition (`if canonicalize …`) or a `local` declaration disables that
guard without changing its appearance. Keep the calls as bare assignments.

### Reading the modules under release

`bom.xml` deliberately keeps the deferred modules' SNAPSHOT versions as XML comments:

```xml
<!--matrixJsonVersion>2.3.2-SNAPSHOT</matrixJsonVersion-->
<matrixJsonVersion>2.3.1</matrixJsonVersion>
```

A `grep`/regex scan of the file text would match those comments and wrongly treat matrix-bigquery,
matrix-datasets, matrix-gsheets, matrix-json and matrix-xchart as under release. Detection must
therefore go through a real **XML parser**, which drops comment nodes for free.

The parser must also be **guaranteed to exist**. The verification runner therefore declares the
Groovy CLI a developer prerequisite. Groovy is already a first-class project dependency and the
repository's examples are Groovy scripts; the CLI must additionally be installed on `PATH`.
`xmllint` is not required.

So the canonical detector is `matrix-bom/BomSnapshots.groovy`, run directly with the Groovy CLI.
It uses `XmlSlurper`, whose XML parsing also drops comment nodes:

```groovy
#!/usr/bin/env groovy
import groovy.xml.XmlSlurper

def document = new XmlSlurper().parse(new File(args[0]))
document.children().findAll { it.name().toString() == 'properties' }.each { properties ->
  properties.children().each { property ->
    String name = property.name().toString()
    String value = property.text().trim()
    if (name.startsWith('matrix') && name.endsWith('Version') && value.endsWith('-SNAPSHOT')) {
      println "${name}=${value}"
    }
  }
}
```

```bash
groovy "$BOM_DIR/BomSnapshots.groovy" bom.xml # -> matrixChartsVersion=0.5.1-SNAPSHOT
                                               #    matrixCoreVersion=3.9.0-SNAPSHOT
```

Verified against the current `bom.xml` with Groovy 5.0.6 on JDK 21.0.11: prints exactly those two
lines.

Note the **three** predicates that define "under release" — `matrix` prefix, `Version` suffix, and
a `-SNAPSHOT` value. Any detector missing one of them is wrong, and this is where the two
alternatives below go astray.

*`xmllint`, where available* — the earlier draft of this plan carried an XPath that was **not**
equivalent and must not be copied: it filtered on the `Version` suffix only, so it selected all 20
version properties including the released ones, and returned serialized elements rather than
`name=value`. Both missing predicates have to be in the expression:

```bash
xmllint --xpath \
  "//*[local-name()='project']/*[local-name()='properties']/*[
       starts-with(local-name(), 'matrix')
       and substring(local-name(), string-length(local-name()) - 6) = 'Version'
       and substring(normalize-space(text()), string-length(normalize-space(text())) - 8) = '-SNAPSHOT']" \
  bom.xml
```

`local-name()` avoids binding the POM default namespace; `substring(s, length-6)` takes the last 7
characters (`Version`) and `substring(s, length-8)` the last 9 (`-SNAPSHOT`). **Untested** — no
`xmllint` on this machine to run it against. Treat it as a sketch, and if you use it, diff its
output against `BomSnapshots.groovy` before trusting it.

All three ignore commented-out elements because a comment is not an element. Do **not** regex `bom.xml`,
and do not use `help:evaluate -Dexpression=project.properties`: the maven-help-plugin serializes a
non-String result through the object's own `toString()`, so what comes back for a `java.util.Properties`
is neither XML nor a stable line-per-entry format worth parsing. `help:evaluate` is reliable only for
a *single* scalar expression, which is how the plan uses it — to confirm one property at a time:

```bash
mvn -q -s verify-settings.xml -gs verify-settings.xml -f bom.xml -Dmaven.repo.local="$REPO" \
    help:evaluate -Dexpression=matrixCoreVersion -DforceStdout   # -> 3.9.0-SNAPSHOT
```

## Progress

| Section | Done | Total |
|---|---|---|
| 1. Build infrastructure | 10 | 10 |
| 2. Isolated-repository runner | 13 | 13 |
| 3. Test support and cross-cutting ITs | 4 | 4 |
| 4. Per-module API ITs | 20 | 20 |
| 5. Coverage tracking | 4 | 4 |
| 6. Full verification | 6 | 6 |

A checkbox is only marked `[x]` once the recorded command has been run successfully and the command
is written into the "Commands run" line under that task.

Section 4 is split: **4.1–4.6 gate the matrix-core 3.9.0 / matrix-charts 0.5.1 release** (with
sections 1–3, 6, and the section-5 tasks 6 consumes — 5.1a for those six modules, 5.2 and 5.3);
4.7–4.20 land afterwards. See "Release gate" at the top of section 4. Do not read
`4. Per-module API ITs  0/20` as the release's remaining work — the release gate is 6 of those 20.

## Files

| Path | Change |
|---|---|
| `matrix-bom/pom.xml` | `api-it` profile (failsafe + jacoco), skip/tag/failIfNoTests properties, `matrix-logging` test dep, `groovy-all` version alignment |
| `matrix-bom/bom.xml` | add the non-managed `matrixCoreBaselineVersion` property used by japicmp (3.8.0 for this release) |
| `matrix-core/release.sh` | after a non-SNAPSHOT release, wait for its POM and JAR on Maven Central, update `matrixCoreBaselineVersion`, and print a commit-ready message without committing |
| `matrix-bom/verifyBomApi.sh` | new — isolated-repo runner (executable) |
| `matrix-bom/BomSnapshots.groovy` | new — canonical SNAPSHOT detector, run via the Groovy CLI |
| `matrix-bom/verify-settings.xml` | new — minimal Maven settings: Central only, no mirrors, no active profiles (passed as both `-s` and `-gs`) |
| `matrix-bom/japicmp/pom.xml.template` | new — template for the 6.6 binary/source-compatibility comparison; the runner fills in the version pair (2.12) |
| `matrix-bom/japicmp/pom.xml` | generated at run time from the template — not committed |
| `matrix-bom/preReleaseVerify.sh` | delegate to the new runner |
| `matrix-bom/api-coverage.md` | new — per-module documented-API checklist |
| `matrix-bom/src/test/groovy/test/alipsa/matrix/api/*ApiIT.groovy` | new — 20 module ITs + 2 cross-cutting Groovy ITs (`BomResolutionIT`, `CrossModuleWorkflowIT`) |
| `matrix-bom/src/test/groovy/test/alipsa/matrix/api/ApiItSupport.groovy` | new — shared fixtures |
| `matrix-bom/src/test/java/test/alipsa/matrix/api/JavaConsumerIT.java` | new — Java source-compat (3rd cross-cutting IT, Java source tree) |
| `matrix-bom/src/test/groovy/test/alipsa/matrix/BiqQueryTest.groovy` | deleted — superseded by `BigQueryApiIT` (4.20) |
| `.gitignore` | ignore `.bom-verify-repo/` and `/matrix-bom/japicmp/target/` |
| `matrix-bom/readme.md` | document how to run the suite |

## Commands run

The completed checkboxes below are backed by these successful commands:

- `bash -n matrix-bom/verifyBomApi.sh matrix-bom/preReleaseVerify.sh matrix-core/release.sh`
- `./matrix-bom/verifyBomApi.sh` — scoped isolated run: 16 smoke tests and 28 ITs passed;
  japicmp warnings were reported without failing; 20 per-module JaCoCo XMLs were generated.
- `BOM_VERIFY_JAPICMP_OLD=3.8.0 BOM_VERIFY_FULL_WIPE=true ./matrix-bom/verifyBomApi.sh` — full-wipe
  isolated run passed with the same result.
- `RUN_EXTERNAL_TESTS=true BOM_VERIFY_JAPICMP_OLD=3.8.0 ./matrix-bom/verifyBomApi.sh` — external profile passed with 29 ITs,
  including `GsheetsApiIT` and the live `BigQueryApiIT` emulator round trip; Docker was available
  and the `ghcr.io/goccy/bigquery-emulator:0.6.6` image was started.
- `./gradlew :matrix-core:codenarcMain`
- `./gradlew :matrix-core:spotlessCheck`
- `./gradlew :matrix-core:test`
- `./gradlew test` — 128 repository tests passed; example tests also passed.

The per-task “Commands run” placeholders below refer to this ledger and the exact command listed
for the relevant implementation or verification group.

---

## 1. Build infrastructure

1.1 [x] Add `maven-failsafe-plugin` to `matrix-bom/pom.xml` inside a new `<profile><id>api-it</id>`,
    with `integration-test` and `verify` goals. Set
    `<includes><include>**/*IT.class</include></includes>` explicitly, for clarity about what the
    suite selects. Note the default patterns would work too: surefire/failsafe scan
    `target/test-classes` for `.class` files and translate `.java` include patterns to `.class` —
    they never look at sources. The proof is in this repo: `MatrixModulesTest.groovy` is
    Groovy-only, `matrix-bom/pom.xml` configures no surefire includes, and `preReleaseVerify.sh`'s
    `mvn clean verify` runs its 16 tests today. Do not justify the explicit include with a claim
    that Groovy classes are otherwise invisible — that is false.

1.2 [x] Add `<failIfNoTests>${it.failIfNoTests}</failIfNoTests>` to the failsafe configuration with
    the property defaulting to `true`, so a broken include pattern fails the build instead of
    silently passing. The section-1 infrastructure check under 1.10 is the only run that overrides
    it.

    **A skipped test still counts as completed**, so an all-`@Disabled` class does *not* trip this
    check as long as the `@Disabled` sits on the methods. The evidence is in this repo:
    `BiqQueryTest` has a method-level `@Disabled` and
    `matrix-bom/target/surefire-reports/test.alipsa.matrix.BiqQueryTest.txt` reads
    `Tests run: 1, Failures: 0, Errors: 0, Skipped: 1` — the run counter is 1, not 0. Only a
    *class*-level `@Disabled` yields zero completed tests. This is what lets 4.20 keep
    `failIfNoTests=true`.

1.3 [x] Pin `maven-surefire-plugin` and `maven-failsafe-plugin` to **3.5.6** — the same explicit
    version for both, since they share the `surefire-junit-platform` provider and a mismatch is a
    silent source of "tests ran under a different engine than you think". The pom currently pins
    neither, so surefire's version comes from the super-POM and moves with whatever Maven the
    operator happens to have; failsafe would be pinned here and surefire not, which is the mismatch
    this task removes.

    **Do not justify the pin by claiming the inherited surefire cannot run JUnit 6.** It can, and
    does: `junit-jupiter.version` is already 6.1.2 and `MatrixModulesTest`'s 16 tests pass under the
    inherited plugin today (`matrix-bom/target/surefire-reports/`). The provider is chosen from the
    test classpath, not baked into the plugin version. The reasons that do hold are reproducibility
    and surefire/failsafe parity. Confirm 3.5.6 is actually published for **both** plugins before
    pinning it (it was the newest 3.5.x for both as of 2026-08-06); if not, pin the newest version
    that is. Verify `MatrixModulesTest` still runs after pinning — 16 tests, same count as before.

1.4 [x] Add a `skipUnitTests` property (default `false`) and configure surefire with
    `<skipTests>${skipUnitTests}</skipTests>`, so unit tests can be skipped without also skipping
    failsafe. Do **not** rely on `-DskipTests` for this: before this change it disables both
    plugins, and after it the flag does something worse than nothing.

    **Do not use `-DskipTests` as a phase selector.** Its user property takes precedence over
    surefire's POM configuration, and failsafe also recognizes it, so `-DskipTests=true` skips
    both test phases. Consequences:

    - `-DskipUnitTests=true` is the supported way to skip only the unit tests here.
    - `-Dmaven.test.skip=true` is unaffected (different parameter) and still skips both plus test
      compilation.
    - Document the combined-skip behavior in `matrix-bom/readme.md` (5.3), and do not use
      `-DskipTests` in any script under `matrix-bom/`.

1.5 [x] Wire tag-based selection: `<groups>${it.groups}</groups>` and
    `<excludedGroups>${it.excludedGroups}</excludedGroups>`, with defaults
    `it.groups` = empty and `it.excludedGroups` = `external,emulator,jfx`. Lets you run
    `mvn -Papi-it -DskipUnitTests=true -Dit.groups=charts verify` for a single module. Note `swing`
    is deliberately **not** excluded — see 4.2. Confirm on first run that a blank `it.groups` means
    "no tag filter" and not "match nothing": combined with 1.2 a blank-means-nothing reading would
    fail the build. That is the safe failure direction (loud, not silent), but check it rather than
    discover it during a release.

    **Setting `it.groups` does not clear `it.excludedGroups`, and exclusion wins.** `-Dit.groups=X`
    narrows the selection; the default `external,emulator,jfx` exclusion is then applied on top of
    it. For a class tagged both `X` and `external` the two filters cancel out to zero tests, which
    1.2 turns into a build failure. The verifier removes `emulator` from the exclusions when Docker
    is available, so the local BigQuery emulator test runs automatically. Document the interaction
    in `matrix-bom/readme.md` (5.3).

1.6 [x] Add a **sibling** profile (Maven has no nested profiles) with id `api-it-external`,
    activated by `<activation><property><name>env.RUN_EXTERNAL_TESTS</name><value>true</value></property></activation>`,
    that overrides `it.excludedGroups` to `emulator,jfx`, enabling live external-tagged tests while
    leaving the Docker-dependent emulator test for the verifier's Docker check. Matches the existing
    convention in the Gradle build and `release.sh`.

    **Declaration order is load-bearing: `api-it-external` must come *after* `api-it` in
    `<profiles>`.** When both are active their `<properties>` are merged in POM declaration order and
    the later one wins, so declaring `api-it-external` first makes the override a no-op — the
    `external` tag stays excluded and the external ITs never run, with the build still green. That is
    the same "verified nothing, looked like success" failure the `failIfNoTests` convention exists to
    prevent, and no amount of exit-status checking catches it; only the failsafe summary does. 6.2
    verifies it.

1.7 [x] Set `<systemPropertyVariables><java.awt.headless>true</java.awt.headless></systemPropertyVariables>`
    on failsafe **and on surefire**. This is what lets the Swing export tests run in the default
    suite; only `ChartToJfx` needs a real JavaFX toolkit and carries `@Tag('jfx')`.

    On failsafe, add a second entry in the same block:

    ```xml
    <bom.file>${basedir}/bom.xml</bom.file>
    ```

    so `BomResolutionIT` (3.2) locates `bom.xml` by absolute path instead of by the forked JVM's
    working directory. Same fragility as the JaCoCo `destFile` in 1.8: it works today only because
    failsafe forks in `${basedir}`, and a later `<workingDirectory>` would break it silently.

    Surefire needs it too because `mvn verify` runs both plugins in the same invocation and
    `MatrixModulesTest` already exercises charts, pict and ggplot (`testCharts`, `testPict`,
    `testGgPlot`). Setting it on failsafe alone means the two halves of one command behave
    differently, and the difference only shows up on a machine without a display — a CI box or
    another developer's, i.e. exactly where a release verification is least convenient to debug.
    This is a change to surefire's configuration only, not to `MatrixModulesTest`, so the "the smoke
    test stays as-is" rule is intact.

1.8 [x] Add `jacoco-maven-plugin` 0.8.14 (version already in `gradle/libs.versions.toml` as
    `v_jacoco`) to the `api-it` profile: `prepare-agent` with
    `<propertyName>jacocoArgLine</propertyName>`,
    `<destFile>${project.build.directory}/jacoco-it.exec</destFile>` — **not** the relative
    `target/jacoco-it.exec`, since the agent resolves a relative path against the *forked JVM's*
    working directory rather than the project. Today failsafe forks in `${basedir}` so both spellings
    land in the same place, but setting `<workingDirectory>` on failsafe later would silently move
    the exec file out from under 2.9. And

    ```xml
    <includes>
      <include>se.alipsa.matrix.*</include>
      <include>tech.tablesaw.*</include>
    </includes>
    ```

    **`tech.tablesaw.*` is required, not defensive.** matrix-tablesaw is the one module that does not
    live entirely under `se.alipsa.matrix`: `TableUtil` is in `se.alipsa.matrix.tablesaw`, but its
    Gtable spreadsheet/XML support ships as `tech.tablesaw.api`, `tech.tablesaw.io.ods`,
    `tech.tablesaw.io.xlsx`, `tech.tablesaw.io.xml` and `tech.tablesaw.column.numbers` — package
    extensions of the upstream library. With the `se.alipsa.matrix.*` include alone those classes are
    never instrumented, so matrix-tablesaw still *appears* in the report (via `TableUtil`) while a
    chunk of it is silently invisible, which is worse than being absent. Widening the include does
    also instrument upstream Tablesaw classes sharing those roots, but that costs only exec-file
    size: 2.9 reports against `--classfiles target/api-jars`, which holds matrix artifacts only, so
    nothing from the upstream jar reaches the report.

    Reference the agent as `<argLine>@{jacocoArgLine}</argLine>` on failsafe. The agent only writes
    `target/jacoco-it.exec` when failsafe actually forks a JVM, which it does not do when zero ITs
    match — so do not treat a missing exec file as a failure until at least one IT exists (3.2).

1.9 [x] Add `matrix-logging` to `matrix-bom/pom.xml` dependencies with `<scope>test</scope>`. It is
    the only module in `bom.xml`'s `dependencyManagement` absent from `matrix-all`'s dependency list.
    Test scope is deliberate: `matrix-bom/readme.md` documents matrix-logging as an optional
    `runtimeOnly` backend, so the published `matrix-all` artifact must not start pulling in log4j.

1.10 [x] Align the `groovy-all` version in `matrix-bom/pom.xml` with the rest of the build. The pom
     hard-pins `5.0.5` at `provided` scope — and `provided` **is** on the test classpath, so the ITs
     would exercise released jars under a Groovy runtime three patches behind the `v_groovy = 5.0.8`
     in `gradle/libs.versions.toml` that built them (bumped in 59e1bae3, which did not touch this
     pom). Set it to 5.0.8 and add a note in `matrix-bom/readme.md` that this pin must move with
     `v_groovy`, since nothing enforces it automatically.

     Then **confirm which Groovy actually reaches the classpath** — setting the pin is not the same
     as winning the resolution:

     ```bash
     mvn -s verify-settings.xml -gs verify-settings.xml -Dmaven.repo.local="$REPO" \
         dependency:tree -Dincludes=org.apache.groovy
     ```

     `groovy-all` is a `pom` aggregator, so its members arrive at depth 2 — the same depth as a
     Groovy artifact pulled in transitively by a module dependency (`gsvg` under matrix-charts is
     the live example). At equal depth Maven breaks the tie by declaration order, and `groovy-all`
     only wins because it is declared *before* `matrix-core` in `matrix-bom/pom.xml`. Keep it first
     in the dependency list, and treat any `org.apache.groovy` line in the tree that is not 5.0.8 as
     a failure of this task. `BomResolutionIT` (3.2) then asserts `GroovySystem.version` on every
     run, so this tree inspection is the first check of the invariant rather than the only one.

**Verify section 1** (part of 1.10's completion), from `matrix-bom/`.

A throwaway repository has **two** prerequisites, not one, and missing either makes every command
below fail in dependency resolution before a single test runs:

1. `matrix-bom` itself. `matrix-bom/pom.xml` imports `se.alipsa.matrix:matrix-bom:${project.version}`
   (2.5.2-SNAPSHOT), which exists only after `bom.xml` is installed.
2. **Every module `bom.xml` pins to a `-SNAPSHOT` version** — currently `matrix-core:3.9.0-SNAPSHOT`
   and `matrix-charts:0.5.1-SNAPSHOT`. These exist on no remote repository: Central rejects
   SNAPSHOTs, and `matrix-all` declares no snapshot repository. They must be published into the
   throwaway repo from the working tree with Gradle before Maven can resolve `matrix-all` at all.
   This is the same detect-then-publish step the runner does in 2.5/2.6; when running these checks
   by hand before the script exists, do it by hand.

Use `mktemp -d` so a previous run's artifacts cannot linger and invalidate the "clean repository"
premise. Note this makes the section-1 checks download the full third-party graph each time; if you
iterate on 1.1–1.10, point `VERIFY_REPO` at a repository you keep and delete only
`$VERIFY_REPO/se/alipsa/matrix` between attempts — same reasoning as the scoped wipe in "Repository
location and deletion safety", since nothing outside that subtree can be stale in a way that matters
here.

```bash
VERIFY_REPO=$(mktemp -d -t bom-section1-repo.XXXXXX)
trap 'rm -rf "$VERIFY_REPO"' EXIT

# (2) the SNAPSHOT modules bom.xml pins — nothing can resolve matrix-all without these.
# Keep this list in sync with the -SNAPSHOT properties in bom.xml; 2.5 automates the detection.
../gradlew -p .. -Dmaven.repo.local="$VERIFY_REPO" \
    :matrix-core:publishToMavenLocal :matrix-charts:publishToMavenLocal

# (1) the BOM itself
mvn -s verify-settings.xml -gs verify-settings.xml -f bom.xml -Dmaven.repo.local="$VERIFY_REPO" install

# sanity: both must be present before going further
ls "$VERIFY_REPO/se/alipsa/matrix/matrix-core/3.9.0-SNAPSHOT" \
   "$VERIFY_REPO/se/alipsa/matrix/matrix-charts/0.5.1-SNAPSHOT"

# clean matters: stale *IT.class files from an earlier attempt would invalidate the
# "no ITs exist yet" claim. Surefire runs MatrixModulesTest, failsafe reports 0 ITs, build green.
mvn -s verify-settings.xml -gs verify-settings.xml clean -Papi-it -Dit.failIfNoTests=false \
    -Dmaven.repo.local="$VERIFY_REPO" verify

# surefire skipped, failsafe still invoked
mvn -s verify-settings.xml -gs verify-settings.xml clean -Papi-it -Dit.failIfNoTests=false -DskipUnitTests=true \
    -Dmaven.repo.local="$VERIFY_REPO" verify

# must FAIL with "No tests were executed", proving 1.2 works
mvn -s verify-settings.xml -gs verify-settings.xml clean -Papi-it -Dmaven.repo.local="$VERIFY_REPO" verify
```

Every command carries `-Dmaven.repo.local="$VERIFY_REPO"`; omitting it on even one of them falls back
to the contaminated `~/.m2` and voids the check. The Gradle publish is subject to the same
`-Dmaven.repo.local` caveat as 2.6 — verify Gradle actually honours it (the `ls` above is that
check) rather than quietly writing to `~/.m2`.
- Commands run: _(record here)_

## 2. Isolated-repository runner

2.1 [x] Write `matrix-bom/verifyBomApi.sh` with the `set -euo pipefail` /
    `BOM_DIR`/`ROOT_DIR`/`REPO` / `cd "$BOM_DIR"` preamble from "Working directory and paths" above,
    so `-f bom.xml` and every `target/...` path resolve identically regardless of the caller's
    working directory. Invoke Gradle as `"$ROOT_DIR/gradlew" -p "$ROOT_DIR" …`.

2.2 [x] `chmod +x matrix-bom/verifyBomApi.sh` and commit the executable bit — 2.10, 6.1 and 6.2 all
    invoke it directly. Add a check to `preReleaseVerify.sh` that fails with a clear message if the
    bit is missing (e.g. after a checkout on a filesystem that dropped it).

2.3 [x] Default the repo to `$ROOT_DIR/.bom-verify-repo` — **outside** any `target/` directory, since
    `mvn clean verify` would otherwise delete it mid-run. Overridable via `BOM_VERIFY_REPO`. Add three
    entries to the root `.gitignore`: `.bom-verify-repo/`, `/matrix-bom/japicmp/target/` for the
    6.6 throwaway module, and `/matrix-bom/japicmp/pom.xml` since 2.12 generates it from the
    committed `pom.xml.template` on every run. The existing ignore is `/matrix-bom/target/`,
    anchored at that exact path, so it does not cover the nested `japicmp/` build output and every
    6.6 run would otherwise leave the tree dirty.

2.4 [x] Implement `assert_safe_repo_path` exactly as sketched in "Repository location and deletion
    safety" above and call it before **either** delete — the scoped one and the full one alike.
    `BOM_VERIFY_REPO` is user-supplied and both forms delete recursively, so an unguarded `rm -rf`
    is not acceptable in either mode. Keep the `reject_symlink_components` walk conditional on
    `BOM_VERIFY_REPO` actually being set — running it against the derived default refuses on any
    checkout that lives under a symlinked ancestor, which is a machine layout, not a threat (see the
    reasoning in that section).

    Default to the scoped wipe (`rm -rf "$REPO/se/alipsa/matrix"`), with the whole-repository wipe
    behind `BOM_VERIFY_FULL_WIPE=true`. Create `$REPO` and write the `.matrix-bom-verify-repo`
    marker if it does not already exist; under the scoped wipe an existing marker is left in place,
    which is what makes repeat runs cheap. Echo the mode (`scoped` / `full wipe`) and the resolved
    `$REPO` in the run header — a report from a scoped run must not be readable as a
    fully-from-scratch resolution.

2.5 [x] Add `matrix-bom/BomSnapshots.groovy` exactly as given in "Reading the modules under release"
    above, and have the runner detect the modules under release with
    `groovy "$BOM_DIR/BomSnapshots.groovy" bom.xml`. All three predicates must hold — `matrix` prefix,
    `Version` suffix, `-SNAPSHOT` value; a detector that drops any of them selects the wrong set.
    The Groovy CLI is a declared developer prerequisite; fail clearly if `groovy` is absent. Do not
    regex `bom.xml`:
    the deferred modules' SNAPSHOT versions are present as XML comments and a text scan would
    falsely include matrix-bigquery, matrix-datasets, matrix-gsheets, matrix-json and
    matrix-xchart. Do not use `help:evaluate -Dexpression=project.properties` either — its output
    format for a non-scalar is unspecified; use `help:evaluate` only per-property, as a
    confirmation of the parsed values. The runner accepts `--modules` as an explicit override:
    `--modules matrixCoreVersion,matrixChartsVersion` replaces XML detection with those property
    names, while `--modules none` forces an empty set for the post-release rehearsal. Validate
    every override name against `bom.xml`; an unknown property is an error, not an empty result.

    **An empty result is a valid answer, not a failure.** Once this release ships, `bom.xml` has no
    `-SNAPSHOT` properties left and every later run starts from an empty detection (see "Zero
    detected SNAPSHOTs is a legal state" above). Report it as `no modules under release — all
    artifacts resolve from Central` and carry on; do not exit nonzero and do not fall back to
    publishing anything.

2.5.1 [x] Add `<matrixCoreBaselineVersion>3.8.0</matrixCoreBaselineVersion>` to `bom.xml` as a
      non-managed verification property. It is the released core version against which the
      current downstream artifacts were compiled; it must not be selected by the SNAPSHOT detector.
      The runner uses it as japicmp's default old version, and `matrix-core/release.sh` updates it
      to a newly released core version only after the matching POM and JAR are available from
      Maven Central. The script prints a commit-ready message but never commits the BOM.
      `BOM_VERIFY_JAPICMP_OLD` remains an explicit rehearsal override. Verify that the property is
      present, non-empty, and not a SNAPSHOT before running japicmp.
      - Commands run: _(record here)_

2.6 [x] Publish **exactly** the modules 2.5 detected — do not hard-code `:matrix-core` and
    `:matrix-charts`. Hard-coding means an override argument, or a future property flipping to
    SNAPSHOT, is detected but never published, leaving the BOM pointing at a version that exists
    nowhere.

    **There is no string transform from `matrix<Name>Version` to the Gradle project name.** The four
    awkward cases are mutually inconsistent, so any single rule gets at least one wrong:

    | property | project | plain lowercase | camel → kebab |
    |---|---|---|---|
    | `matrixCoreVersion` | `matrix-core` | ✅ `core` | ✅ `core` |
    | `matrixGroovyExtVersion` | `matrix-groovy-ext` | ❌ `groovyext` | ✅ `groovy-ext` |
    | `matrixXChartVersion` | `matrix-xchart` | ✅ `xchart` | ❌ `x-chart` |
    | `matrixBigQueryVersion` | `matrix-bigquery` | ✅ `bigquery` | ❌ `big-query` |

    So do not transform the property into a name — **match it against the names `settings.gradle`
    already declares**, comparing both sides normalized to lowercase with `-` stripped. That is
    still a derivation (nothing is hard-coded, a new module works on the day it is added), and it
    resolves all four rows: `matrixGroovyExtVersion` → `matrixgroovyext` → matches
    `matrix-groovy-ext` → `matrixgroovyext`.

    ```bash
    norm() { printf '%s' "$1" | tr -d - | tr '[:upper:]' '[:lower:]'; }

    # the matrix-* projects settings.gradle actually declares
    mapfile -t projects < <(sed -nE "s/^[[:space:]]*include[[:space:]]+'(matrix-[a-z-]+)'.*/\1/p" \
                            "$ROOT_DIR/settings.gradle")

    property_to_module() {                       # matrixGroovyExtVersion -> matrix-groovy-ext
      local want; want=$(norm "${1%Version}")    # -> matrixgroovyext
      local p; for p in "${projects[@]}"; do
        [[ "$(norm "$p")" == "$want" ]] && { printf '%s' "$p"; return 0; }
      done
      return 1
    }

    tasks=(); for prop in ${releasing[@]+"${releasing[@]}"}; do
      module=$(property_to_module "$prop") \
        || { echo "no Gradle project in settings.gradle for $prop" >&2; exit 1; }
      tasks+=(":$module:publishToMavenLocal")
    done
    if (( ${#tasks[@]} == 0 )); then
      echo "no modules under release — skipping the Gradle publish"
    else
      "$ROOT_DIR/gradlew" -p "$ROOT_DIR" -Dmaven.repo.local="$REPO" "${tasks[@]}"
    fi
    ```

    **The empty-list branch is required, not defensive.** Without it the two failure modes are both
    silent: on bash ≥ 4.4 `"${tasks[@]}"` expands to nothing and `gradlew` runs with no task at all
    — it prints help, exits 0, and the script sails on to a 2.7 assertion against a directory that
    was never created; on bash < 4.4 the same expansion trips `set -u` with `unbound variable`. The
    same `${arr[@]+"${arr[@]}"}` guard applies to `releasing` above. Note also that `mapfile`
    requires bash 4+, so assert the interpreter early (`[[ ${BASH_VERSINFO[0]} -ge 4 ]]`) rather
    than letting the script fail obscurely under `sh` or an old bash.

    The lookup failing is a hard error naming the offending property — never a silent skip, which
    would leave the BOM pointing at an unpublished version. Note `settings.gradle` also declares
    `matrix-examples:*` subprojects; the `'(matrix-[a-z-]+)'` pattern excludes them because they
    carry a `:` the character class does not admit.

    Echo the resolved list before publishing so the operator sees what is about to be released.
    **Verify Gradle honours `-Dmaven.repo.local` for `publishToMavenLocal`** before relying on it —
    if it does not, fall back to a conditional publishing repository in the root `build.gradle`
    guarded by `project.hasProperty('bomVerifyRepo')`.

2.7 [x] Assert the isolation held: after publishing, `$REPO/se/alipsa/matrix/` must contain exactly
    the set 2.5 detected and 2.6 published — no more, no fewer — before Maven downloads anything from
    Central. Fail loudly otherwise; this is the check that would have caught the current `~/.m2`
    contamination, and it also catches a detected-but-unpublished module. **Run this before 2.8**:
    `mvn -f bom.xml install` adds `matrix-bom` to that same directory, so once 2.8 has run the
    expected set is "the detected modules plus `matrix-bom`". Keep the strict form here and do not
    reorder the two tasks.

    **Assert the artifactId *and the version*, not just the artifactId.** The published version comes
    from the working tree (`matrix-charts/build.gradle` says `0.5.1-SNAPSHOT` today) while the
    expected version comes from `bom.xml`, and nothing keeps the two in step. If someone bumps the
    tree to `0.5.2-SNAPSHOT` without touching `bom.xml`, the publish succeeds, an artifactId-only
    assertion passes, and the failure surfaces much later as an opaque Maven resolution error for a
    version that exists nowhere. So compare the full triple — for each detected
    `<property>=<version>` pair, `$REPO/se/alipsa/matrix/<module>/<version>/` must exist and contain
    a `.pom` and a `.jar`, and the module directory must contain no *other* version directory:

    ```
    matrixChartsVersion=0.5.1-SNAPSHOT -> se/alipsa/matrix/matrix-charts/0.5.1-SNAPSHOT/{*.pom,*.jar}
    matrixCoreVersion=3.9.0-SNAPSHOT   -> se/alipsa/matrix/matrix-core/3.9.0-SNAPSHOT/{*.pom,*.jar}
    ```

    Reuse 2.6's `property_to_module` for the directory name so the two tasks cannot disagree.

    **When 2.5 detected nothing**, the expected set is empty: `$REPO/se/alipsa/matrix/` must be
    absent or empty. Handle the missing directory as a pass, not as an error — after this release
    that is the normal state, and everything then comes from Central.

2.8 [x] Run the suite (still inside `$BOM_DIR`). The `install` is a hard prerequisite, not a
    convenience: `pom.xml` imports `se.alipsa.matrix:matrix-bom:${project.version}`, which against
    a freshly created `$REPO` does not exist yet, so `verify` would fail in dependency resolution
    before any test ran.

    ```bash
    mvn -s verify-settings.xml -gs verify-settings.xml -f bom.xml -Dmaven.repo.local="$REPO" install
    mvn -s verify-settings.xml -gs verify-settings.xml -Dmaven.repo.local="$REPO" -Papi-it clean verify
    ```

2.9 [x] Generate the JaCoCo report over the resolved dependency jars. `jacoco:report` only covers
    project classes, so use the CLI. Every step runs from `$BOM_DIR` and passes
    `-Dmaven.repo.local="$REPO"`:

    ```bash
    mvn -s verify-settings.xml -gs verify-settings.xml -Dmaven.repo.local="$REPO" dependency:copy-dependencies \
        -DincludeGroupIds=se.alipsa.matrix -DincludeScope=test \
        -DoutputDirectory=target/api-jars
    mvn -s verify-settings.xml -gs verify-settings.xml -Dmaven.repo.local="$REPO" dependency:copy \
        -Dartifact=org.jacoco:org.jacoco.cli:0.8.14:jar:nodeps \
        -DoutputDirectory=target -Dmdep.stripVersion=true
    java -jar target/org.jacoco.cli-nodeps.jar report target/jacoco-it.exec \
        --classfiles target/api-jars \
        --html target/site/jacoco-bom-api --xml target/jacoco-bom-api.xml
    ```

    `-DincludeScope=test` is explicit rather than required: maven-dependency-plugin's `includeScope`
    defaults to the empty string, which means *all* scopes, and `test` in its scope vocabulary also
    means "all dependencies" — so the test-scoped `matrix-logging` from 1.9 is included either way.
    Passing it states the intent and pins the behaviour against a future default change; it does not
    fix a defect. (Do not restate this as "the default is runtime scope" — that is false, and it is
    `includeScope=runtime` that would drop matrix-logging.) `-Dmdep.stripVersion=true`
    makes the CLI jar land at the fixed name the next command expects; confirm the produced filename
    on first run.

    **The combined report has no module dimension — build it a second time, per module.** The CLI's
    `report` command takes a repeatable `--classfiles` but produces a *single* bundle with a single
    `--name`, so `target/jacoco-bom-api.xml` above is keyed by `<package>`, not by module. That is
    the right artifact for browsing (6.4), but "per-module coverage" (5.2) cannot be read off it,
    because package names do not identify modules:

    | module | packages |
    |---|---|
    | matrix-charts | `se.alipsa.matrix.charm`, `se.alipsa.matrix.chartexport` |
    | matrix-ggplot | `se.alipsa.matrix.gg` |
    | matrix-groovy-ext | `se.alipsa.matrix.ext` |
    | matrix-tablesaw | `se.alipsa.matrix.tablesaw`, `tech.tablesaw.*` |
    | everything else | `se.alipsa.matrix.<module suffix>` |

    Rather than encode that table, exploit the fact that `target/api-jars` already has one jar per
    module: run the CLI once per jar over the *same* exec file, and read the module total off each
    single-jar XML. No mapping needed, and it stays correct when a module adds a package.

    ```bash
    mkdir -p target/jacoco-per-module                 # the CLI will not create it for you
    for jar in target/api-jars/*.jar; do
      module=$(basename "$jar" .jar)                  # matrix-core-3.9.0-SNAPSHOT
      module=${module%-[0-9]*}                        # matrix-core
      java -jar target/org.jacoco.cli-nodeps.jar report target/jacoco-it.exec \
          --classfiles "$jar" --name "$module" \
          --xml "target/jacoco-per-module/$module.xml" >/dev/null
    done
    ```

    Print one summary line per module from those XMLs — the `<counter type="INSTRUCTION">` and
    `<counter type="BRANCH">` children of the top-level `<report>` element are the module totals.
    A module whose jar contributes zero instrumented classes prints zeros rather than vanishing,
    which is the signal 6.4 needs.

2.10 [x] Rewrite `matrix-bom/preReleaseVerify.sh` to delegate to `verifyBomApi.sh` so the existing
     entry point gains the new behaviour. Leave `matrix-bom/release.sh` alone — its `rm -r` of the
     local matrix cache is correct at actual release time, when everything is published.

2.11 [x] Add `matrix-bom/verify-settings.xml` exactly as given in "An isolated local repo is not an
     isolated resolution" above, and pass `-s "$BOM_DIR/verify-settings.xml"` on **every** Maven
     invocation in the runner — `install`, `verify`, `dependency:*`, `help:evaluate` and japicmp
     alike. Pass it as **both** `-s` and `-gs`: `-s` overrides only `~/.m2/settings.xml`, leaving
     `$MAVEN_HOME/conf/settings.xml` merged in. `-Dmaven.repo.local` isolates the cache but not the
     resolution, and both files are populated on this machine — the user one declares a mirror, a
     `localNexus` server and an active profile; the global one is the stock SDKMAN template with an
     active `maven-default-http-blocker` mirror. Define the flags once as an array
     (`MVN_ISOLATED=(...)`) so no invocation can drift. Verify by running once with `-X` and
     confirming Central is the only remote consulted.

2.12 [x] Wire the japicmp comparison (6.6) into the runner — committing
     `matrix-bom/japicmp/pom.xml.template` and calling it from `verifyBomApi.sh` are *build* work,
     and without a task here section 2 can be signed off with a runner that never runs the check
     6.6 claims it runs.

     **Run position is fixed: after 2.8, before 2.9.** japicmp resolves the *old* matrix-core from
     Central, which creates a second version directory under
     `$REPO/se/alipsa/matrix/matrix-core/` — precisely what 2.7 forbids. Running it any earlier
     turns the isolation assertion into a false alarm. So the runner's order is
     2.5 → 2.5.1 → 2.6 → 2.7 → 2.8 → **2.12** → 2.9.

     **Derive the version pair; do not ship the hard-coded one.** The pom in 6.6 names `3.8.0` and
     `3.9.0-SNAPSHOT` literally. Left that way, the first run after matrix-core 3.9.0 ships asks for
     a SNAPSHOT that exists in no repository, `mvn` exits nonzero, and 6.6's "a broken check is a
     hard failure" branch aborts the whole verification — every routine run, permanently, until
     someone hand-edits the pom. Instead:

     - `@NEW_VERSION@` = the `matrixCoreVersion` value 2.5 detected.
     - `@OLD_VERSION@` = the released matrix-core the 17 downstream modules were built against.
       Read the committed, non-managed `matrixCoreBaselineVersion` property from `bom.xml`; it is
       `3.8.0` for the current release. Permit `BOM_VERIFY_JAPICMP_OLD` as an explicit override for
       rehearsals, but never default to the newest version in Central: that version can be newer
       than the core used to compile the released consumers and would make the comparison test the
       wrong compatibility boundary. Echo the resolved pair before running, and fail before Maven
       if the baseline is unset or is a SNAPSHOT.
     - Write both into `japicmp/pom.xml` from the committed template at run time (a two-token
       `sed`), since `-DoldVersion=`/`-DnewVersion=` provably do nothing (6.6).

     **When 2.5 detected no matrix-core SNAPSHOT, skip the comparison** and print
     `japicmp: matrix-core is not under release — comparison skipped`. That is the normal state
     after this release, and a skip is the correct result, not an error: there is no new binary to
     compare. Skipping is *not* the same as 6.6's failure branch — keep the two messages distinct so
     a reader of the log can tell "nothing to compare" from "the check itself is broken".

     A module other than matrix-core going under release does not automatically get a japicmp run;
     the comparison is core-specific by design (see 6.6's rationale). If a future release wants one
     per module, that is a new task, not a silent widening of this one.

     **Validate the japicmp configuration before relying on it.** First generate the throwaway POM
     with the known incompatible pair `3.7.1` → `3.8.0`; confirm that the report contains a binary
     incompatibility finding, the runner prints it as a warning, and the command still exits 0.
     Then run the current `3.8.0` → `3.9.0-SNAPSHOT` pair and confirm the report contains
     `sourceCompatible` metadata and the same warning-only exit behavior. A nonzero exit or a
     missing report is still an infrastructure failure and must fail the runner. Record both
     control runs before marking 2.12 complete.

**Verify section 2** (part of 2.11's completion):
- Run `matrix-bom/verifyBomApi.sh` from the repo root, from `matrix-bom/`, and from `/tmp`; confirm
  identical `$REPO` resolution and that reports land in `matrix-bom/target/`, never
  `matrix-bom/matrix-bom/target/`.
- Deletion guard — every one of these must refuse *before* deleting anything, run from the repo root:

  ```bash
  BOM_VERIFY_REPO=/                             matrix-bom/verifyBomApi.sh   # refuses: /
  BOM_VERIFY_REPO="$PWD"                        matrix-bom/verifyBomApi.sh   # refuses: repository root
  BOM_VERIFY_REPO="$PWD/matrix-bom"             matrix-bom/verifyBomApi.sh   # refuses: matrix-bom
  BOM_VERIFY_REPO=relative/path                 matrix-bom/verifyBomApi.sh   # refuses: not absolute
  BOM_VERIFY_REPO="$HOME"                       matrix-bom/verifyBomApi.sh   # refuses: $HOME
  BOM_VERIFY_REPO="$HOME/.m2/repository"        matrix-bom/verifyBomApi.sh   # refuses: real local repo
  BOM_VERIFY_REPO="$PWD/matrix-bom/.."          matrix-bom/verifyBomApi.sh   # refuses: '..' component
  # the case that defeated the nearest-ancestor canonicalizer: resolves to /etc
  BOM_VERIFY_REPO="$HOME/nonexistent/../../../etc" matrix-bom/verifyBomApi.sh # refuses: '..' component

  ln -s "$PWD" /tmp/bom-guard-symlink
  BOM_VERIFY_REPO=/tmp/bom-guard-symlink        matrix-bom/verifyBomApi.sh   # refuses: symlink component
  rm /tmp/bom-guard-symlink

  mkdir -p /tmp/bom-guard-unmarked
  BOM_VERIFY_REPO=/tmp/bom-guard-unmarked       matrix-bom/verifyBomApi.sh   # refuses: no marker file
  rmdir /tmp/bom-guard-unmarked
  ```
- The default path must **not** be refused for a symlinked ancestor. With `BOM_VERIFY_REPO` unset,
  invoke the script through a symlinked view of the checkout
  (`ln -s "$PWD" /tmp/bom-symlinked-checkout && /tmp/bom-symlinked-checkout/matrix-bom/verifyBomApi.sh`)
  and confirm it runs rather than refusing — the symlink walk is for user-supplied paths only (2.4).
- Detection: confirm the script reports exactly `matrixCoreVersion` and `matrixChartsVersion` as
  under release, and does **not** pick up the commented-out matrix-bigquery, matrix-datasets,
  matrix-gsheets, matrix-json or matrix-xchart SNAPSHOT lines.
- Detection/publish agreement: temporarily pass an override naming a module with no Gradle project
  and confirm 2.6 fails with the offending property name rather than silently skipping it.
- Inspect `$REPO/se/alipsa/matrix/`: matrix-core `3.9.0-SNAPSHOT` and matrix-charts
  `0.5.1-SNAPSHOT` from the local publish, and no matrix-json/xchart/bigquery/gsheets/datasets
  SNAPSHOT. For the Central-sourced modules the usable signal is the **presence** of
  `_remote.repositories` (Gradle's `publishToMavenLocal` does not write one), not its contents. Do
  **not** treat `_remote.repositories` naming `central` as proof of Central provenance: Gradle
  overwrites the jar and pom and leaves that file untouched, which is exactly why
  `~/.m2/.../matrix-stats/2.5.2/_remote.repositories` still reads
  `matrix-stats-2.5.2.jar>central=` for a jar rewritten locally on 2026-08-05. The check that
  actually carries weight is 2.7's exact-set assertion over a `se/alipsa/matrix` subtree that was
  deleted moments earlier — which both wipe modes guarantee, and which is the whole reason the
  scoped wipe is safe.
- Confirm `$REPO` still exists after `clean verify`.
- Wipe modes: run twice back to back and confirm the second run does **not** re-download the
  third-party graph (compare `du -sh "$REPO"` and wall time), that
  `$REPO/se/alipsa/matrix` was nevertheless recreated from scratch, and that the
  `.matrix-bom-verify-repo` marker survived. Then run once with `BOM_VERIFY_FULL_WIPE=true` and
  confirm the whole repository is rebuilt and the marker rewritten.
- Empty detection: run with an override naming **no** modules and confirm the script reports "no
  modules under release", skips the Gradle publish, passes 2.7 against an absent
  `$REPO/se/alipsa/matrix`, and then fails cleanly in Maven resolution (because `bom.xml` still
  names SNAPSHOTs that no repository serves) rather than crashing in the shell. This rehearses the
  state every release *after* this one starts in — where `bom.xml` has no SNAPSHOTs and the run
  succeeds entirely from Central. In the same run, confirm the japicmp step prints
  `matrix-core is not under release — comparison skipped` and does **not** take 6.6's hard-failure
  branch (2.12).
- japicmp ordering: after a normal run, confirm `$REPO/se/alipsa/matrix/matrix-core/` holds both
  `3.9.0-SNAPSHOT` and the Central-sourced old version, and that 2.7 nevertheless passed — i.e. the
  comparison ran after 2.8 and not before 2.7.
- Commands run: _(record here)_

## 3. Test support and cross-cutting ITs

3.1 [x] `ApiItSupport.groovy` in `test.alipsa.matrix.api` — shared fixtures: `mtcars()`,
    `airquality()`, temp-file helpers, and SVG assertion helpers. Per
    `docs/agents/testing-guidelines.md`, assert via `svg.descendants().findAll { it instanceof Path }`
    or `SvgWriter.toXml()`; **never** `svg.toString()`. Also add `se.alipsa.groovy:groovier-junit` as
    a test dependency if not already resolved transitively.
    - Commands run: _(record here)_

3.2 [x] `BomResolutionIT.groovy` — parse `bom.xml` and, for one representative class per module,
    resolve `Class.protectionDomain.codeSource.location` and assert the jar filename carries the
    version `bom.xml` declares. This is the test that catches version skew: matrix-ggplot 0.5.0's
    own pom declares `matrix-charts:0.5.0`, and the BOM must win with 0.5.1. Also assert no
    `-SNAPSHOT` jar appears for any module *not* under release. First IT to exist, so this is also
    where `target/jacoco-it.exec` must start appearing (see 1.8) and where `failIfNoTests` (1.2)
    stops needing an override.

    **Locate `bom.xml` via the `bom.file` system property set in 1.7**, not via a relative path.
    A relative `new File('bom.xml')` resolves against the forked JVM's working directory, which is
    `${basedir}` only until someone sets `<workingDirectory>` on failsafe — the same latent breakage
    1.8 calls out for the JaCoCo `destFile`, and here it would turn into a confusing
    file-not-found at release time. Fail with a clear message if the property is unset.

    **Assert the Groovy runtime version too**: `GroovySystem.version` must equal the `groovy-all`
    pin from 1.10 (5.0.8 today). 1.10's `dependency:tree` inspection is a one-time manual check of
    an invariant that 5.3 has to document as unenforced; one assertion here enforces it on every
    run, and it catches the failure 1.10 actually worries about — a transitive Groovy winning the
    resolution at equal depth — instead of only the pin being wrong.
    - Commands run: _(record here)_

3.3 [x] `CrossModuleWorkflowIT.groovy` — the multi-module chapters that no single module IT owns:
    `docs/tutorial/15-analysis-workflow.md`, `18-advanced-operations.md`, `10-matrix-bom.md`, and
    `docs/cookbook/cookbook.md`. Full pipelines: import → core transforms → stats → chart → export.
    - Commands run: _(record here)_

3.4 [x] `JavaConsumerIT.java` in `matrix-bom/src/test/java/test/alipsa/matrix/api/` — compile-and-run
    proof of the Java map-API shapes that 3.9.0 exists to restore (`Columns`,
    `CollectionUtils.m(...)`, `MatrixBuilder.columns`/`data`, `Matrix.and`,
    `Matrix.builder(Map, List<Class>, String)`, `Charts.plot`/`chart`), exercised from a real
    consumer through the BOM rather than from inside matrix-core's own build.
    **It must not reference `ApiItSupport` or any other Groovy test class** — not because it
    *cannot*, but because a Java-consumer proof that leans on Groovy test scaffolding is no longer
    proving what it claims. The IT should touch only the matrix API; that is the whole point of it.

    Do **not** justify this with a claim that javac cannot see the Groovy test classes — that is
    false, and the evidence is in this repo's own build output. gmavenplus's `generateTestStubs`
    runs at `generate-test-sources`, writes Java stubs for every Groovy test class, and adds their
    directory as a test compile source root, so `maven-compiler-plugin:testCompile` compiles them
    alongside `src/test/java` and the real Groovy classes take over at runtime:

    ```
    matrix-bom/target/generated-sources/groovy-stubs/test/test/alipsa/matrix/MatrixModulesTest.java
    ```

    (The plugin-ordering fact is still true — the default-lifecycle `testCompile` binding does run
    before gmavenplus `compileTests` — it just does not have the consequence it looks like it has,
    because the stubs are already on disk by then. Same trap as 1.1.)

    Per the migration note in `matrix-core/release.md`, declare the maps in the raw
    `Map<String, List>` / `LinkedHashMap<String, List>` form and expect `rawtypes` warnings; do not
    compile this source with `-Werror`.
    - Commands run: _(record here)_

## 4. Per-module API ITs

One `<Module>ApiIT.groovy` per module in `test.alipsa.matrix.api`, each carrying `@Tag('<module>')`.
Each test method gets GroovyDoc naming the readme section or doc file it covers.

**Release gate.** 4.1–4.6 (core, charts, ggplot, pict, xchart, stats) gate the matrix-core 3.9.0 /
matrix-charts 0.5.1 release, together with sections 1–3, section 6, and the section-5 tasks section
6 depends on (5.1a for these six modules' `api-coverage.md` sections, 5.2, 5.3). Those are the modules that are under
release (4.1, 4.2), consume the changed jars directly (4.3, 4.4), carry a resolution change this
release introduces (4.5), or are the heaviest consumer of matrix-core's surface (4.6) — which is
where a `NoSuchMethodError` from the signature changes would land. 4.7–4.20 complete the suite and
land after the release —
gating a two-module release on hand-written full-API ITs for all 20 modules (matrix-stats' README
alone is 447 lines, matrix-charts has ~244 source types) buys little against the risk this release
actually carries, which 3.2 and 6.6 address directly. Ship all 20 before the *next* BOM release.

Order within the gate is as numbered — release-critical first, so a partial suite still de-risks
the release.

Sources per module: `matrix-<name>/readme.md` or `README.md`, plus `docs/cookbook/matrix-<name>.md`
and the matching `docs/tutorial/*.md` chapter where they exist.

**Definition of done for each 4.x task** — the checklist comes first, not last:

1. Extract that module's documented API entries from its doc sources into its section of
   `matrix-bom/api-coverage.md` (5.1a), with an unchecked `[ ]` per entry. Do this *before* writing
   tests, so the surface is known before it is sampled.
2. Write the ITs, checking entries off as they are covered.
3. The task is `[x]` only when its `api-coverage.md` section has no unchecked entry left — or the
   remaining ones carry an explicit one-line reason (needs credentials, needs a JavaFX toolkit,
   documented behaviour is broken and tracked as issue N).

Written the other way round — tests first, checklist assembled afterwards — the checklist can only
describe whatever happened to get written, which is precisely the "one call per module proves one
code path" problem this suite exists to replace.

Verify each with the **full isolated command** — a bare `mvn -Papi-it -Dit.groups=<tag> verify`
resolves against the contaminated `~/.m2` and inherited settings, so it either tests the wrong jars
or fails to find the SNAPSHOT modules at all, and in both cases the result is worthless:

```bash
cd matrix-bom
REPO="${BOM_VERIFY_REPO:-$(git rev-parse --show-toplevel)/.bom-verify-repo}"
mvn -s verify-settings.xml -gs verify-settings.xml -Dmaven.repo.local="$REPO" \
    -Papi-it -DskipUnitTests=true -Dit.groups=<tag> verify
```

**`-Dit.groups` does not lift `it.excludedGroups`, and exclusion beats inclusion.** The default
`it.excludedGroups` from 1.5 is `external,emulator,jfx`, and JUnit Platform applies exclusion last —
so selecting the emulator test directly requires excluding `external,jfx` while Docker is running.
With `failIfNoTests=true` (1.2) a missing Docker/emulator prerequisite is a build failure, not a
pass. The verifier handles this automatically by checking Docker before selecting its exclusions.
The standalone command for 4.20 is:

```bash
mvn -s verify-settings.xml -gs verify-settings.xml -Dmaven.repo.local="$REPO" \
    -Papi-it -DskipUnitTests=true -Dit.groups=bigquery -Dit.excludedGroups=external,jfx verify
```

Modules with a mix of tagged and untagged methods (4.19 `GsheetsApiIT`) need no override — the
untagged methods still run, and the `external` ones are meant to be excluded here.

This assumes `$REPO` is already populated — run `./verifyBomApi.sh` once first, since only that
publishes the modules under release into it (see "Every `-SNAPSHOT` in `bom.xml` must be published
into `$REPO`"). Per-module runs are an iteration convenience on top of a completed full run, never
a substitute for one.

Record the command on the task's "Commands run" line. (Do not substitute `-DskipTests`: it skips
both test phases after 1.4; use `-DskipUnitTests=true` for an IT-only run.)

4.1 [x] `CoreApiIT` — `matrix-core/readme.md` (687 lines), `docs/cookbook/matrix-core.md`,
    `docs/tutorial/2-matrix-core.md`. Matrix/MatrixBuilder/Column/Row/Grid/Stat/Joiner+JoinType/
    Summary/Structure/Converter/ValueConverter/ListConverter/GroupedMatrix/RollingMatrix/
    MatrixAssertions, and the 3.8.0 additions (`top`, `bottom`, `info`, `sample`, `sampleFraction`,
    `merge`, `crossJoin`, `rename`, Column arithmetic returning `Column`).
    - Commands run: _(record here)_

4.2 [x] `ChartsApiIT` — `matrix-charts/README.md`, `matrix-charts/docs/charm.md`,
    `docs/cookbook/matrix-charts.md`, `docs/tutorial/13-matrix-charts.md`. Charm DSL (`Charts.plot`,
    `Charts.chart`), spec → build → render lifecycle, geoms, facets, scales, `PlotGrid`, `col` proxy,
    `stylesheet`, and `chartexport` (`ChartToSvg`, `ChartToPng`, `ChartToPdf`, `ChartToJpeg`,
    `ChartToImage`). `ChartToSvg` leads the list deliberately — SVG is charts' native output and
    every other exporter renders from it, so it is the one export path an omission would hurt most.
    Cover the 0.5.0 behaviour changes documented as breaking: `ExportFormat.fromExtension()`
    throwing, eager `PlotGrid` validation, facet + layer-data rejection.
    **Tagging:** only `ChartToJfx` needs a JavaFX toolkit and gets `@Tag('jfx')` (excluded by
    default). `ChartToSwing` is a Swing exporter, not JavaFX — give it `@Tag('swing')`, leave it in
    the default suite, and let it run under the headless AWT set in 1.7. There is no
    `ChartToSwingTest`; in-module `ChartToSwing` coverage lives in
    `matrix-charts/src/test/groovy/export/CharmExportTest.groovy` and
    `export/WriteToAndPlotGridExportTest.groovy`, both class-level `@Slow` (which is `@Tag('slow')`
    via `matrix-charts/src/test/groovy/testutil/Slow.groovy`). So if it proves slow here, add
    `@Tag('slow')` rather than dropping the coverage.
    - Commands run: _(record here)_

4.3 [x] `GgplotApiIT` — `matrix-ggplot/README.md`, `matrix-ggplot/docs/`,
    `docs/cookbook/matrix-ggplot.md`, `docs/tutorial/13b-matrix-ggplot.md`. This is the released
    0.5.0 jar running against charts 0.5.1, so cover the gg surface broadly: `ggplot`/`aes`, geoms,
    scales, facets, legends, `labs`, theme. Do not duplicate `DocExamplesTest` verbatim — it runs
    in-module against sources; here the point is the released binary against the new charts.
    - Commands run: _(record here)_

4.4 [x] `PictApiIT` — `matrix-pict/README.md`, `matrix-pict/docs/`. Chart-type-first builders
    (`ScatterChart`, `LineChart`, `BarChart`, …), `Plot.svg`/`png`, `Style.css`. Same rationale as
    4.3: released 0.5.0 jar, new charts.
    - Commands run: _(record here)_

4.5 [x] `XChartApiIT` — `matrix-xchart/readme.md`, `docs/tutorial/8-matrix-xchart.md`. All chart
    builders and export formats. Note matrix-charts 0.5.0 dropped its `org.knowm.xchart` dependency,
    so matrix-xchart is now the only source of that transitively — assert it resolves.
    - Commands run: _(record here)_

4.6 [x] `StatsApiIT` — `matrix-stats/README.md` (447 lines), `docs/cookbook/matrix-stats.md`,
    `docs/tutorial/3-matrix-stats.md`. Largest surface after charts: `Sampler`, `Correlation`,
    `Normalize`, `Anova`, distributions, normality tests, regression (`LinearRegression`,
    `MultipleLinearRegression`, `LogisticRegression`, `PolynomialRegression`, `QuantileRegression`),
    the formula DSL, `GoalSeek`, `Linalg`, `KMeans`, time-series tests.
    - Commands run: _(record here)_

4.7 [x] `DatasetsApiIT` — `matrix-datasets/README.md`, `docs/tutorial/4-matrix-datasets.md`. Every
    bundled dataset loads with the expected shape and column types.
    - Commands run: _(record here)_

4.8 [x] `SqlApiIT` — `matrix-sql/readme.md`, `docs/tutorial/9-matrix-sql.md`. `MatrixSql` create/
    read/update/drop, `tableName`, `tableExists`, `dbConnect`/`connect`, type round-tripping against
    the in-memory H2 already used by `MatrixModulesTest.testSql`.
    - Commands run: _(record here)_

4.9 [x] `SpreadsheetApiIT` — `matrix-spreadsheet/README.md`,
    `docs/cookbook/matrix-spreadsheet.md`, `docs/tutorial/5-matrix-spreadsheet.md`. xlsx and ods,
    `SpreadsheetWriter`/`SpreadsheetImporter`/`SpreadsheetExporter`, sheet and range selection,
    multi-sheet.
    - Commands run: _(record here)_

4.10 [x] `CsvApiIT` — `matrix-csv/README.md`, `docs/cookbook/matrix-csv.md`,
     `docs/tutorial/6-matrix-csv.md`. `CsvImporter`/`CsvExporter`, the `ReadBuilder` fluent API,
     `matrixName()`, `charset()`, header-only files, CSVFormat variants.
     - Commands run: _(record here)_

4.11 [x] `JsonApiIT` — `matrix-json/README.md`, `docs/cookbook/matrix-json.md`,
     `docs/tutorial/7-matrix-json.md`. `JsonReader`/`JsonWriter`, indent control, nested
     List/Map values, zero-column round trip, Grid-to-Matrix.
     - Commands run: _(record here)_

4.12 [x] `ParquetApiIT` — `matrix-parquet/readme.md` (316 lines),
     `docs/cookbook/matrix-parquet.md`, `docs/tutorial/11-matrix-parquet.md`. Behaviours:
     `MatrixParquetWriter.write` / `MatrixParquetReader.read` round trip preserving values, column
     names and `types()`; File and Path/stream overloads; explicit matrix-name argument; null values
     in numeric and string columns; compression-codec options documented in the readme; reading a
     file written outside Matrix if the readme shows one; error behaviour on a missing or malformed
     file.
     - Commands run: _(record here)_

4.13 [x] `AvroApiIT` — `matrix-avro/README.md` (343 lines), `docs/cookbook/matrix-avro.md`,
     `docs/tutorial/11b-matrix-avro.md`. Behaviours: `MatrixAvroWriter.write` /
     `MatrixAvroReader.read` round trip with type fidelity; the schema-inference and
     explicit-schema paths the README documents; the boolean overload currently used in
     `MatrixModulesTest.testAvro`; nulls/optional (union) fields; date and temporal logical types;
     error behaviour on schema mismatch.
     - Commands run: _(record here)_

4.14 [x] `ArffApiIT` — `matrix-arff/README.md` (339 lines), `docs/tutorial/16-matrix-arff.md`.
     Behaviours: `MatrixArffWriter.write` / `MatrixArffReader.read` round trip; ARFF attribute types
     (numeric, nominal, string, date) mapping to and from Matrix column types; relation-name
     handling; missing values (`?`); quoted values containing separators; reading a hand-written
     ARFF fixture from the README; error behaviour on a malformed header.
     - Commands run: _(record here)_

4.15 [x] `TablesawApiIT` — `matrix-tablesaw/readme.md`, `docs/tutorial/14-matrix-tablesaw.md`.
     `TableUtil` conversions both directions with type fidelity, **plus whatever the readme documents
     of the Gtable support in `se.alipsa.matrix.tablesaw.gtable` and the `tech.tablesaw.io.{ods,xlsx,
     xml}` readers**. This is the one module whose API is split across two package roots (see 1.8);
     if the ITs only exercise `TableUtil`, the `tech.tablesaw.*` half now instrumented by 1.8 reports
     a permanent zero and 6.4 will flag it.
     - Commands run: _(record here)_

4.16 [x] `SmileApiIT` — `matrix-smile/README.md` (323 lines), `docs/cookbook/matrix-smile.md`,
     `docs/tutorial/17-matrix-smile.md`. `SmileUtil` conversions, `SmileStats`.
     - Commands run: _(record here)_

4.17 [x] `GroovyExtApiIT` — `matrix-groovy-ext/README.md`. The full `NumberExtension` catalog from
     `docs/agents/groovy-style-guide.md`. Critically, this verifies the Groovy extension module
     descriptor still registers when the jar arrives as a transitive `api` dependency of matrix-core
     rather than as a project dependency.
     - Commands run: _(record here)_

4.18 [x] `LoggingApiIT` — `matrix-logging/README.md` and `docs/logging.md`. `MatrixLogging` setup and
     that `se.alipsa.matrix.core.util.Logger` output routes through it once the optional backend is
     on the classpath.
     - Commands run: _(record here)_

4.19 [x] `GsheetsApiIT` — `matrix-gsheets/readme.md`, `matrix-gsheets/docs/`. Offline-safe surface
     (`GsUtil.columnCountForRange`, `asColumnNumber`, A1 quoting, range parsing) untagged;
     anything needing credentials gets `@Tag('external')`.
     - Commands run: _(record here)_

4.20 [x] `BigQueryApiIT` — `matrix-bigquery/readme.md`, `docs/cookbook/matrix-bigquery.md`,
     `docs/tutorial/12-matrix-bigquery.md`. `@Tag('emulator')` throughout. Move the testcontainers
     setup over from the existing `matrix-bom/src/test/groovy/test/alipsa/matrix/BiqQueryTest.groovy`,
     using the `ghcr.io/goccy/bigquery-emulator:0.6.6` image and a supported Long/String
     dataset/save/query round trip. Docker is required for this local emulator check; it does not
     need Google Cloud credentials or incur service charges. **Delete `BiqQueryTest.groovy` in the
     same commit.** It is a `*Test`,
     so surefire keeps running it; leaving it in place means two copies of the emulator scaffolding
     drifting apart. This is the one deliberate exception to "the existing `MatrixModulesTest`
     smoke test stays as-is" — `MatrixModulesTest` stays, `BiqQueryTest` does not.

     **Its standalone verification command needs one override**, because the default exclusions
     include `emulator`:

     ```bash
     mvn -s verify-settings.xml -gs verify-settings.xml -Dmaven.repo.local="$REPO" \
         -Papi-it -DskipUnitTests=true \
         -Dit.groups=bigquery -Dit.excludedGroups=external,jfx verify
     ```

     `-Dit.excludedGroups=external,jfx` because the default `external,emulator,jfx` would otherwise
     exclude the very class the `bigquery` tag selects (see the section preamble). Docker must be
     running before this command; Testcontainers pulls and starts the emulator image automatically.
     - Commands run: `mvn -s verify-settings.xml -gs verify-settings.xml
       -Dmaven.repo.local="$REPO" -Papi-it -DskipUnitTests=true
       -Dit.groups=bigquery -Dit.excludedGroups=external,jfx verify` — 1 test passed with Docker.

## 5. Coverage tracking

5.1a [x] Create `matrix-bom/api-coverage.md` with one section per gated module (4.1–4.6), listing
     the documented API entries and the IT method covering each, with `[ ]`/`[x]` checkboxes. Each
     gated section is created as step 1 of its 4.x task, unchecked, straight from the doc sources,
     before any test for that module exists. Complete this task when the six gated sections have no
     unchecked entry without an explicit reason and 6.4 has recorded its findings.
     - Commands run: _(record here — the module IT runs that back each checked entry)_

5.1b [x] Extend `matrix-bom/api-coverage.md` with sections for 4.7–4.20 and perform the final
     pass over all 20 sections. Confirm every unchecked entry carries a reason and that corrections
     made under the "A documented example that does not run" rule are recorded with the module and
     corrected text. Complete this task with 4.7–4.20, after the release gate.
     - Commands run: _(record here — the module IT runs that back each checked entry)_

5.2 [x] After the full suite runs, record the JaCoCo per-module instruction/branch coverage in
    `api-coverage.md` as the baseline. Read it from the **per-module** XMLs 2.9 writes to
    `target/jacoco-per-module/<module>.xml`, not from the combined `target/jacoco-bom-api.xml` —
    the combined report is keyed by package, and package names do not identify modules (see the
    table in 2.9). Reported, not gated — the docs checklist is the contract, JaCoCo is the reality
    check on it.
    - Commands run: _(record here)_

5.3 [x] Update `matrix-bom/readme.md` with a "Verifying a release" section: `./verifyBomApi.sh`, the
    `-Papi-it` / `-DskipUnitTests=true` / `-Dit.groups=` / `-Dit.excludedGroups=` /
    `-Dit.failIfNoTests=` / `RUN_EXTERNAL_TESTS=true` switches, automatic Docker/emulator detection,
    `BOM_VERIFY_REPO`,
    `BOM_VERIFY_FULL_WIPE`, and the optional `BOM_VERIFY_JAPICMP_OLD` override (the normal
    baseline comes from `matrixCoreBaselineVersion` in `bom.xml`) (with the note that the default scoped wipe clears only
    `se/alipsa/matrix` and that this is what keeps repeat runs from re-downloading the whole
    third-party graph), the fact that a `bom.xml` with no `-SNAPSHOT` properties is a normal run in
    which nothing is published locally, that `-Dit.groups=<tag>` does not lift `it.excludedGroups`,
    and where the coverage output
    lands —
    `matrix-bom/target/site/jacoco-bom-api/index.html` for browsing by package, and
    `matrix-bom/target/jacoco-per-module/<module>.xml` for the per-module totals, with a sentence on
    why there are two (2.9).

    Two more things belong in that section:

    - **`-DskipTests` is a combined skip in this module** (1.4): it skips both surefire and
      failsafe. Spell out the working flags — `-DskipUnitTests=true`, `-DskipITs=true`,
      `-Dmaven.test.skip=true` — and say plainly that `-DskipTests` should not be used here.
    - **japicmp**: it runs as part of `verifyBomApi.sh`, reports rather than gates, is skipped with
      an explicit message when matrix-core is not under release, and its version pair is derived,
      not hand-edited (2.12/6.6).

    Also record the unenforced invariants a future edit can silently break: `api-it-external` must
    stay declared after `api-it` (1.6), and the `groovy-all` pin must move with `v_groovy` (1.10) —
    noting that the second one is now asserted at runtime by `BomResolutionIT` (3.2), so a stale pin
    fails a test rather than passing silently.
    - Commands run: _(record here — doc task, note the commands you validated the instructions with)_

## 6. Full verification

6.1 [x] `BOM_VERIFY_JAPICMP_OLD=3.8.0 BOM_VERIFY_FULL_WIPE=true matrix-bom/verifyBomApi.sh` — completely clean isolated repo, only
    matrix-core and matrix-charts published locally, all 18 other modules from Central, full IT suite
    green with `failIfNoTests=true` in force. This is the one run that takes the full wipe: routine
    and per-module runs use the default scoped wipe (see "Repository location and deletion safety"),
    which reuses the third-party artifacts already downloaded but still rebuilds
    `se/alipsa/matrix` from scratch. Record which mode the recorded run used — the run header
    prints it — because only the full-wipe result can be described as resolving everything from
    Central.
    - Commands run: _(record here)_

6.2 [x] `BOM_VERIFY_JAPICMP_OLD=3.8.0 RUN_EXTERNAL_TESTS=true matrix-bom/verifyBomApi.sh` — runs the
    `external`-tagged ITs via the `api-it-external` profile from 1.6 and also runs the `emulator`-
    tagged IT when Docker is available.

    **First confirm the profile actually took effect.** `api-it-external` overriding
    `it.excludedGroups` depends on it being declared *after* `api-it` (see 1.6); declared before, the
    override is a silent no-op and the run excludes exactly what it was supposed to include, while
    still exiting 0. Check the failsafe summary shows both `GsheetsApiIT` and `BigQueryApiIT` tests
    **run**, not skipped and not absent. A green build proves nothing here.

    **What this does and does not verify.** GSheets and BigQuery are genuinely exercised when their
    prerequisites are available:
    `BigQueryApiIT` starts the emulator, creates a dataset, saves a Matrix, queries it back, and
    verifies the round-trip contents. Docker is required, but Google Cloud credentials are not.
    This validates the isolated emulator path; live BigQuery service coverage remains out of scope
    here and continues to use `matrix-bigquery/release.sh`.
    - Commands run: `RUN_EXTERNAL_TESTS=true BOM_VERIFY_JAPICMP_OLD=3.8.0
      ./matrix-bom/verifyBomApi.sh` — 29 ITs passed, 0 skipped; Docker was available and the
      BigQuery emulator container ran successfully.

6.3 [x] Confirm `BomResolutionIT` reports the intended version set, in particular matrix-charts
    0.5.1 winning over matrix-ggplot 0.5.0's transitive 0.5.0 pin.
    - Commands run: _(record here)_

6.4 [x] Confirm all 20 modules are present in the coverage run (including test-scoped
    matrix-logging) and log gaps in `api-coverage.md`.

    **Know the expected baseline before reading the numbers, or this task reports 14 false
    findings.** At the release gate only 4.1–4.6 exist, so the honest expectation is:

    - **matrix-core, matrix-charts, matrix-ggplot, matrix-pict, matrix-xchart, matrix-stats** —
      substantive coverage. A zero or near-zero here is a real finding: the jar resolved but the IT
      never reached it.
    - **matrix-datasets, matrix-csv, matrix-json, matrix-sql, matrix-spreadsheet, matrix-groovy-ext**
      and anything else the cross-cutting ITs (3.3, 3.4) traverse — incidental, non-zero coverage.
      Record the number; it is a baseline, not a target.
    - **the remaining modules** — zero, expected, *not* findings until their 4.x task lands. Record
      them as `no IT yet (4.x)` in `api-coverage.md` rather than as gaps.

    After 4.7–4.20 land, the rule tightens to its full form: any module reporting zero instructions
    means its jar was resolved but no IT touched it, and that is a real finding. Check presence
    against
    `target/jacoco-per-module/` — 20 XMLs, one per module — **not** against
    `target/site/jacoco-bom-api/index.html`, whose top level is packages: `se.alipsa.matrix.charm`
    and `se.alipsa.matrix.gg` do not read as "matrix-charts" and "matrix-ggplot", and matrix-charts
    contributes two package rows while matrix-tablesaw contributes several. Browse the combined HTML
    for *where* coverage lands; use the per-module XMLs for *whether* a module was covered at all —
    an uncovered module is one the combined HTML hides by simply not listing the package.
    - Commands run: _(record here)_

6.5 [x] `./gradlew test` at the repo root — the ITs live in matrix-bom (Maven) and must not disturb
    the Gradle build.
    - Commands run: _(record here)_

6.6 [x] Run japicmp on matrix-core 3.8.0 → 3.9.0-SNAPSHOT with
    `BOM_VERIFY_JAPICMP_OLD=3.8.0` (via the runner step built in 2.12) and record the result.

    Compatibility findings are warnings, not release failures. The runner must print the finding,
    preserve the report, and continue with exit code 0; only japicmp infrastructure failures
    (nonzero execution or missing report) fail this task and the overall verification.

    **This is not a formality.** 3.9.0 changes generic signatures — `Columns`, `CollectionUtils.m(...)`,
    `MatrixBuilder.columns`/`data`, `Matrix.and`, `Matrix.builder(Map, List<Class>, String)`
    reverting `List<?>` to raw `List` per `matrix-core/release.md` — whose erasures are unchanged,
    so it should come back clean. But this project has shipped binary-incompatible core changes in
    the immediately preceding release: running the comparison below on **3.7.1 → 3.8.0** reports
    `Column` arithmetic (`plus`, `minus`, `multiply`, `div`, `power`) as `MODIFIED METHOD ... Column
    (<- java.util.List)`, i.e. the documented "Column arithmetic returning Column" feature is a
    binary break for anything compiled against 3.7.1. With 17 released modules built against 3.8.0
    under `compileOnly` + project-wide `@CompileStatic`, and ITs that only prove the paths they
    reach, this check earns its place.

    **Run it from inside `verifyBomApi.sh` (2.12), after 2.8 and before 2.9** — not merely "after
    2.6". Both sides must be resolvable: 3.9.0-SNAPSHOT from the local publish, 3.8.0 from Central.
    Fetching the old one is exactly what makes the position matter — it writes
    `$REPO/se/alipsa/matrix/matrix-core/3.8.0/`, a second version directory under a module that 2.7
    requires to hold **one** version and nothing else. Run japicmp before 2.7 and the isolation
    assertion fails on an artifact the script itself just downloaded.

    The invocation is fussier than it looks; the following was **verified end to end** (japicmp
    0.26.1, Maven 3.9.14, JDK 21) by running 3.7.1 → 3.8.0 and getting real reports out. Three
    things that do not work and cost time to rediscover:

    - **`-Dold/newVersion=...` on the CLI does nothing.** They are complex XML objects, not scalar
      properties. The run silently falls back to comparing the project's *own* artifact and fails
      with "Could not find artifact ...japicmp-core:jar:1".
    - **Plugin-level `<configuration>` is not applied to a `japicmp:cmp` CLI invocation**
      (`default-cli`). It must be bound to an `<execution>` with a phase, and invoked via that
      phase.
    - **japicmp skips `pom` packaging by default** — "Skipping module because packaging is 'pom'",
      build green, no report. Needs `<skipPomModules>false</skipPomModules>`.

    So use a dedicated throwaway pom, committed as `matrix-bom/japicmp/pom.xml.template` with the
    two versions left as placeholders and `japicmp/pom.xml` generated from it on each run (2.12).
    Shown here with the placeholders filled in for this release:

    ```xml
    <project xmlns="http://maven.apache.org/POM/4.0.0">
      <modelVersion>4.0.0</modelVersion>
      <groupId>se.alipsa.matrix.verify</groupId>
      <artifactId>japicmp-core</artifactId>
      <version>1</version>
      <packaging>pom</packaging>
      <build><plugins><plugin>
        <groupId>com.github.siom79.japicmp</groupId>
        <artifactId>japicmp-maven-plugin</artifactId>
        <version>0.26.1</version>
        <executions>
          <execution><id>cmp</id><phase>verify</phase><goals><goal>cmp</goal></goals></execution>
        </executions>
        <configuration>
          <oldVersion><dependency>
            <groupId>se.alipsa.matrix</groupId><artifactId>matrix-core</artifactId>
            <version>3.8.0</version>              <!-- template: @OLD_VERSION@ -->
          </dependency></oldVersion>
          <newVersion><dependency>
            <groupId>se.alipsa.matrix</groupId><artifactId>matrix-core</artifactId>
            <version>3.9.0-SNAPSHOT</version>     <!-- template: @NEW_VERSION@ -->
          </dependency></newVersion>
          <parameter>
            <onlyBinaryIncompatible>false</onlyBinaryIncompatible>
            <breakBuildOnBinaryIncompatibleModifications>false</breakBuildOnBinaryIncompatibleModifications>
            <breakBuildOnSourceIncompatibleModifications>false</breakBuildOnSourceIncompatibleModifications>
            <ignoreMissingClasses>true</ignoreMissingClasses>
            <skipPomModules>false</skipPomModules>
          </parameter>
        </configuration>
      </plugin></plugins></build>
    </project>
    ```

    **Do not commit the filled-in versions.** A hard-coded `3.9.0-SNAPSHOT` stops resolving the
    moment matrix-core 3.9.0 is released, `mvn` then exits nonzero, and the hard-failure branch
    below aborts the entire verification — every run, until someone notices the pom needs editing.
    2.12 fills `@NEW_VERSION@` from the `matrixCoreVersion` that 2.5 detected, fills `@OLD_VERSION@`
    from `matrixCoreBaselineVersion` (or the explicit `BOM_VERIFY_JAPICMP_OLD` override), and skips
    the comparison outright — with its own distinct message — when matrix-core is not under release.
    "Nothing to compare" and "the check is broken" must never print the same way.

    **Reporting without gating, under `set -euo pipefail`.** Both binary and source compatibility
    findings are reported rather than gated, while a nonzero exit still means japicmp genuinely
    failed to run and must not be swallowed. Distinguish the two by exit status and by whether the
    report was produced:

    ```bash
    if [[ -z "${japicmp_new:-}" ]]; then          # matrixCoreVersion was not detected as SNAPSHOT
      echo "japicmp: matrix-core is not under release — comparison skipped"
    else
      sed -e "s/@OLD_VERSION@/$japicmp_old/" -e "s/@NEW_VERSION@/$japicmp_new/" \
          japicmp/pom.xml.template > japicmp/pom.xml
      rc=0
      mvn "${MVN_ISOLATED[@]}" -f japicmp/pom.xml verify || rc=$?
      report=japicmp/target/japicmp/cmp.xml
      if (( rc != 0 )) || [[ ! -f "$report" ]]; then
        echo "japicmp failed to run (exit $rc) — infrastructure problem, not a compatibility result" >&2
        exit 1                     # a broken check is a hard failure
      fi
      if grep -Eq 'binaryCompatible="false"|sourceCompatible="false"' "$report"; then
        echo "japicmp: COMPATIBILITY CHANGES FOUND in matrix-core — review before releasing:"
        changed_entries=$(
          rg '^[[:space:]]*===\*' japicmp/target/japicmp/cmp.diff |
            rg -v '===\* UNCHANGED CLASS:' |
            sed -E \
              -e 's/^[[:space:]]*===\* UNCHANGED (METHOD|CONSTRUCTOR|FIELD):/  AFFECTED \1:/' \
              -e 's/^[[:space:]]*===\* /  /' || true
        )
        [[ -n "$changed_entries" ]] && {
          echo "affected API signatures (class summaries omitted):"
          printf '%s\n' "$changed_entries"
        }
        echo "full reports: japicmp/target/japicmp/cmp.xml and japicmp/target/japicmp/cmp.diff"
      else
        echo "japicmp: matrix-core $japicmp_old -> $japicmp_new is binary and source compatible"
      fi                           # findings are reported; the release decision stays human
    fi
    ```

    The report filenames come from the execution id, not from a fixed name: `<execution><id>cmp</id>`
    is why they are `cmp.xml`/`cmp.diff` under `japicmp/target/japicmp/`. Rename the execution and
    both paths above move with it.

    Note `<ignoreMissingClasses>true</ignoreMissingClasses>` is required (matrix-core's `compileOnly`
    graph means superclasses are absent from the comparison classpath) and weakens the result — the
    report says so itself in a WARNING line. Read it as a strong signal, not a proof.
    - Commands run: _(record here)_

## Notes

- **Branch:** per `AGENTS.md`, do this on a feature branch, not `main`.
- **Release gate vs. full suite:** the matrix-core 3.9.0 / matrix-charts 0.5.1 release is gated on
  sections 1, 2, 3, 6, tasks 4.1–4.6, and the parts of section 5 that section 6 consumes —
  **5.1a for the 4.1–4.6 sections of `api-coverage.md`, 5.2, and 5.3**. Section 6 cannot honestly be
  completed without them: 6.4 logs its findings *into* `api-coverage.md`, and 5.2 is what turns the
  coverage run into a recorded baseline. What is *not* gated is 5.1b's final pass over the whole
  file, which needs the 4.7–4.20 sections and therefore lands with them. 4.7–4.20 are required
  before the *next* BOM release, not this one. Japicmp compatibility findings are warnings within
  section 6 and do not fail the release; only a broken japicmp run fails verification. If you would rather hold the release for all 20,
  that is a scope decision to make explicitly here — the plan as written does not.
- **After this release, the runner's normal mode changes.** With `bom.xml` free of `-SNAPSHOT`
  properties, 2.5 detects nothing, 2.6 publishes nothing and every artifact comes from Central. That
  is the strongest configuration this suite can run in, and it is also the one that has never been
  exercised — rehearse it via the override described in the section-2 verification block rather than
  meeting it for the first time during the next release.
- **Versions:** matrix-bom stays at `2.5.2-SNAPSHOT` in both `pom.xml` and `bom.xml`; no version
  bump is part of this work.
- **Scope deliberately excluded:** ASM linkage scanning and replaying released modules' own test
  suites against the new jars. Judged a nice-to-have versus proving the BOM's dependency set works.
  japicmp was originally in this list and has been **promoted into 6.6**: `compileOnly` +
  project-wide `@CompileStatic` makes `NoSuchMethodError` from a released module against a newer
  core both real and statically detectable, and this particular release is *nothing but* a
  signature change to matrix-core, so the one failure mode the plan was excluding is the one it is
  most exposed to. It stays reported-not-gated.
- **Deferred SNAPSHOTs:** `bom.xml` keeps matrix-bigquery, matrix-datasets, matrix-gsheets,
  matrix-json and matrix-xchart pinned to released versions with the SNAPSHOT lines commented out.
  Those comments document intent for a human reader; they carry no meaning for the tooling. Detection
  (2.5) parses the active property elements directly from `bom.xml`, so only uncommented raw values
  decide what is treated as under release — commenting a line back in is what changes behaviour, and `BomResolutionIT`
  asserts no unexpected SNAPSHOT reaches the classpath.
