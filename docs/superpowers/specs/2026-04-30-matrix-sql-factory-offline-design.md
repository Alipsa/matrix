# matrix-sql Factory Offline Behavior — Design Spec

**Phase:** 4 of matrix-sql 2.4.0 roadmap (section 5)
**Date:** 2026-04-30

## Problem

`MatrixSqlFactory.create(ConnectionInfo, String version)` throws `RuntimeException` when
`version == null` and the Maven Central version lookup fails. The typed factory methods
`createH2` and `createDerby` already have inline fallback versions, but those are hardcoded
strings scattered in method bodies rather than a centralized registry.

## Goal

Make generic factory creation predictable in offline or restricted-network environments:
- Known providers fall back to a pinned version instead of throwing.
- Fallback versions live in one place and are easy to update.
- Lookup failures are logged as warnings, not silently swallowed.
- Unsupported providers still fail with a clear message including dependency coordinates.

## Design

### 1. Fallback version registry

Add one static field to `MatrixSqlFactory`:

```groovy
static final Map<DataBaseProvider, String> FALLBACK_VERSIONS = [
    (DataBaseProvider.H2)   : '2.4.240',
    (DataBaseProvider.DERBY): '10.17.1.0'
].asImmutable()
```

Only H2 and DERBY receive entries initially — they are the only providers with typed
factory methods and verified test coverage. Additional providers can be added as their
versions are validated. `asImmutable()` prevents accidental mutation at runtime.

### 2. `createH2` and `createDerby`

Replace the inline string literals in each method's catch block with a lookup from
`FALLBACK_VERSIONS`. Log message structure and flow are unchanged.

```groovy
// createH2
dependencyVersion = FALLBACK_VERSIONS[DataBaseProvider.H2]
log.warn("Failed to fetch latest H2 artifact, falling back to version $dependencyVersion: ${e.message}", e)

// createDerby
dependencyVersion = FALLBACK_VERSIONS[DataBaseProvider.DERBY]
log.warn("Failed to fetch latest Derby artifact, falling back to version $dependencyVersion: ${e.message}", e)
```

### 3. Generic `create(ConnectionInfo, String version)`

Replace the bare re-throw with fallback-aware logic. `getDependencyName()` already runs
before the try/catch and provides the coordinates for the error message.
`DataBaseProvider.fromUrl(ci.url)` resolves the URL to a provider for the map lookup.
If it returns `UNKNOWN` (no match), the map lookup returns `null` and the error path is taken.

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

### 4. Tests

`ArtifactLookup` is a non-final concrete class. Tests replace the static
`MatrixSqlFactory.artifactLookup` field with an anonymous subclass that throws
`IOException`, and restore it in a `finally` block.

```groovy
ArtifactLookup original = MatrixSqlFactory.artifactLookup
try {
    MatrixSqlFactory.artifactLookup = new ArtifactLookup() {
        @Override String fetchLatestVersion(String g, String a) throws Exception {
            throw new IOException("Simulated network failure")
        }
    }
    // assertions
} finally {
    MatrixSqlFactory.artifactLookup = original
}
```

Four test methods:

| Test | What it verifies |
|---|---|
| `testGenericCreateFallsBackOnNetworkFailure` | `create(h2Url, user, pass)` succeeds; `connectionInfo.dependency` contains the H2 fallback version |
| `testCreateH2FallsBackOnNetworkFailure` | `createH2(url, user, pass)` succeeds; dependency contains H2 fallback version |
| `testCreateDerbyFallsBackOnNetworkFailure` | `createDerby(dbName)` succeeds; dependency contains Derby fallback version (no DB connection needed) |
| `testGenericCreateThrowsWithCoordinatesWhenNoFallback` | PostgreSQL URL with no fallback throws `RuntimeException` whose message contains the dependency coordinates |

## Files Changed

- `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixSqlFactory.groovy`
- `matrix-sql/src/test/groovy/MatrixSqlTest.groovy`

## Out of Scope

- Adding fallback versions for providers other than H2 and DERBY.
- Changing `getDependencyName()` return type.
- Any change to `MatrixSql`, `MatrixDbUtil`, or `MatrixResultSet`.
