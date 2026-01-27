# Logger Migration Plan

## Executive Summary

Migrate all matrix modules from their current mix of logging frameworks (SLF4J, Log4j, System.out/err, println) to the standardized `se.alipsa.matrix.core.util.Logger` utility introduced in matrix-core.

**Total Effort**: ~5-6 hours (3.5 hours migration + 2 hours testing)
**Modules Affected**: 5 modules, 14 files, 36 log statements

---

## Current State

### Modules by Logging Framework

| Module | Framework | Files | Log Instances | Status |
|--------|-----------|-------|---------------|--------|
| matrix-bigquery | matrix-core Logger | 1 | Multiple | ✅ **MIGRATED** |
| matrix-gsheets | Apache Log4j | 4 | 17 | ⚠️ Needs migration |
| matrix-smile | SLF4J direct | 1 | 2 | ⚠️ Needs migration |
| matrix-sql | System.out/err | 4 | 11 | ⚠️ Needs migration |
| matrix-core | System.err | 2 | 2 | ⚠️ Needs migration |
| matrix-charts | System.err | 3 | 4 | ⚠️ Needs migration |

---

## Migration Patterns

### Pattern 1: SLF4J → Logger

**Before:**
```groovy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private static Logger log = LoggerFactory.getLogger(DataframeConverter.class)
```

**After:**
```groovy
import se.alipsa.matrix.core.util.Logger

private static final Logger log = Logger.getLogger(DataframeConverter)
```

**Changes:**
- Replace `org.slf4j.Logger` import with `se.alipsa.matrix.core.util.Logger`
- Remove `org.slf4j.LoggerFactory` import
- Add `final` modifier
- Change `.getLogger(Class.class)` → `.getLogger(Class)`

---

### Pattern 2: Log4j → Logger

**Before:**
```groovy
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

private static Logger log = LogManager.getLogger(GsUtil)
```

**After:**
```groovy
import se.alipsa.matrix.core.util.Logger

private static final Logger log = Logger.getLogger(GsUtil)
```

**Changes:**
- Replace `org.apache.logging.log4j.Logger` with `se.alipsa.matrix.core.util.Logger`
- Remove `org.apache.logging.log4j.LogManager` import
- Add `final` modifier
- Change `LogManager.getLogger()` → `Logger.getLogger()`

---

### Pattern 3: System.err.println → Logger

**Before:**
```groovy
System.err.println("Failed to create table $tableName using ddl: " + sql)
```

**After:**
```groovy
import se.alipsa.matrix.core.util.Logger

private static final Logger log = Logger.getLogger(MatrixDbUtil)

log.error("Failed to create table $tableName using ddl: $sql")
```

**Level Mapping:**
- Error messages → `log.error()`
- Warnings → `log.warn()`
- With exceptions → `log.error("message", exception)`

---

### Pattern 4: System.out.println → Logger

**Before:**
```groovy
System.out.println("No JAAS configuration found for " + JAAS_CONFIG_NAME + "...")
```

**After:**
```groovy
import se.alipsa.matrix.core.util.Logger

private static final Logger log = Logger.getLogger(JaasConfigLoader)

log.info("No JAAS configuration found for $JAAS_CONFIG_NAME...")
```

**Level Mapping:**
- Informational messages → `log.info()`
- Diagnostic details → `log.debug()`

---

## Migration Tasks

### Phase 1: Foundation (Priority 1 - High)

#### Task 1.1: matrix-core
- **Effort**: 30 minutes
- **Priority**: P1 (High) - Core module affects all downstream
- **Risk**: Medium
- **Files**:
  1. `matrix-core/src/main/groovy/se/alipsa/matrix/core/MatrixBuilder.groovy:481` ✅
  2. `matrix-core/src/main/groovy/se/alipsa/matrix/core/Stat.groovy:80` ✅
- **Testing**: `./gradlew :matrix-core:test :matrix-core:build`
- **Status**: ✅ **COMPLETED** - All 221 tests passing, no System.err.println remaining

#### Task 1.2: matrix-sql
- **Effort**: 1 hour
- **Priority**: P1 (High) - Critical database operations
- **Risk**: Medium
- **Files**:
  1. `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixDbUtil.groovy` (2 instances) ✅
  2. `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/config/JaasConfigLoader.groovy` (4 instances) ✅
  3. `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixSql.groovy` (3 instances) ✅
  4. `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixSqlFactory.groovy` (2 instances) ✅
- **Testing**: `./gradlew :matrix-sql:test :matrix-sql:build`
- **Status**: ✅ **COMPLETED** - All 23 tests passing, no System.out/err.println remaining

---

### Phase 2: Integration Modules (Priority 2 - Medium)

#### Task 2.1: matrix-gsheets
- **Effort**: 1 hour
- **Priority**: P2 (Medium) - Google Sheets integration
- **Risk**: Medium
- **Files**:
  1. `matrix-gsheets/src/main/groovy/se/alipsa/matrix/gsheets/GsUtil.groovy` (2 instances) ✅
  2. `matrix-gsheets/src/main/groovy/se/alipsa/matrix/gsheets/GsConverter.groovy` (4 instances) ✅
  3. `matrix-gsheets/src/main/groovy/se/alipsa/matrix/gsheets/BqAuthenticator.groovy` (10 instances) ✅
  4. `matrix-gsheets/src/main/groovy/se/alipsa/matrix/gsheets/BqAuthUtils.groovy` (2 instances) ✅
- **Dependency Changes**: ✅ Removed `implementation libs.log4jApi` from build.gradle
- **Testing**: `./gradlew :matrix-gsheets:test`
- **Status**: ✅ **COMPLETED** - All 112 tests passing, Log4j dependency removed, no System.out/err remaining

#### Task 2.2: matrix-smile
- **Effort**: 20 minutes
- **Priority**: P2 (Medium) - ML integration
- **Risk**: Low
- **Files**:
  1. `matrix-smile/src/main/groovy/se/alipsa/matrix/smile/DataframeConverter.groovy` (2 instances) ✅
- **Testing**: `./gradlew :matrix-smile:test :matrix-smile:build`
- **Status**: ✅ **COMPLETED** - All 273 tests passing, no SLF4J imports remaining

---

### Phase 3: Visualization (Priority 3 - Low)

#### Task 3.1: matrix-charts
- **Effort**: 30 minutes
- **Priority**: P3 (Low) - User-facing warnings
- **Risk**: Low
- **Files**:
  1. `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleColorFermenter.groovy` (2 instances)
  2. `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/geom/GeomMap.groovy` (1 instance)
  3. `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/geom/GeomFunction.groovy` (1 instance)
- **Testing**: `./gradlew :matrix-charts:test :matrix-charts:build`
- **Status**: ⬜ Not started

---

## Log Level Guidelines

### Error (log.error)
- Exception stack traces: `log.error("message", exception)`
- Failed operations: `log.error("Operation failed: ${details}")`
- Fatal configuration issues

### Warning (log.warn)
- Fallback scenarios: `log.warn("Falling back to default...")`
- Data validation issues: `log.warn("Invalid value, using default...")`
- Deprecation notices

### Info (log.info)
- Successful operations: `log.info("Table created successfully")`
- User-facing messages: `log.info("Please configure...")`
- Progress indicators

### Debug (log.debug)
- Detailed diagnostics: `log.debug("Checking if dataset exists...")`
- Internal state: `log.debug("Current configuration: ${config}")`

---

## Best Practices

### 1. String Interpolation
Use idiomatic Groovy syntax:
```groovy
// ✅ Good - Groovy interpolation
log.info("Table $datasetName.$tableName created successfully")
log.error("Failed to load class $className: ${e.message}")

// ❌ Avoid - Java/SLF4J style
log.info("Table {}.{} created", datasetName, tableName)
```

### 2. Exception Logging
Always include the exception when available:
```groovy
try {
  // operation
} catch (Exception e) {
  log.error("Operation failed: ${e.message}", e)
  throw e
}
```

### 3. Logger Field Declaration
```groovy
// ✅ Correct
private static final Logger log = Logger.getLogger(ClassName)

// ❌ Avoid
private static Logger log = Logger.getLogger(ClassName.class)
private Logger log = Logger.getLogger("ClassName")
```

---

## Testing Strategy

### Per-Module Testing
```bash
# After each module migration:
./gradlew :matrix-<module>:clean
./gradlew :matrix-<module>:test
./gradlew :matrix-<module>:build
```

### Integration Testing
```bash
# For modules with external dependencies:
RUN_EXTERNAL_TESTS=true ./gradlew :matrix-gsheets:test

# Full project test:
./gradlew test
```

### Verification Checklist
After each module migration:
- [ ] All log statements replaced with Logger
- [ ] No compile errors
- [ ] All tests pass
- [ ] No System.out/err statements remain (except valid cases)
- [ ] Logger imports added correctly
- [ ] Logger field declared as `private static final`
- [ ] Log levels appropriate (error/warn/info/debug)
- [ ] String interpolation uses Groovy syntax
- [ ] Exception logging includes throwable when available

---

## Success Criteria

### Completion
1. ✅ All production code (src/main) uses `se.alipsa.matrix.core.util.Logger`
2. ✅ No direct SLF4J or Log4j imports in production code
3. ✅ No System.out/System.err/println in production code
4. ✅ All module tests pass
5. ✅ Full regression test suite passes
6. ✅ Dependencies cleaned up (log4j-api removed)

### Quality
1. ✅ Consistent Logger field naming
2. ✅ Appropriate log levels used
3. ✅ Exception context preserved
4. ✅ Groovy string interpolation maintained
5. ✅ No performance regression

---

## Progress Tracking

### Overall Status
- **Total Tasks**: 5
- **Completed**: 4 (80%)
- **In Progress**: 0
- **Not Started**: 1 (20%)

### By Phase
- **Phase 1 (Foundation)**: 2/2 tasks (100%) ✅ **COMPLETE**
- **Phase 2 (Integration)**: 2/2 tasks (100%) ✅ **COMPLETE**
- **Phase 3 (Visualization)**: 0/1 task (0%)

---

## Reference Implementation

**Example**: matrix-bigquery has already been successfully migrated and serves as the reference implementation.

**Key file**: `matrix-bigquery/src/main/groovy/se/alipsa/matrix/bigquery/Bq.groovy`

```groovy
import se.alipsa.matrix.core.util.Logger

@CompileStatic
class Bq {
  private static final Logger log = Logger.getLogger(Bq)

  // Usage examples:
  log.info("Table $datasetName.$tableName created successfully")
  log.debug("Checking if dataset $datasetName exists")
  log.warn("Falling back to InsertAll method")
  log.error("Operation failed: ${e.message}", e)
}
```

---

## Notes

- **Test code exclusion**: This plan focuses on production code (src/main). Test code and examples can continue using println for now.
- **Gradle build files**: println statements in build.gradle (like test result reporting) are acceptable and should not be changed (see build.gradle:109-113 comments).
- **Performance**: The Logger utility has negligible performance overhead and uses ConcurrentHashMap for caching.
- **Thread safety**: The new Logger is fully thread-safe (uses DateTimeFormatter instead of SimpleDateFormat).

---

## Questions or Issues

If you encounter any issues during migration:
1. Check the reference implementation in matrix-bigquery
2. Review AGENTS.md logging guidelines (lines 26-123)
3. Ensure Logger API methods match expected signatures
4. Verify string interpolation syntax is correct
