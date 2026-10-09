# Release eligible Matrix modules in one Central bundle

## 1. Approach

1.1 [x] Add `releaseAll.sh` to discover release candidates, prepare their signed artifacts, assemble one ZIP, and upload it once. Include Gradle modules plus `matrix-bom` and `matrix-all`.

1.2 [x] Keep existing module release scripts, `release.sh`, and plugin `bundle`/`release` tasks available for separate releases.

1.3 [x] Put reusable discovery, dependency eligibility, bundling, validation, and deployment support in `../nexus-release-plugin`; keep Matrix-specific candidate registration and orchestration in Matrix's Gradle build and script. Reuse the existing BOM verifier and its helpers instead of copying their staging, property mapping, API testing, or japicmp logic.

1.4 [x] Use one Maven-layout archive for all selected components. Central supports multiple components in one archive, with a current upload limit of 1 GB. Combined publishing reduces upload operations; file count and storage usage remain separate metrics. [Bundle documentation](https://central.sonatype.org/publish/publish-portal-upload/), [publishing limits](https://central.sonatype.org/publish/maven-central-publishing-limits/)

## 2. Eligibility and manifest

2.1 [x] Introduce typed publication and dependency descriptors containing coordinates, packaging, source project or Maven POM, expected artifact files, dependency origin/configuration, and selection/exclusion reasons. Discover Gradle candidates through configured plugin publications; register the two Maven candidates explicitly. Exclude example projects.

2.2 [x] Add `releaseAllPlan` to produce a JSON manifest and readable report. Read Gradle coordinates from `MavenPublication`; parse Maven candidates statically from `pom.xml`, `bom.xml`, and their properties, including `${project.version}` and BOM-managed dependency versions. Do not run Maven flattening or dependency resolution during planning. Generate Gradle candidate POMs without building their JARs. Skip candidate versions ending in `-SNAPSHOT`.

2.3 [x] Check each exact version's POM at `https://repo.maven.apache.org/maven2`, and for coordinates absent there also check authenticated Portal publication status. A positive result from either source means already released; selection requires a repository 404 and an explicit Portal not-published result. Use bounded retries for transient failures; unresolved HTTP, authentication, network, or response errors abort discovery. Do not infer availability from latest-version metadata or local caches. Dry-run therefore needs Portal credentials when repository absence must be confirmed, but never signing credentials.

2.4 [x] Include Matrix project dependency edges from the production compile classpath, including inherited `compileOnly`, `implementation`, and `api` declarations, even when absent from the published POM. Inspect published dependencies, runtime dependencies, BOM imports, and dependency management as well. Use the actual target project's publication version for project edges, never a substituted previous release. Report every edge with its originating configuration or POM element. Exclude test-only build edges unless they appear in the published POM.

2.5 [x] Skip candidates with SNAPSHOT dependencies or missing Matrix coordinates outside the bundle. Each Matrix eligibility edge must reference an exact version already released or remaining in the bundle. Repeat exclusions to a fixed point so dependants of blocked modules are skipped too. Report direct and cascading reasons; unresolved POM properties or unsupported dependency expressions are planning errors, not assumed valid dependencies.

2.6 [x] Compare each BOM-managed Matrix version with the final selected module set. For a selected module, require the BOM pin to equal its publication version; a mismatch skips `matrix-bom` and any `matrix-all` depending on it. For an unselected module, accept any exact BOM-pinned version already released, even when its build script has advanced to a SNAPSHOT; report the difference as a warning only. A pin neither already released nor supplied by the selected bundle blocks the BOM through the dependency rules. Reevaluate this rule after cascading exclusions. Do not exclude an independent module solely because an unselected BOM is stale, rewrite versions, or replace pins automatically.

2.7 [x] Persist selected and excluded coordinates, dependency edges, expected published Maven dependencies, verification scope, HEAD revision, effective module versions, and hashes of release-relevant build/POM inputs. Consume the manifest in a separate preparation invocation so task dependencies can be configured without network access during Gradle configuration. An empty selection exits successfully without tests, signing, or upload.

2.8 [x] Before preparation, bundling, and upload, require unchanged HEAD, a clean working tree, matching effective versions, and matching recorded input hashes. Reject stale manifests. Keep generated artifacts under ignored output paths; any release-time generated POM or japicmp configuration must not modify tracked files.

## 3. Artifact preparation and BOM verification

3.1 [x] Add root aggregation tasks `bundleAll` and `releaseAll`, accepting the manifest and staged artifact repository. Use typed Gradle properties and configuration-cache-compatible inputs. Preserve existing extension setters and single-module task behavior.

3.2 [x] Register a local file publishing repository for selected Gradle publications, including signing outputs, under `build/releaseAll/staging/`. Keep Maven downloads and installs in a separate throwaway `build/releaseAll/maven-local/` repository. Maven resolves staged Gradle publications through a releases-only `file://` remote; it never writes downloads into staging or uses the user's Maven Local repository.

3.3 [x] Make `matrix-bom/verifyBomApi.sh` portable on macOS and Linux before making it a release gate. Replace GNU-only `realpath -m` and `find -printf` with shared Groovy/Java path normalization and directory enumeration helpers, using the already-required Groovy CLI. Batch all path-normalization inputs and directory listings available at each workflow phase into one Groovy invocation returning structured JSON or a single structured TSV batch for shell consumers; do not launch a JVM for each path or directory. Collect the repository, root, BOM, home, and protected Maven-cache paths together. Revalidate after filesystem-changing phases in one further batch when needed. Preserve all deletion safeguards: reject dot-dot and symlink components, resolve existing ancestors for nonexistent paths, check protected directories and marker files, and never delete staging. Retain Bash 4+ and explicitly require `rg`; do not require GNU coreutils or findutils. Add a non-mutating shared preflight used by both the verifier and `releaseAll.sh --dry-run`.

3.3.1 [x] Extend `matrix-bom/verifyBomApi.sh` with a manifest-driven mode that consumes existing staged artifacts through that file remote and uses the separate local cache. Reuse its current path guards, property mapping, `api-it` execution, and japicmp comparison. Retain standalone SNAPSHOT detection and `--modules` behavior. In manifest mode, select exact unreleased release coordinates from the manifest rather than properties ending in `-SNAPSHOT`; validate their presence without republishing or deleting staging. Maintain a shared staging registration/helper for standalone and release workflows.

3.4 [x] Run the adapted verifier whenever `matrix-bom` or `matrix-all` is selected. Install the matching BOM into the isolated local cache before building `matrix-all`; when an unselected same-version BOM is already released, resolve that exact BOM from Central. Run the existing API integration tests and, when selected core is covered by the BOM, japicmp against `matrixCoreBaselineVersion`. Preserve compatibility findings as reported warnings and infrastructure/test failures as blocking errors. Do not update the baseline.

3.5 [x] Retain Central-only dependency/plugin resolution plus the staged file remote for verification. Use isolated verification settings without signing. For signed Maven artifact preparation, preserve user/global Maven settings and their GPG configuration while adding the staging repository profile and isolated local-cache argument; do not reuse `verify-settings.xml` as both `-s` and `-gs` for signing. Fail clearly if a configured mirror prevents staged dependency resolution. Keep secrets out of manifests, reports, and generated repository configuration.

3.6 [x] Prepare Maven candidates in dependency order through the existing release profile's `verify`/`install` lifecycle, never `deploy`, and prove this performs no Central uploads. Copy only the manifest's expected artifact files from build/install outputs into staging, including the signed flattened POM for `matrix-all`. After flattening, require published coordinates and resolved dependency coordinates/scopes/classifiers to match the static manifest expectations; differences abort bundling.

3.7 [x] Preserve `matrix-all`'s current JAR packaging and publication artifact set through an explicit per-publication policy requiring its POM and main JAR, their signatures, and checksums. Do not introduce sources/Javadoc plugin executions or change packaging. Other JAR publications continue requiring sources and Javadoc JARs; POM packaging requires no JARs. This is a deliberate `matrix-all` compatibility exception, not a claim that Central generally waives these requirements; server validation remains authoritative. [Central requirements](https://central.sonatype.org/publish/requirements/)

## 4. Bundle, upload, and script

4.1 [x] Extract shared checksum and validation logic from the plugin. Build the archive from an allow-list of exact manifest coordinates and expected files, not a repository-directory scan or deny-list. Preserve the existing POM/JAR artifact set without introducing Gradle module metadata. Reject missing signatures, invalid POM metadata or coordinates, incorrect MD5/SHA-1 checksums, duplicate archive paths, unexpected components, and bundles over the supported size limit before upload.

4.2 [x] Make `releaseAll.sh` executable and independent of the caller's working directory. Before discovery, run the shared non-mutating preflight, including in `--dry-run`: verify Java 21, Maven 3.9.9+, Bash 4+, Groovy CLI, Git, and ripgrep (`rg`), and exercise portable path normalization/directory enumeration without creating or deleting files. On macOS, document Homebrew Bash (`brew install bash`) and ripgrep (`brew install ripgrep`) and ensure `/usr/bin/env bash` resolves to Bash 4+, rather than Apple's Bash 3.2. GNU realpath/find are not prerequisites after section 3.3's portability change. Require publishing credentials for Portal checks and signing configuration for preparation; dry-run reports missing signing setup without invoking signing. Check Git cleanliness without running a formatter. After discovery determines the verification scope, run section 4.2.5's external prerequisite preflight before release verification builds, tests, signing, or staging, including in `--dry-run`; only compilation of the dedicated authentication-preflight source set, matrix-gsheets main classes, and their required project dependencies (including matrix-core) is permitted first; the initial tool preflight remains before discovery. Dry-run may therefore compile matrix-core and required dependencies, but never runs verification tests, signs, or stages artifacts.

4.2.1 [x] `./releaseAll.sh --dry-run`: discover and report eligibility, dependency edges, drift, and verification scope; no signing or upload.

4.2.2 [x] `./releaseAll.sh --bundle-only`: run verification and create the validated ZIP without uploading.

4.2.3 [x] `./releaseAll.sh`: verify, prepare, bundle, upload, and check publication.

4.2.4 [x] `./releaseAll.sh --resume <deployment-id>`: resume status/availability checks against the saved manifest and deployment record, without rebuilding or uploading. Permit status-only recovery after source changes, but never reuse that record to authorize a new upload.

4.2.5 [x] Derive external prerequisites from tests actually tagged `external` and enabled by the final verification scope and effective tag filters, not module names, comments, or profile activation alone. Inspect real class/method annotations and maintain prerequisite mappings for those tests, checked against the tag inventory so additions cannot silently bypass preflight. With external tests enabled, credentialed gsheets tests invoke section 4.2.6's authentication check for the union of scopes requested by the enabled tests, including cleanup, and bounded probes of the corresponding endpoints; enabled datasets external tests probe their dataset endpoints. BOM API verification currently contains no external-tagged tests, so BOM/all-only verification requires neither Google credentials nor dataset endpoint probes. Report each prerequisite's triggering test and fail early with setup instructions or the explicit `RUN_EXTERNAL_TESTS=false` override. Dedicated BigQuery opt-in additionally requires ADC, `GOOGLE_CLOUD_PROJECT`, and a running Docker daemon. Dataset-only checks need no ADC; disabled external tests and status-only `--resume` need no test prerequisites.

4.2.6 [x] Add an `authPreflight` source set in matrix-gsheets with a small entry-point class exposing `main(String[] args)` and a shared external-test authentication requirements helper. Both the preflight and external-test setup consume this helper's scope requirements for enabled tests, including their cleanup paths. The current union is `GsAuthenticator.SCOPES`, `SCOPE_SHEETS_READONLY`, and `SCOPE_DRIVE_FILE`, deduplicated; probe both Sheets and Drive endpoints plus the existing authentication endpoints. Call `GsAuthenticator.authenticate(requiredScopes)` to reuse its noninteractive scope validation and quota-project handling, including broader Sheets covering Sheets-readonly and broader Drive covering drive.file; do not duplicate that implication logic or accept refresh success alone. Run a Gradle `JavaExec` against the entry point with `authPreflight` output, main output, main runtime dependencies, and explicit Groovy runtime, groovy-json, and project(':matrix-core') dependencies on a dedicated configuration. Explicitly add `platform(libs.groovy.bom)` to `authPreflightImplementation`, `authPreflightRuntimeOnly`, and the dedicated JavaExec dependency configuration so both source-set compile/runtime resolution and JavaExec resolve the versionless Groovy catalog entries; do not assume standard configurations' BOM constraints are inherited. Extend the root `GroovyCompile` task filter for matrix-gsheets to include `compileAuthPreflightGroovy` and apply the same `config/groovy/compileStatic.groovy` script as main code. Include `codenarcAuthPreflight` in static-analysis gates before formatting/tests. Make the test source set consume the shared helper output; never include `authPreflight` classes in the published JAR, sources/Javadoc artifacts, or POM dependencies. Wire source-set compilation and required project outputs explicitly: the preflight also compiles matrix-gsheets, matrix-core, and necessary dependencies before verification builds, including in dry-run. Do not run tests, signing, staging, browser login, or credential writes. Sanitize failures so tokens and credential contents never enter logs or manifests.

4.2.7 [x] Add offline scope-coverage regression tests executing the actual credential-acquisition paths of `GsUtil.deleteSheet(String)`, `GsUtil.getSheetNames(String)` without supplied credentials, public `GsheetsWriter` write/update operations, and `GsheetsReader.read`, `readAsStrings`, and `readAsObject`, all without supplied credentials. Exercise the corresponding `GsImporter` wrappers and `GsExporter` writer delegation. `DEFAULT_BACKEND` is private and final, so its public injected-backend overload does not affect these callers. Open the Mockito static mock with a forwarding answer that intercepts only `GsAuthenticator.authenticate(List)` to record scopes and throw a sentinel, and delegates every other overload to `Mockito.CALLS_REAL_METHODS`. Do not install a separate `.when(authenticate(anyList()))` stub under `CALLS_REAL_METHODS`: Mockito invokes the real method while installing that stub, causing an unintended credential lookup. The real String overload must delegate to that List interception; assert delegation explicitly. Assert every tested operation reaches the interception with valid inputs and that every recorded scope is covered by the shared helper's union using existing scope implication logic. Stop before credential lookup, API requests, or spreadsheet creation; `getSheetNames` may construct its trusted HTTP transport before authentication, which does not itself make a network call. Close the static mock after each operation; do not add mutable global production backend hooks.

4.2.8 [x] Maintain a checked inventory of every production call to `GsAuthenticator.authenticate` in matrix-gsheets `src/main`, including statically imported calls and ignoring comments/GroovyDoc. Use Groovy AST inspection with matrix-gsheets' compile classpath and explicitly resolve single-name, aliased, and wildcard static imports from the module AST import tables. Resolve unqualified calls such as `authenticate(SCOPES)` to their imported owner, including `GsAuthenticator.*`; do not rely on the parsed call initially naming `GsAuthenticator`. Respect local method/closure shadowing and fail on unresolved ambiguous authentication targets. Identify call sites by class, enclosing method, and overload/call expression; require each to map to an exercised coverage operation or a documented exclusion. The current service call sites are `GsUtil.deleteSheet`, `GsUtil.getSheetNames`, `GsheetsReader.buildSheetsService`, and `GsheetsWriter.buildSheetsService`; map private builders to their public read/write operations and cover importer/exporter delegation. Exclude only authenticator-internal overload delegation and explicit interactive-authentication internals, which have their own authentication tests. Fail the inventory test on added or changed unmapped calls so a new service scope request cannot silently escape coverage.

4.3 [x] Enable `RUN_EXTERNAL_TESTS=true` and `RUN_SLOW_TESTS=true` by default for normal build tasks and the BOM verifier; respect explicit `false` overrides and preserve existing tag exclusions. Define the Gradle verification set as selected Gradle modules plus their transitive production project dependants, excluding examples. This includes SNAPSHOT dependants for regression testing but never publishing them. Run scoped `codenarcMain` plus `:matrix-gsheets:codenarcAuthPreflight` when its preflight source set is involved, then `spotlessCheck`, then `build -Pheadless=true`; `build` must include module-specific checks such as `matrix-csv:testNonUtf8DefaultEncoding`. Dedicated external tasks are opt-in through `RELEASE_ALL_DEDICATED_EXTERNAL_TESTS=true` (default `false`) and also require `RUN_EXTERNAL_TESTS=true`; reject contradictory settings during preflight. For BigQuery, add a release-specific external task that includes `external` and excludes `flaky`, leaving the existing `externalTest` task unchanged. Never gate a release on flaky-tagged tests. Dependencies may compile as prerequisites; unrelated modules' tests do not become release gates. The selected BOM/all additionally gates on section 3's verifier, preserving its existing Docker-unavailable emulator skips. Run section 4.2.5's prerequisite checks for the actual enabled external-tagged tests, even when dedicated external tasks are disabled; an external Maven profile alone does not create credential or network requirements.

4.4 [x] Implement Sonatype's `checkComponentPublished` contract: authenticated `GET /api/v1/publisher/published` with required URL-encoded `namespace`, `name`, and `version` parameters, using the existing Bearer token authentication (Basic is also supported). Parse the success body as `{"published": true|false}` with a required boolean; do not coerce missing or malformed values to false. The documented error responses are 400, 401, 403, and 500: 400/401/403 block immediately; retry 500 transiently within bounds, then block. Other unexpected statuses or transport errors also block. Source: the official OpenAPI at `https://central.sonatype.com/api-doc`, operation `checkComponentPublished`; record its capture date with section 4.4.1's fixtures. Use section 4.4.1's recorded fixtures to establish namespace and absence semantics; never map absence or authorization errors to not-published. [Official API entry point](https://central.sonatype.com/api-doc)

4.4.1 [ ] Capture authenticated, sanitized published-status fixtures using read-only requests for a known published Matrix coordinate and a coordinate never uploaded. Establish whether `namespace` is the exact Maven groupId or the registered namespace; record the tested parameter values, response statuses/bodies, capture date, and resulting namespace/absence policy alongside the fixtures. Never store credentials or Authorization headers. Confirm the tests in section 5.1 pass with these fixtures and record their commands/results before marking this task complete. This is a prerequisite for discovery that depends on Portal absence, including dry-run, and every real upload; until confirmed, fail closed rather than permitting repository-only absence.

4.5 [x] Immediately before upload, recheck every selected coordinate against both repository and Portal status, bypassing earlier availability results. If any coordinate is now published, stop and require fresh planning and bundling. Check locally recorded deployments for overlapping coordinates and resume unresolved submissions instead of uploading again. These checks reduce duplicate submissions but cannot guarantee atomicity against another publisher racing the upload.

4.6 [x] Reuse the Central client and shared deployment polling logic. Upload exactly once with automatic publishing. Persist deployment ID and bundle hash immediately; never automatically retry an upload with an unknown outcome. Resume polls the recorded deployment; failures report affected coordinates. After publication, poll for every selected POM and main JAR, where applicable, on Central, treating propagation delay separately from upload failure.

4.7 [x] Document prerequisites, credentials, signing, all commands/defaults, verification scope, dependency exclusions, output locations, `matrix-all`'s artifact exception, and recovery from stale manifests, failed deployments, and interrupted uploads. Explain that strict compile-classpath eligibility is intentional: if core is `3.10.0-SNAPSHOT`, a non-SNAPSHOT stats module with `compileOnly project(':matrix-core')` is skipped even when core `3.9.x` exists on Central. The dependency must target core's already-released exact version, or core must join the bundle with a release version; existing separate-release scripts retain their behavior. Show a BOM pin to released arff `0.3.0` remaining valid when unselected arff's build version is `0.3.1-SNAPSHOT`. Document prerequisites from the enabled external-tagged test inventory alongside the opt-in ones: credentialed gsheets tests require ADC validated against the shared union of Sheets, Sheets-readonly, and Drive-file scopes (including cleanup) by the existing noninteractive authenticator, and access to both Sheets and Drive endpoints; document that broader Drive grants satisfy Drive-file through the existing scope checks; datasets external tests require their dataset endpoints; dedicated BigQuery checks additionally require `GOOGLE_CLOUD_PROJECT` and Docker. Explain that the current BOM API tests are offline helpers or separately handled emulator tests, so a BOM/all-only release adds no Google credential or dataset endpoint requirement. Document the dedicated unpublished `authPreflight` entry point/classpath, its explicit Groovy runtime/groovy-json/matrix-core dependencies, and compilation of matrix-core and necessary project dependencies before authentication, including in dry-run. Include credential setup, endpoint-specific network failure messages, and `RUN_EXTERNAL_TESTS=false` instructions. Document dedicated checks as opt-in and unconditional exclusion of flaky-tagged tests from release gates. Do not rewrite versions, update the core baseline, commit, push, or create tags.

4.8 [ ] Deliver and validate the plugin enhancement first, test Matrix against it locally, then update Matrix's plugin version to the published enhancement. Keep plugin publication as a separate release step.

## 5. Verification and acceptance

5.1 [ ] Extend plugin unit and TestKit tests for exact-version lookup, repository 404 with Portal-published status, explicit Portal absence, mixed versions, customized coordinates, empty selections, and dependency exclusion cascades. Include `compileOnly` edges and confirm SNAPSHOT or blocked core excludes stats/ggplot even without those dependencies in their POMs. Use confirmed Portal fixtures for groupId versus registered-namespace semantics and never-uploaded coordinates; cover required query encoding, strict boolean parsing, blocking 400/401/403, bounded retries for 500, unexpected statuses, and network failures.

5.2 [x] Test static Maven planning with same-version artifacts unavailable from Central and an empty local cache. Cover selected-module BOM pin mismatches blocking BOM/all, unselected module pins to released versions producing only warnings, unavailable unselected pins blocking BOM/all, and reevaluation after cascading exclusions. Include arff `0.3.0` pinned in the BOM with unselected build version `0.3.1-SNAPSHOT`. Cover unresolved properties and flattened POM mismatches after staging. Ensure planning does not invoke Maven flattening or depend on Maven Local.

5.3 [x] Test combined JAR/POM bundles and the explicit `matrix-all` artifact policy, checksum correctness, missing signatures, duplicate paths, unexpected files, size rejection, and allow-list isolation from third-party downloads/bookkeeping. Verify existing single-module tests still pass.

5.4 [x] Exercise the adapted BOM verifier with non-SNAPSHOT release candidates, isolated read-only staging, a separate download cache, existing `--modules` and SNAPSHOT workflows, API ITs, japicmp, and signing configuration from user Maven settings. Test on macOS using BSD system utilities and on Linux; GNU realpath/find must not be needed. Cover nonexistent paths, spaces, symlink and dot-dot rejection, protected/ancestor directories, marker enforcement, and dry-run preflight failures before builds or uploads. Assert that all path/listing inputs at a phase are processed in one helper invocation, with no per-path JVM launches. Verify tracked files are unchanged and preparation/`--bundle-only` make no upload requests.

5.5 Workflow acceptance. Mark each sub-task complete only after its tests pass and the exact commands/results are recorded in this plan or the PR description.

5.5.1 [x] Upload and resume: use the mock Central server to prove multiple selected modules cause exactly one upload and excluded modules cause none. Cover a coordinate becoming Portal-published just before upload, deployment failure, unknown upload outcome, timeout, resume without re-upload, and delayed Central availability.

5.5.2 [x] Stale-manifest and scope guards: test changed HEAD, dirty tree, changed effective versions/input hashes, invocation from another directory, scoped regression tests, and unrelated external-test failures not gating release.

5.5.3 [x] External-test preflight: confirm dedicated tasks are absent from the default graph; opt-in BigQuery checks require ADC/project/Docker; contradictory flags fail early; flaky-tagged tests never enter release gates. Check unavailable endpoints and missing/expired credentials are detected before verification builds, including dry-run, allowing only minimal authentication-preflight compilation. Confirm BOM/all-only scope needs neither ADC nor dataset probes, comments/profile activation do not count as tags, dataset-only scope needs no ADC, disabled external tests bypass probes, and status-only resume needs no test credentials. Check prerequisite mappings against the actual external-tagged inventory so test additions/removals update required capabilities.

5.5.4 [x] GSheets authentication preflight and classpath: test noninteractive authentication/quota-project handling through its injectable backend without real credentials. Confirm refreshable ADC missing required scopes fails before spreadsheet creation, valid Sheets plus drive.file passes, and broader Drive covers drive.file through existing checks. Verify cleanup scopes and Sheets/Drive probes. Run the real JavaExec task in an isolated fixture without inherited Groovy version constraints and with controlled authentication responses; require explicit BOM resolution and successful loading of Groovy, groovy-json, Logger, and the entry point. Verify matrix-core compilation prerequisites, the static-compilation script and rejection of invalid authenticator signatures, and `codenarcAuthPreflight` participation in `check`/`build` and the earlier analysis gate. Confirm helper/preflight classes and dependencies do not leak into published artifacts or POMs.

5.5.5 [x] Scope coverage and call-site inventory: confirm tests and preflight consume the shared requirements helper, then execute section 4.2.7's actual production operations under the recording static mock. Assert String-overload delegation reaches the List interception under `CALLS_REAL_METHODS`; include reader/importer/exporter paths and prove no API request occurs. A new uncovered scope must fail coverage. Run section 4.2.8's inventory with fixtures for qualified calls, unqualified single-name imports, aliased imports, and wildcard imports, including a future unqualified call in a `GsAuthUtils`-style fixture. Detect added/changed unmapped production calls; ignore comments and unrelated same-name methods/closures. Resolve fixtures against the module compile classpath and verify the existing unqualified writer call is present. Record the exact commands/results for these coverage and inventory tests independently.

5.6 [x] Run configuration-cache TestKit tests twice with `--configuration-cache --configuration-cache-problems=fail`, overriding Matrix's warning default; require reuse without problems. Cover discovery/preparation task graphs separately.

5.7 [x] Record successful implementation checks before marking tasks complete: plugin `./gradlew build`; Matrix `./gradlew codenarcMain :matrix-gsheets:codenarcAuthPreflight`, `./gradlew spotlessCheck`, then `./gradlew build -Pheadless=true` (which includes the full normal test suite and module checks). Add `bash -n releaseAll.sh matrix-bom/verifyBomApi.sh` and ShellCheck. Record the manifest-scoped release checks and adapted verifier command/results in the release report, including external/slow test overrides and any existing skips.

**Chosen defaults:** include both Maven artifacts; retain slow/external flags for normal builds and BOM verification; dedicated external tasks are opt-in and flaky tests never gate release; use batched portable macOS/Linux verifier helpers with Bash 4+ and explicit ripgrep preflight; derive credential/network preflight from actually enabled external-tagged tests; validate the shared union of gsheets external-test scopes, including Drive cleanup, through its existing noninteractive authenticator; use a statically compiled unpublished entry point/classpath with explicit Groovy BOM, Groovy, groovy-json, and matrix-core dependencies, permitting required main compilation before verification builds and in dry-run; current BOM/all-only verification needs no ADC or dataset endpoint probes; skip blocked modules and their dependants under strict compile-time eligibility, including modules depending on SNAPSHOT core; fail closed on uncertain publication status; preserve the existing `matrix-all` artifact set; block BOM/all on selected-module pin mismatches while allowing released pins for unselected modules with warnings. Implementation verification and remaining operational gates are recorded below.


## 6. Implementation verification record (2026-10-09)

6.1 [x] Plugin implementation and existing separate-release regression tests passed:
`cd ../nexus-release-plugin && ./gradlew build --no-configuration-cache --max-workers=2`.
The combined unit tests cover dependency cascades, old and mismatched BOM pins, strict Portal responses,
allow-listed archives, source guards, exactly one upload for multiple components, a late-published
component, failed deployments, unknown outcomes and propagation/resume. TestKit ran planning,
bundling, preparation and the source guard twice with
`--configuration-cache --configuration-cache-problems=fail`, and asserted reuse. Static Maven paths
are preserved from task inputs, including fixture POMs outside Matrix's directory naming convention.
The preparation cache fixture selects both Maven components and checks their four install commands
over two invocations; real signed Maven preparation is recorded in 6.10. Concurrent local
submissions additionally reserve one journal atomically and produce only one upload.

6.2 [x] Matrix analysis, formatting and normal build passed in the required order:
`./gradlew codenarcMain :matrix-gsheets:codenarcAuthPreflight --no-configuration-cache --max-workers=3`;
`./gradlew spotlessCheck --no-configuration-cache --max-workers=3`;
`MAVEN_OPTS="-Dmaven.repo.local=$PWD/build/releaseAll/bom-verify" ./gradlew build -Pheadless=true --no-configuration-cache --max-workers=3 "-Dmaven.repo.local=$PWD/build/releaseAll/bom-verify"`.
The isolated repository was populated by 6.4, avoiding a stale matrix-pict snapshot in the user's Maven Local.
A pre-existing CodeNarc failure in `PlotWithoutJavafxTest` was fixed by loading the class through
its classloader; the test still checks that JavaFX is unavailable.
Then `./gradlew test :matrix-gsheets:jar :matrix-gsheets:sourcesJar :matrix-gsheets:javadocJar :matrix-gsheets:generatePomFileForMavenPublication -Pheadless=true --no-configuration-cache --max-workers=3`
passed. No external credentials or flaky suites were enabled during implementation verification.

6.3 [x] Portable helpers and script orchestration passed:
`groovy scripts/release-all/test-tools.groovy .` (39 assertions);
`groovy scripts/release-all/test-workflow.groovy .`;
`bash -n releaseAll.sh matrix-bom/verifyBomApi.sh scripts/release-all/preflight.sh`;
`shellcheck releaseAll.sh matrix-bom/verifyBomApi.sh scripts/release-all/preflight.sh`.
Coverage includes batched paths with spaces/missing ancestors, symlinks/dot-dot,
namespace-preserving Maven settings with existing signing properties and owner-only permissions,
released-BOM pins, real external annotations versus comments, Java/Groovy tags, aliases,
disabled/flaky/slow filtering, unmapped methods, empty selection, caller directory, scoped build order,
bundle-only, one submission invocation, resume with dirty sources and contradictory flags.
The macOS/Linux portable-tools CI workflow passed; remote results are recorded in 6.14.

6.4 [x] Real macOS portable BOM verification passed in both modes:
`RUN_EXTERNAL_TESTS=false RUN_SLOW_TESTS=false BOM_VERIFY_REPO="$PWD/build/releaseAll/bom-verify" bash matrix-bom/verifyBomApi.sh`;
`groovy scripts/release-all/test-manifest-verifier.groovy . build/releaseAll/bom-verify`.
The manifest fixture verifies 20 unreleased non-SNAPSHOT coordinates with API ITs and japicmp,
read-only staging, separate download cache, no Gradle republishing, no deploy goal and unchanged
staging hashes. Existing Docker-unavailable emulator skips and japicmp compatibility warnings
remain unchanged. Linux integration passed in 6.14; actual signed Maven preparation passed in 6.10.

6.5 [x] GSheets runtime, scope and publication checks passed:
`./gradlew :matrix-gsheets:test --tests '*AuthPreflightTest' --tests '*AuthenticationInventoryTest' --tests '*ReleaseScopeCoverageTest' :matrix-gsheets:releaseAuthPreflight --args=--check-classpath --no-configuration-cache --max-workers=3`
passed six tests and the real JavaExec.
The full build includes these tests too. Authentication/probe responses are mocked; no real Sheets
are created. Main, sources and Javadoc JAR ZIP entries were inspected and contain no preflight/helper
classes; the generated POM contains no preflight, matrix-core or groovy-json runtime dependency.
Independent JavaExec classpath and controlled-authentication acceptance also passed in 6.11.

6.6 [x] Actual Matrix release wiring passed twice:
`./gradlew help -PreleaseAllMode=true -PreleaseAllPluginDir=../nexus-release-plugin --configuration-cache --configuration-cache-problems=fail --max-workers=3`.
The second invocation reused the cache. Verification builds intentionally disable configuration
cache for existing test listeners; discovery, preparation/guard and bundling have strict TestKit reuse.

6.7 [ ] Capture live Portal fixtures (4.4.1). No Sonatype credentials are available in this environment.
`groovy scripts/release-all/capture-portal-fixture.groovy` fails safely with setup instructions.
Mock responses are deliberately not committed as purported live fixtures. Absence-based discovery
and every real upload remain blocked until authenticated read-only capture confirms the contract.
This also leaves the live-fixture portion of 5.1 incomplete.

6.8 [ ] Publish the enhanced plugin separately and update Matrix's published plugin version (4.8).
The implementation uses the explicitly selected sibling composite; ordinary Matrix builds retain
the existing plugin version. No plugin/Matrix publication, Git tag or version rewrite has occurred.

6.9 [x] Linux verifier integration acceptance passed in 6.14. Local checks run on macOS; the remote workflow verifies both OSes and both BOM modes on Linux.


6.10 [x] Actual signed Maven preparation passed:
`groovy scripts/release-all/test-maven-preparation.groovy . ../nexus-release-plugin`.
This isolated fixture uses a disposable GPG key and its own settings/repositories, never the user's
keyring or Maven Local. It runs the real preparation and bundle tasks, installs a same-version BOM,
resolves a staged library, builds/signs an aggregate JAR, validates its flattened dependency POM,
verifies every staged signature with GPG, preserves the input settings and removes temporary signing
settings. It checks no deploy goal occurs and the fixture Git tree remains clean.
The check exposed and fixed private-method closure dispatch, GString ProcessBuilder arguments and
missing console forwarding of subprocess output. Nonempty Maven preparation in TestKit now covers
these paths as well as strict cache reuse. GPG was installed to perform this check.

6.11 [x] Independent source-set/runtime acceptance passed:
`groovy scripts/release-all/test-auth-classpath.groovy .`.
The fixture copies the actual unpublished helper/entry point, excludes existing Groovy JARs, adds
versionless Groovy/groovy-json with explicit BOM constraints to its own configurations and runs
JavaExec outside Matrix's shared build. It loads main/core/JSON classes, exercises the same mocked
scope/authentication/Sheets-and-Drive probe checks in a separate Java process, and confirms static
compilation rejects an invalid authenticator signature. No real credentials or API requests are used.

6.12 [x] Extended early-failure orchestration acceptance passed:
`groovy scripts/release-all/test-workflow.groovy .`.
Authentication failures in dry-run/bundle-only stop before analysis/build/staging; dataset endpoint
failure requires no Sheets preflight; dedicated BigQuery is absent by default and missing project,
Docker or explicit ADC-file prerequisites block opt-in. The authentication and network responses in
this shell fixture are controlled; real entry-point behavior is verified in 6.5 and 6.11.

6.13 [x] The actual clean-checkout command `./releaseAll.sh --dry-run` passed after implementation commits.
It generated Gradle POMs and the manifest/report, found all current publications to be SNAPSHOTs,
reported an empty selection and exited without tests, signing, staging, credentials or uploads.
The report exposed Maven test dependencies that flattening omits; those are now excluded from
Maven eligibility, with a TestKit regression proving a test-only SNAPSHOT does not block an aggregate.


6.14 [x] Remote CI passed on the implementation commit:
[Matrix workflow](https://github.com/Alipsa/matrix/actions/runs/37967268614)
ran `groovy scripts/release-all/test-tools.groovy .` and
`groovy scripts/release-all/test-workflow.groovy .` on macOS and Linux, plus
`BOM_VERIFY_REPO="$GITHUB_WORKSPACE/build/releaseAll/bom-verify" bash matrix-bom/verifyBomApi.sh`
and `groovy scripts/release-all/test-manifest-verifier.groovy . build/releaseAll/bom-verify`
on Linux with `RUN_EXTERNAL_TESTS=false RUN_SLOW_TESTS=false`.
Both verifier modes completed API ITs/japicmp; manifest mode confirmed 20 unreleased fixture
coordinates, immutable staging and separate cache. The
[plugin workflow](https://github.com/Alipsa/nexus-release-plugin/actions/runs/37967189167)
passed `./gradlew build` and Gradle 8.13/9.4 compatibility jobs.

6.15 [x] Explicit standalone module selection and repository safeguards passed locally.
`module_properties=$(groovy matrix-bom/BomSnapshots.groovy matrix-bom/bom.xml | cut -d= -f1 | paste -sd, -)`;
`RUN_EXTERNAL_TESTS=false RUN_SLOW_TESTS=false BOM_VERIFY_REPO="$PWD/build/releaseAll/bom-verify" bash matrix-bom/verifyBomApi.sh --modules "$module_properties"`.
This republished the explicit current SNAPSHOT list into the isolated marked repository and ran
API ITs/japicmp successfully. `groovy scripts/release-all/test-tools.groovy .` now runs 39 assertions,
including the verifier's actual guard functions extracted without any deletion/build code:
project/BOM/home/Maven-cache roots and project ancestors reject; an unmarked existing directory
rejects; a marked fixture directory passes; project-looking, symlink and dot-dot directories reject.

6.16 [x] PRs opened after local implementation verification:
[Matrix #476](https://github.com/Alipsa/matrix/pull/476) and
[release plugin #6](https://github.com/Alipsa/nexus-release-plugin/pull/6).
Both use `feature/release-all`; no commits or pushes were made to main.
Only operational fixture capture and the separate published-plugin release/version switch remain
unchecked. Mock fixture tests cannot substitute for credentialed live Portal capture.
