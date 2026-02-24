package charm.render

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Enforces Phase 1 completion governance:
 * parity matrix coverage, completion consistency, and completion prerequisites.
 */
@CompileStatic
class CharmParityGovernanceTest {

  /**
   * Ensure the parity matrix contains exactly one row for every tracked feature/task.
   */
  @Test
  void testParityMatrixContainsRowsForEveryTrackedFeature() {
    Map<String, FeatureRow> planRows = parseFeatureRowsFromPlan()
    Map<String, MatrixRow> matrixRows = parseRowsFromParityMatrix()
    assertEquals(planRows.keySet(), matrixRows.keySet(), 'parity matrix rows must match plan-tracked features exactly')
  }

  /**
   * Ensure plan `[x]` states and matrix `[x]` states stay aligned.
   */
  @Test
  void testCompletionStatesStayInSyncBetweenPlanAndMatrix() {
    Map<String, FeatureRow> planRows = parseFeatureRowsFromPlan()
    Map<String, MatrixRow> matrixRows = parseRowsFromParityMatrix()

    Set<String> completedInPlan = planRows.findAll { String _, FeatureRow row -> row.completed }.keySet()
    Set<String> completedInMatrix = matrixRows.findAll { String _, MatrixRow row -> row.completed }.keySet()
    assertEquals(completedInPlan, completedInMatrix, 'completed feature sets must match between plan and parity matrix')
  }

  /**
   * Enforce completion preconditions for rows marked `[x]`.
   */
  @Test
  void testCompletedRowsRequireImplementationTestsAndRecordedCommands() {
    Map<String, MatrixRow> matrixRows = parseRowsFromParityMatrix()
    List<MatrixRow> completedRows = matrixRows.values().findAll { MatrixRow row -> row.completed } as List<MatrixRow>

    completedRows.each { MatrixRow row ->
      assertFalse(row.implementation == '-', "completed row '${row.feature}' must reference implementation")
      assertFalse(row.tests == '-', "completed row '${row.feature}' must reference tests")
    }

    if (!completedRows.isEmpty()) {
      List<String> lines = Files.readAllLines(resolveDocPath('docs/gg-charm-parity-matrix.md'))
      long checkedCommands = (lines.count { String line -> line ==~ /^- \[x\] `.*`$/ } as Number).longValue()
      assertTrue(checkedCommands > 0, 'at least one recorded test command must be checked when rows are completed')
    }
  }

  private static Map<String, FeatureRow> parseFeatureRowsFromPlan() {
    List<String> lines = Files.readAllLines(resolveDocPath('docs/gg-charm-migration.md'))
    Map<String, FeatureRow> rows = [:]
    String area = null
    String priority = null

    lines.each { String line ->
      if (line.startsWith('### 3.1 ')) {
        area = 'Geom'
        priority = 'P0'
        return
      }
      if (line.startsWith('### 3.2 ')) {
        area = 'Stat'
        priority = 'P0'
        return
      }
      if (line.startsWith('### 3.3 ')) {
        area = 'Position'
        priority = 'P0'
        return
      }
      if (line.startsWith('### 3.4 ')) {
        area = 'Coord'
        priority = 'P0'
        return
      }
      if (line.startsWith('### 3.5 ')) {
        area = 'Scale'
        priority = 'P0'
        return
      }
      if (line.startsWith('### 3.6 ')) {
        area = 'Annotation'
        priority = 'Cross-cutting'
        return
      }
      if (line.startsWith('### 3.7 ')) {
        area = 'Guide'
        priority = 'Cross-cutting'
        return
      }
      if (line.startsWith('### 3.8 ')) {
        area = 'Expression'
        priority = 'Cross-cutting'
        return
      }
      if (line.startsWith('### 3.9 ')) {
        area = 'Helper'
        priority = 'Cross-cutting'
        return
      }
      if (line.startsWith('### 3.10 ')) {
        area = 'Theme'
        priority = 'Cross-cutting'
        return
      }
      if (line.startsWith('### 3.11 ')) {
        area = 'Charm DSL'
        priority = 'Cross-cutting'
        return
      }

      if (line.startsWith('**P0')) {
        priority = 'P0'
        return
      }
      if (line.startsWith('**P1')) {
        priority = 'P1'
        return
      }
      if (line.startsWith('**P2')) {
        priority = 'P2'
        return
      }

      Matcher sectionFeature = (line =~ /^(3\.[0-9]+\.[0-9]+) \[([ x])\] (.+)$/)
      if (sectionFeature.matches()) {
        String featureText = normalizeFeatureText(sectionFeature.group(3), area)
        String key = "${area}: ${featureText}"
        rows[key] = new FeatureRow(
            key: key,
            id: sectionFeature.group(1),
            priority: priority,
            completed: sectionFeature.group(2) == 'x'
        )
        return
      }

      Matcher phaseFeature = (line =~ /^(5\.(9|10|11|12)\.[0-9]+) \[([ x])\] (.+)$/)
      if (phaseFeature.matches()) {
        String key = "Phase Task ${phaseFeature.group(1)}: ${phaseFeature.group(4)}"
        rows[key] = new FeatureRow(
            key: key,
            id: phaseFeature.group(1),
            priority: 'Cross-cutting',
            completed: phaseFeature.group(3) == 'x'
        )
      }
    }
    rows
  }

  private static Map<String, MatrixRow> parseRowsFromParityMatrix() {
    List<String> lines = Files.readAllLines(resolveDocPath('docs/gg-charm-parity-matrix.md'))
    Map<String, MatrixRow> rows = [:]

    lines.each { String line ->
      if (!line.startsWith('| ')) {
        return
      }
      if (line.startsWith('| Feature ') || line.startsWith('|---')) {
        return
      }
      String[] parts = line.split('\\|', -1)
      if (parts.length < 7) {
        return
      }
      String feature = parts[1].trim()
      String priority = parts[2].trim()
      String implementation = parts[3].trim()
      String tests = parts[4].trim()
      String status = parts[5].trim()
      if (feature.isEmpty()) {
        return
      }
      rows[feature] = new MatrixRow(
          feature: feature,
          priority: priority,
          implementation: implementation,
          tests: tests,
          completed: status == '[x]'
      )
    }
    rows
  }

  private static String normalizeFeatureText(String raw, String area) {
    if (area == 'Guide' && raw.startsWith('Guide: ')) {
      return raw.substring('Guide: '.length())
    }
    raw
  }

  private static Path resolveDocPath(String relativePath) {
    // Try current directory (module root when run from matrix-charts)
    Path modulePath = Paths.get(relativePath)
    if (Files.exists(modulePath)) {
      return modulePath
    }
    // Try sibling matrix-ggplot module (gg docs moved there during extraction)
    Path siblingGgplotPath = Paths.get('../matrix-ggplot').resolve(relativePath)
    if (Files.exists(siblingGgplotPath)) {
      return siblingGgplotPath
    }
    // Try from project root
    Path ggplotPath = Paths.get('matrix-ggplot').resolve(relativePath)
    if (Files.exists(ggplotPath)) {
      return ggplotPath
    }
    Path repoPath = Paths.get('matrix-charts').resolve(relativePath)
    if (Files.exists(repoPath)) {
      return repoPath
    }
    throw new IllegalStateException("Unable to resolve documentation path '${relativePath}'")
  }

  private static class FeatureRow {
    String key
    String id
    String priority
    boolean completed
  }

  private static class MatrixRow {
    String feature
    String priority
    String implementation
    String tests
    boolean completed
  }
}
