# Combined Matrix releases

`releaseAll.sh` publishes all eligible, unreleased, non-SNAPSHOT publications in one
Maven-layout ZIP and one automatic Central Portal deployment. The existing `release.sh`
and module release scripts remain available for individual releases. No version is
rewritten, no baseline is advanced, and no Git tag is created.

## First run

1. Install Java 21, Maven 3.9.9 or later, Bash 4 or later, Groovy CLI, Git and ripgrep.
   On macOS, run `brew install bash ripgrep` and put Homebrew's `bin` on PATH before
   `/bin`, so `#!/usr/bin/env bash` selects modern Bash. GNU realpath/find are unnecessary.
2. Check out the enhanced sibling `../nexus-release-plugin`. Its Gradle build also needs
   a discoverable JDK 17 toolchain. Until the enhancement is published separately, the
   release workflow resolves it through an explicit Gradle composite build. Ordinary
   Matrix builds continue using the existing published plugin.
3. Set `sonatypeUsername` and `sonatypePassword` in `~/.gradle/gradle.properties`, or set
   `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` in the process environment. These must be
   Central Portal user-token credentials. Do not put secrets into tracked files.
4. Capture the read-only Portal contract fixtures once:

   ```bash
   groovy scripts/release-all/capture-portal-fixture.groovy
   ```

   This checks published `se.alipsa.matrix:matrix-core:3.9.0` and a never-uploaded version,
   using the exact groupId as `namespace`. Only parameter values, status, boolean body,
   date and official API source are saved to `req/releaseAll-portal-fixture.json`.
   Commit the fixture and release versions on your release branch. If credentials,
   namespace semantics or the absence response cannot be confirmed, discovery fails
   closed when it needs Portal absence. A selection containing only SNAPSHOT versions
   does not need credentials or fixtures.
5. Configure Gradle artifact signing through the existing `signing.keyId`, signing
   secret-key/passphrase properties. Maven's `release` profile uses your existing
   global/user Maven settings and GPG configuration. GPG and an available secret key
   are checked before verification when Maven components are selected; dry-run reports
   missing setup. Configure the key selection/passphrase through your existing Maven settings.
6. Commit or stash all changes. Run:

   ```bash
   ./releaseAll.sh --dry-run
   ./releaseAll.sh --bundle-only
   ./releaseAll.sh
   ```

   The first command reports exclusions and test scope. The second verifies and creates
   the ZIP; the third additionally submits it. Each command freshly plans against current
   Central/Portal availability. Do not submit the ZIP separately while using this workflow.

## Commands and defaults

| Option or variable | Default | Behavior |
| --- | --- | --- |
| no option | full release | Plan, preflight, scoped verification, local staging, validate, upload once, poll publication and repository propagation. |
| `--dry-run` | off | Generate POMs and report selection; check required test prerequisites. No tests, signing, staging or upload. Authentication preflight may compile gsheets, core and required project dependencies. |
| `--bundle-only` | off | Verify and prepare the validated ZIP without upload. |
| `--resume <deployment-id>` | off | Poll the saved deployment without planning, rebuilding or uploading. Source changes are allowed for status-only recovery. |
| `RUN_EXTERNAL_TESTS` | `true` | Enable normal externally tagged tests. Set `false` explicitly to omit their credentials/network prerequisites and tests. |
| `RUN_SLOW_TESTS` | `true` | Preserve existing slow-test release behavior. Set `false` to override it. |
| `RELEASE_ALL_DEDICATED_EXTERNAL_TESTS` | `false` | Opt in to dedicated BigQuery release tests; requires external tests enabled. Flaky tests are excluded. |
| `RELEASE_ALL_PLUGIN_DIR` | sibling `../nexus-release-plugin` | Path to the enhanced plugin checkout for the composite build. |

Run from any working directory; the script anchors all paths at its own directory.
For example, an explicitly offline test release is:

```bash
RUN_EXTERNAL_TESTS=false RUN_SLOW_TESTS=false /path/to/matrix/releaseAll.sh --bundle-only
```

## Eligibility and verification

Every production compile-classpath project dependency is an eligibility edge, including
`compileOnly`, `implementation` and `api`, even if absent from the POM. Published Matrix
POM dependencies and BOM pins are checked too. An exact version must already be released
or be eligible in this bundle. Exclusions cascade to dependants.

For example, stats `2.5.4` compiled against core `3.10.0-SNAPSHOT` is skipped even if core
`3.9.0` exists in Central. Set the actual core project to the required released version,
or release core's exact non-SNAPSHOT version in the same bundle. Separate-release scripts
retain their existing behavior.

A selected module's BOM pin must match its publication version. An unselected module may
be pinned to any exact released version: arff `0.3.0` remains valid when the unselected
arff build has advanced to `0.3.1-SNAPSHOT`, with a warning about the difference.

Verification runs selected Gradle modules plus their transitive production dependants,
including SNAPSHOT dependants for regression testing, excluding examples. CodeNarc runs
before Spotless checks and scoped `build -Pheadless=true`; this includes CSV's non-UTF-8
encoding check. Unrelated module tests are not release gates. Verification builds disable the configuration cache for the existing test listeners; new planning/bundling tasks are tested with strict cache reuse. Explicit external/slow flags are passed as Gradle properties as well as environment variables. BOM/all releases also run
the existing API verifier, with existing emulator skips and japicmp compatibility warnings.

External prerequisites are based on actual enabled `@Tag('external')` test methods, checked against `scripts/release-all/external-requirements.json`. New unmapped tests block preflight; disabled, flaky and disabled slow tests add no prerequisites:

- GSheets needs noninteractive ADC with Sheets, Sheets-readonly and Drive-file grants,
  including cleanup. Broader Sheets and Drive grants satisfy their narrower scopes
  through the existing authenticator checks. Enable Sheets/Drive APIs and use, for example,
  `gcloud auth application-default login --scopes=https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/spreadsheets,https://www.googleapis.com/auth/drive.file`.
  Set the ADC quota project as needed. Preflight validates scopes and quota handling
  using the existing authenticator, then probes Sheets and Drive endpoints. It does not
  start a browser login or create spreadsheets.
- Datasets needs access to the Rdatasets overview and CSV endpoints on
  `raw.githubusercontent.com`. It needs no ADC.
- Dedicated BigQuery checks additionally require ADC, `GOOGLE_CLOUD_PROJECT` and a
  running Docker daemon. The release task excludes flaky tests; the existing
  `externalTest` task remains unchanged.
- Current BOM API tests have no external-tagged credentialed Sheets or remote dataset
  tests. A BOM/all-only release adds neither ADC nor dataset probes.

The unpublished `authPreflight` source set has a dedicated statically compiled entry
point, explicit Groovy BOM/runtime, groovy-json and matrix-core dependencies. These
helpers are also used by external tests and excluded from all published artifacts/POMs.
The entry point accepts `--check-classpath` for a noncredentialed runtime wiring check:
`./gradlew :matrix-gsheets:releaseAuthPreflight --args=--check-classpath`.
The release script always runs full authentication preflight with no entry-point arguments.
Offline scope-coverage tests intercept production operations, and an AST inventory checks
qualified and statically imported authentication calls.

## Outputs and recovery

Outputs are under ignored `build/releaseAll/`:

- `manifest.json`: selected/excluded coordinates, dependency origins, verification scope,
  source revision, effective versions and tracked-source hashes, without credentials.
- `report.txt`: readable coordinates, every dependency edge/origin, exclusions and test scope.
- `staging/`: signed release files only. Maven accesses this as a releases-only file remote.
- `maven-local/`: isolated Maven downloads/install outputs, separate from staging.
- `bundle.zip` and `bundle-receipt.json`: allow-listed archive and validation hashes.
- `deployment.json`: submission intent, bundle hash, manifest and immediately saved ID/state.

BOM packaging requires only its POM. Normal JAR modules require main, sources and Javadoc
JARs and POM. `matrix-all` deliberately preserves its existing POM/main JAR artifact set;
Central server validation is authoritative for this compatibility exception. Every artifact
requires its signature. MD5/SHA-1 checksums are generated and existing staged checksums are
verified. Third-party downloads, bookkeeping, module metadata and unrelated coordinates
never enter the ZIP. The local limit is 1 GB (1,000,000,000 bytes).

Preparation, bundling and new uploads reject changed HEAD, dirty sources, changed versions
or input hashes. Replan from a clean checkout after changing source/version inputs.
The validated ZIP/manifest hashes must still match immediately before upload. Availability
is rechecked against both Central and Portal; 400/401/403 and uncertain responses block,
while transient failures have bounded retries.

If interrupted after receiving a deployment ID, run:

```bash
./releaseAll.sh --resume <saved-deployment-id>
```

If the record says `SUBMITTING` with no ID, the outcome is unknown: inspect Portal and
reconcile the deployment manually. **Do not retry upload.** A failed deployment also
requires inspection/reconciliation before another submission. Preserve the record until
all components are available on Central; repository propagation is distinct from Portal
publication. Archive a completed `AVAILABLE` record outside the active `deployment.json`
path before starting the next release. Never erase an unresolved record to retry.

Verification settings contain only Central plus the file remote. Signing preparation
copies existing user Maven settings temporarily with owner-only permissions, preserves
global settings, adds the file remote and deletes the copy afterwards. If your configured
mirror intercepts file repositories, exclude `matrix-release-staging` from that mirror's
`mirrorOf` expression before preparation.

The plugin enhancement is released separately from Matrix. Its published version should
replace the development composite only after that separate release is available; no plugin
publication or Matrix publication is performed during implementation verification.

## Implementation checks

The following checks create disposable fixtures under ignored `build/releaseAll/` and
never upload. Run them from the Matrix root after the normal build checks:

```bash
groovy scripts/release-all/test-tools.groovy .
groovy scripts/release-all/test-workflow.groovy .
groovy scripts/release-all/test-auth-classpath.groovy .
groovy scripts/release-all/test-maven-preparation.groovy . ../nexus-release-plugin
RUN_EXTERNAL_TESTS=false RUN_SLOW_TESTS=false \
  BOM_VERIFY_REPO="$PWD/build/releaseAll/bom-verify" bash matrix-bom/verifyBomApi.sh
groovy scripts/release-all/test-manifest-verifier.groovy . build/releaseAll/bom-verify
```

Each script's first argument is the Matrix root (default `.`). Signed preparation's
second argument is the enhanced plugin checkout (default `../nexus-release-plugin`);
it requires GPG and uses a disposable keyring plus fixture signing settings. Manifest
verification's second argument is the isolated repository populated by the preceding
standalone verifier (default `build/releaseAll/bom-verify`). The source-set fixture runs
controlled authentication/HTTP checks and deliberately confirms a bad method signature
fails static compilation. CI runs portable helpers on macOS/Linux and both verifier modes
on Linux. Live Portal fixture capture remains a separate credentialed read-only prerequisite.
