package se.alipsa.matrix.gg.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.gg.aes.Aes

/**
 * Consolidated position adjustment utilities for ggplot.
 * Position adjustments modify the position of elements to avoid overlaps
 * or stack them appropriately.
 */
@CompileStatic
class GgPosition {

  /**
   * Identity position - no adjustment.
   * Default for most geoms.
   */
  static Matrix identity(Matrix data, Aes aes, Map params = [:]) {
    return data
  }

  /**
   * Dodge position - place overlapping objects side by side.
   * Used for grouped bar charts.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x and group/fill for grouping)
   * @param params Map with optional: width (dodge width, default 0.9)
   * @return Matrix with adjusted x positions
   */
  static Matrix dodge(Matrix data, Aes aes, Map params = [:]) {
    double width = (params.width ?: 0.9) as double

    String xCol = aes.xColName
    String groupCol = aes.groupColName ?: aes.fillColName ?: aes.colorColName

    if (xCol == null || groupCol == null) {
      return data  // No grouping, return unchanged
    }

    // Get unique groups and x values
    List<Object> groupValues = data[groupCol] as List<Object>
    List<Object> groups = new ArrayList<>(new LinkedHashSet<>(groupValues))
    int nGroups = groups.size()

    if (nGroups <= 1) {
      return data  // Single group, no dodge needed
    }

    // Calculate offset for each group
    double groupWidth = width / nGroups
    Map<Object, Double> groupOffsets = new LinkedHashMap<>()
    groups.eachWithIndex { Object group, int i ->
      double offset = (-width / 2.0d) + (groupWidth / 2.0d) + (i * groupWidth)
      groupOffsets.put(group, offset)
    }

    // Apply offsets - create new matrix with adjusted x
    List<Map<String, Object>> rows = []
    data.each { Row row ->
      Map<String, Object> newRow = new LinkedHashMap<>(row.toMap())
      Object group = newRow[groupCol]
      Object xVal = newRow[xCol]
      if (xVal instanceof Number && groupOffsets.containsKey(group)) {
        newRow[xCol] = (xVal as Number).doubleValue() + groupOffsets[group]
      }
      rows << newRow
    }

    return Matrix.builder().mapList(rows).build()
  }

  /**
   * Stack position - stack overlapping objects on top of each other.
   * Used for stacked bar charts and area charts.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x for grouping, y for stacking)
   * @param params Map with optional: reverse (boolean)
   * @return Matrix with ymin, ymax columns for stacked positions
   */
  static Matrix stack(Matrix data, Aes aes, Map params = [:]) {
    boolean reverse = params.reverse ?: false

    String xCol = aes.xColName
    String yCol = aes.yColName
    String groupCol = aes.groupColName ?: aes.fillColName ?: aes.colorColName

    if (xCol == null || yCol == null) {
      return data
    }

    // Group by x value
    Map<Object, List<Map<String, Object>>> byX = new LinkedHashMap<>()
    data.each { Row row ->
      Object xVal = row[xCol]
      List<Map<String, Object>> bucket = byX.get(xVal)
      if (bucket == null) {
        bucket = []
        byX.put(xVal, bucket)
      }
      bucket << new LinkedHashMap<>(row.toMap())
    }

    // Stack within each x group
    List<Map<String, Object>> results = []
    byX.each { xVal, rows ->
      List<Map<String, Object>> orderedRows = rows
      if (reverse) {
        orderedRows = new ArrayList<>(rows).reverse()
      }
      double cumSum = 0

      orderedRows.each { row ->
        Map<String, Object> newRow = new LinkedHashMap<>(row)
        Object yRaw = row[yCol]
        double yVal = yRaw instanceof Number ? (yRaw as Number).doubleValue() : 0.0d
        newRow['ymin'] = cumSum
        cumSum += yVal
        newRow['ymax'] = cumSum
        double yMin = (newRow['ymin'] as Number).doubleValue()
        double yMax = (newRow['ymax'] as Number).doubleValue()
        newRow['y'] = (yMin + yMax) / 2.0d  // Center of bar
        results << newRow
      }
    }

    return Matrix.builder().mapList(results).build()
  }

  /**
   * Fill position - stack and normalize to 100%.
   * Like stack but scales to fill the entire height.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings
   * @param params Map with optional: reverse (boolean)
   * @return Matrix with ymin, ymax columns normalized to [0, 1]
   */
  static Matrix fill(Matrix data, Aes aes, Map params = [:]) {
    boolean reverse = params.reverse ?: false

    String xCol = aes.xColName
    String yCol = aes.yColName

    if (xCol == null || yCol == null) {
      return data
    }

    // First stack, then normalize
    Matrix stacked = stack(data, aes, params)

    // Get max for each x group
    Map<Object, Double> maxByX = new LinkedHashMap<>()
    stacked.each { Row row ->
      Object xVal = row[xCol]
      Object yMaxRaw = row['ymax']
      double ymax = yMaxRaw instanceof Number ? (yMaxRaw as Number).doubleValue() : 0.0d
      if (!maxByX.containsKey(xVal) || ymax > maxByX[xVal]) {
        maxByX[xVal] = ymax
      }
    }

    // Normalize
    List<Map<String, Object>> results = []
    stacked.each { Row row ->
      Map<String, Object> newRow = new LinkedHashMap<>(row.toMap())
      Object xVal = row[xCol]
      double total = maxByX[xVal] ?: 1.0d

      if (total > 0) {
        double yMin = (newRow['ymin'] as Number).doubleValue() / total
        double yMax = (newRow['ymax'] as Number).doubleValue() / total
        newRow['ymin'] = yMin
        newRow['ymax'] = yMax
        newRow['y'] = (yMin + yMax) / 2.0d
      }
      results << newRow
    }

    return Matrix.builder().mapList(results).build()
  }

  /**
   * Jitter position - add random noise to avoid overplotting.
   * Used for scatter plots with many overlapping points.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings
   * @param params Map with optional: width (x jitter), height (y jitter), seed
   * @return Matrix with jittered positions
   */
  static Matrix jitter(Matrix data, Aes aes, Map params = [:]) {
    double width = (params.width ?: 0.4) as double
    double height = (params.height ?: 0.4) as double
    Long seed = params.seed as Long

    Random random = seed != null ? new Random(seed) : new Random()

    String xCol = aes.xColName
    String yCol = aes.yColName

    List<Map<String, Object>> results = []
    data.each { Row row ->
      Map<String, Object> newRow = new LinkedHashMap<>(row.toMap())

      if (xCol != null && newRow[xCol] instanceof Number) {
        double jitterX = (random.nextDouble() - 0.5) * width
        newRow[xCol] = (newRow[xCol] as Number).doubleValue() + jitterX
      }

      if (yCol != null && newRow[yCol] instanceof Number) {
        double jitterY = (random.nextDouble() - 0.5) * height
        newRow[yCol] = (newRow[yCol] as Number).doubleValue() + jitterY
      }

      results << newRow
    }

    return Matrix.builder().mapList(results).build()
  }
}
