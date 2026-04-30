# matrix-sql Factory Offline Behavior Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `MatrixSqlFactory` predictable when Maven Central is unreachable by centralising fallback versions and applying them in all three factory paths.

**Architecture:** Add a single `FALLBACK_VERSIONS` map on `MatrixSqlFactory` keyed by `DataBaseProvider`. The two typed methods (`createH2`, `createDerby`) replace their hardcoded inline strings with lookups from that map. The generic `create(ConnectionInfo, String)` gains a new catch path that checks the map before re-throwing, using `DataBaseProvider.fromUrl()` to resolve the provider.

**Tech Stack:** Groovy 5.0.5, `@CompileStatic`, JUnit 5, `se.alipsa.mavenutils.ArtifactLookup` (concrete class — subclassed anonymously in tests)

---

## Files

| File | Change |
|---|---|
| `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixSqlFactory.groovy` | Add `FALLBACK_VERSIONS` map; update `createH2`, `createDerby`, and `create(ConnectionInfo, String)` catch blocks |
| `matrix-sql/src/test/groovy/MatrixSqlTest.groovy` | Add `se.alipsa.mavenutils.ArtifactLookup` import; add 4 new tests |

---

## Task 1: Add `FALLBACK_VERSIONS` and update typed factory methods

**Files:**
- Modify: `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixSqlFactory.groovy`
- Modify: `matrix-sql/src/test/groovy/MatrixSqlTest.groovy`

- [ ] **Step 1: Add `ArtifactLookup` import to test file**

Add this import after the existing imports in `MatrixSqlTest.groovy`:

```groovy
import se.alipsa.mavenutils.ArtifactLookup
```

- [ ] **Step 2: Write two failing tests**

Add these two test methods to `MatrixSqlTest.groovy`:

```groovy
@Test
void testCreateH2FallsBackOnNetworkFailure() {
    String url = h2MemUrl('fallback_h2_testdb')
    ArtifactLookup original = MatrixSqlFactory.artifactLookup
    try {
        MatrixSqlFactory.artifactLookup = new ArtifactLookup() {
            @Override
            String fetchLatestVersion(String g, String a) throws Exception {
                throw new IOException("Simulated network failure")
            }
        }
        MatrixSql ms = MatrixSqlFactory.createH2(url, 'sa', '123')
        assertTrue(ms.connectionInfo.dependency.contains(MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.H2]),
            "Expected dependency to contain H2 fallback version ${MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.H2]}")
    } finally {
        MatrixSqlFactory.artifactLookup = original
    }
}

@Test
void testCreateDerbyFallsBackOnNetworkFailure() {
    ArtifactLookup original = MatrixSqlFactory.artifactLookup
    try {
        MatrixSqlFactory.artifactLookup = new ArtifactLookup() {
            @Override
            String fetchLatestVersion(String g, String a) throws Exception {
                throw new IOException("Simulated network failure")
            }
        }
        MatrixSql ms = MatrixSqlFactory.createDerby("memory:fallback_derby_${System.nanoTime()}")
        assertTrue(ms.connectionInfo.dependency.contains(MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.DERBY]),
            "Expected dependency to contain Derby fallback version ${MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.DERBY]}")
    } finally {
        MatrixSqlFactory.artifactLookup = original
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
./gradlew :matrix-sql:test --tests "MatrixSqlTest.testCreateH2FallsBackOnNetworkFailure" --tests "MatrixSqlTest.testCreateDerbyFallsBackOnNetworkFailure" --rerun-tasks
```

Expected: compile error — `FALLBACK_VERSIONS` does not exist on `MatrixSqlFactory`.

- [ ] **Step 4: Add `FALLBACK_VERSIONS` to `MatrixSqlFactory`**

Insert this field immediately after the `artifactLookup` field declaration (after line `static ArtifactLookup artifactLookup = new ArtifactLookup(REPO_URL)`):

```groovy
static final Map<DataBaseProvider, String> FALLBACK_VERSIONS = [
    (DataBaseProvider.H2)   : '2.4.240',
    (DataBaseProvider.DERBY): '10.17.1.0'
].asImmutable()
```

- [ ] **Step 5: Update `createH2` catch block**

Replace:
```groovy
      } catch (Exception e) {
        dependencyVersion = '2.4.240'
        log.warn("Failed to fetch latest H2 artifact, falling back to version $dependencyVersion: ${e.message}", e)
      }
```
With:
```groovy
      } catch (Exception e) {
        dependencyVersion = FALLBACK_VERSIONS[DataBaseProvider.H2]
        log.warn("Failed to fetch latest H2 artifact, falling back to version $dependencyVersion: ${e.message}", e)
      }
```

- [ ] **Step 6: Update `createDerby` catch block**

Replace:
```groovy
      } catch (Exception e) {
        dependencyVersion = '10.17.1.0'
        log.warn("Failed to fetch latest Derby artifact, falling back to version $dependencyVersion: ${e.message}", e)
      }
```
With:
```groovy
      } catch (Exception e) {
        dependencyVersion = FALLBACK_VERSIONS[DataBaseProvider.DERBY]
        log.warn("Failed to fetch latest Derby artifact, falling back to version $dependencyVersion: ${e.message}", e)
      }
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
./gradlew :matrix-sql:test --tests "MatrixSqlTest.testCreateH2FallsBackOnNetworkFailure" --tests "MatrixSqlTest.testCreateDerbyFallsBackOnNetworkFailure" --rerun-tasks
```

Expected: both pass.

- [ ] **Step 8: Commit**

```bash
git add matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixSqlFactory.groovy \
        matrix-sql/src/test/groovy/MatrixSqlTest.groovy
git commit -m "Centralise factory fallback versions in FALLBACK_VERSIONS map"
```

---

## Task 2: Add fallback to generic `create()` and cover the no-fallback error path

**Files:**
- Modify: `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixSqlFactory.groovy`
- Modify: `matrix-sql/src/test/groovy/MatrixSqlTest.groovy`

- [ ] **Step 1: Write two failing tests**

Add these two test methods to `MatrixSqlTest.groovy`:

```groovy
@Test
void testGenericCreateFallsBackOnNetworkFailure() {
    String url = h2MemUrl('fallback_generic_testdb')
    ArtifactLookup original = MatrixSqlFactory.artifactLookup
    try {
        MatrixSqlFactory.artifactLookup = new ArtifactLookup() {
            @Override
            String fetchLatestVersion(String g, String a) throws Exception {
                throw new IOException("Simulated network failure")
            }
        }
        MatrixSql ms = MatrixSqlFactory.create(url, 'sa', '123')
        assertTrue(ms.connectionInfo.dependency.contains(MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.H2]),
            "Expected dependency to contain H2 fallback version ${MatrixSqlFactory.FALLBACK_VERSIONS[DataBaseProvider.H2]}")
    } finally {
        MatrixSqlFactory.artifactLookup = original
    }
}

@Test
void testGenericCreateThrowsWithCoordinatesWhenNoFallback() {
    // PostgreSQL is a known provider but has no entry in FALLBACK_VERSIONS
    String pgUrl = 'jdbc:postgresql://localhost:5432/testdb'
    Map<String, String> pgDependency = MatrixSqlFactory.getDependencyName(pgUrl)
    assertNotNull(pgDependency, 'Expected PostgreSQL to be a known provider in DataBaseProvider')

    ArtifactLookup original = MatrixSqlFactory.artifactLookup
    try {
        MatrixSqlFactory.artifactLookup = new ArtifactLookup() {
            @Override
            String fetchLatestVersion(String g, String a) throws Exception {
                throw new IOException("Simulated network failure")
            }
        }
        RuntimeException ex = assertThrows(RuntimeException) {
            MatrixSqlFactory.create(pgUrl, 'user', 'pass')
        }
        assertTrue(ex.message.contains(pgDependency.groupId),
            "Expected message to contain groupId '${pgDependency.groupId}', was: ${ex.message}")
        assertTrue(ex.message.contains(pgDependency.artifactId),
            "Expected message to contain artifactId '${pgDependency.artifactId}', was: ${ex.message}")
        assertTrue(ex.message.contains('no fallback version is configured'),
            "Expected message to mention missing fallback, was: ${ex.message}")
    } finally {
        MatrixSqlFactory.artifactLookup = original
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :matrix-sql:test --tests "MatrixSqlTest.testGenericCreateFallsBackOnNetworkFailure" --tests "MatrixSqlTest.testGenericCreateThrowsWithCoordinatesWhenNoFallback" --rerun-tasks
```

Expected: both fail — `testGenericCreateFallsBackOnNetworkFailure` throws instead of falling back; `testGenericCreateThrowsWithCoordinatesWhenNoFallback` fails because the current error message lacks "no fallback version is configured".

- [ ] **Step 3: Update the `create(ConnectionInfo, String)` catch block**

In `MatrixSqlFactory.groovy`, locate the catch block inside `create(ConnectionInfo ci, String version)`:

```groovy
      } catch (Exception e) {
        throw new RuntimeException ("Failed to fetch latest artifact for $dependency.groupId:$dependency.artifactId", e)
      }
```

Replace it with:

```groovy
      } catch (Exception e) {
        DataBaseProvider provider = DataBaseProvider.fromUrl(ci.url)
        String fallback = FALLBACK_VERSIONS[provider]
        if (fallback != null) {
          dependencyVersion = fallback
          log.warn("Failed to fetch latest artifact for $dependency.groupId:$dependency.artifactId, " +
              "falling back to version $dependencyVersion: ${e.message}", e)
        } else {
          throw new RuntimeException(
              "Failed to fetch latest artifact for $dependency.groupId:$dependency.artifactId" +
              " and no fallback version is configured for this provider", e)
        }
      }
```

- [ ] **Step 4: Run new tests to verify they pass**

```bash
./gradlew :matrix-sql:test --tests "MatrixSqlTest.testGenericCreateFallsBackOnNetworkFailure" --tests "MatrixSqlTest.testGenericCreateThrowsWithCoordinatesWhenNoFallback" --rerun-tasks
```

Expected: both pass.

- [ ] **Step 5: Run full test suite to check for regressions**

```bash
./gradlew :matrix-sql:test --rerun-tasks
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixSqlFactory.groovy \
        matrix-sql/src/test/groovy/MatrixSqlTest.groovy
git commit -m "Add offline fallback to generic create(); improve no-fallback error message"
```

---

## Final verification

- [ ] **Run full module test suite**

```bash
./gradlew :matrix-sql:test --rerun-tasks
```

Expected: all tests pass, no regressions.
