[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-bom/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-bom)
# Matrix-bom (Bill of Materials)
Since matrix modules are release separately, the version numbers of the matrix modules does not align with each other.
This requires som research (reading of readme.md files) to figure out which versions works well with each other.
so a way to handle this in a simpler way is to use the bom file which defines the versions that works best together
in a dependency management section.

The point is that, using the bom, you only need to define the version for the bom and not when declaring dependencies.

An example for matrix-core is as follows for Gradle
```groovy
implementation(platform( 'se.alipsa.matrix:matrix-bom:2.5.1'))
implementation('se.alipsa.matrix:matrix-core')
implementation('se.alipsa.matrix:matrix-spreadsheet')
runtimeOnly('se.alipsa.matrix:matrix-logging') // optional script/small-tool logging default
```
...or the following for maven
```xml
<project>
   ...
   <dependencyManagement>
      <dependencies>
         <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-bom</artifactId>
            <version>2.5.1</version>
            <type>pom</type>
            <scope>import</scope>
         </dependency>
      </dependencies>
   </dependencyManagement>
   <dependencies>
      <dependency>
         <groupId>se.alipsa.matrix</groupId>
         <artifactId>matrix-core</artifactId>
      </dependency>
       <dependency>
           <groupId>se.alipsa.matrix</groupId>
           <artifactId>matrix-spreadsheet</artifactId>
       </dependency>
       <!-- etc. etc. -->
   </dependencies>
   ...
</project>
```

## Matrix-all (convenience jar)

If you want a single dependency that pulls in all Matrix modules with their transitive dependencies, use
the `matrix-all` convenience jar. This is simpler than a BOM import, but less flexible.

Note: `matrix-all` does not include a Groovy runtime on purpose, so you can choose your Groovy
version (4.x or 5.x). Add the Groovy dependency explicitly in your build.

Gradle:
```groovy
implementation('org.apache.groovy:groovy-all:4.0.23') // or 5.x if you prefer
implementation('se.alipsa.matrix:matrix-all:2.5.1')
```

Maven:
```xml
<dependency>
  <groupId>org.apache.groovy</groupId>
  <artifactId>groovy-all</artifactId>
  <version>4.0.23</version>
</dependency>
<dependency>
  <groupId>se.alipsa.matrix</groupId>
  <artifactId>matrix-all</artifactId>
  <version>2.5.1</version>
</dependency>
```

## Verifying a release

The BOM consumer suite verifies the resolved, published artifacts from an isolated Maven repository.
From the repository root, run:

```bash
./matrix-bom/verifyBomApi.sh
```

The runner reads active `-SNAPSHOT` properties from `bom.xml`, publishes exactly those Gradle
modules into `.bom-verify-repo`, installs the BOM there, runs the `api-it` Maven profile, performs
the matrix-core japicmp comparison, and writes coverage reports. A BOM with no active snapshots is
also valid: the runner publishes nothing and resolves every artifact from Maven Central.

The repository is wiped safely in two scopes. The default scoped wipe removes only
`se/alipsa/matrix` and reuses downloaded third-party dependencies. For a completely clean
Central-only dependency cache, use:

```bash
BOM_VERIFY_FULL_WIPE=true ./matrix-bom/verifyBomApi.sh
BOM_VERIFY_REPO=/absolute/path/to/a/guarded/repo ./matrix-bom/verifyBomApi.sh
```

`BOM_VERIFY_REPO` must be an absolute, non-project directory. The runner creates a marker before
it can delete the directory. `BOM_VERIFY_FULL_WIPE` defaults to `false`.

The Maven profile can also be run directly after one full runner invocation has populated the
isolated repository:

```bash
cd matrix-bom
REPO="${BOM_VERIFY_REPO:-$(git rev-parse --show-toplevel)/.bom-verify-repo}"
mvn -s verify-settings.xml -gs verify-settings.xml -Dmaven.repo.local="$REPO" \
  -Papi-it -DskipUnitTests=true -Dit.groups=charts -Dit.failIfNoTests=true verify
```

The relevant switches are:

- `-Papi-it` enables Failsafe `*IT` execution and JaCoCo; it is not active for ordinary BOM
  packaging.
- `-DskipUnitTests=true` skips the existing Surefire unit/smoke tests while retaining Failsafe ITs.
  Do not use `-DskipTests`, which skips both phases.
- `-Dit.groups=<tag>` selects JUnit tags, and `-Dit.excludedGroups=<tags>` excludes them. Exclusion
  wins over inclusion, so an all-external class needs `-Dit.excludedGroups=jfx` when selected.
- `-Dit.failIfNoTests=true` is the default and makes an incorrectly filtered IT run fail loudly.
- `RUN_EXTERNAL_TESTS=true ./matrix-bom/verifyBomApi.sh` activates the sibling
  `api-it-external` profile. That profile must remain declared after `api-it`; it changes the
  default exclusions from `external,jfx` to `jfx`, allowing the offline-safe external-tagged
  checks to run. The BigQuery check starts the `ghcr.io/goccy/bigquery-emulator:0.6.6`
  Testcontainers image and performs a dataset/save/query round trip, so Docker must be installed
  and its daemon must be available for this external run. It does not require Google Cloud
  credentials; live-service coverage remains outside this isolated emulator check.
- `BOM_VERIFY_JAPICMP_OLD=3.8.0` optionally overrides the baseline. Without it, the runner reads
  `matrixCoreBaselineVersion` from `bom.xml`. Compatibility findings are warnings: the japicmp
  report is retained and the release verification continues. A japicmp execution failure or a
  missing report is still an infrastructure failure.

The release script updates `matrixCoreBaselineVersion` only after the released matrix-core POM and
JAR are visible from Maven Central. It prints that the BOM can be committed after the version is
verified; it never commits the change itself. Keep the `groovy-all` pin in this POM aligned with
`v_groovy` in `gradle/libs.versions.toml`; `BomResolutionIT` also asserts the runtime Groovy version
so a stale pin fails the verification suite.

Coverage output is available at:

- `target/site/jacoco-bom-api/index.html` for combined package browsing;
- `target/jacoco-bom-api.xml` for the combined machine-readable report;
- `target/jacoco-per-module/<module>.xml` for module-level instruction and branch totals;
- `japicmp/target/japicmp/cmp.html` and `cmp.diff` for compatibility review.

The documented API checkpoints and the recorded baseline are in
[`api-coverage.md`](api-coverage.md).
