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
    BigDecimal width = params.width as BigDecimal ?: 0.9

    String xCol = aes.xColName
    if (xCol == null || !data.columnNames().contains(xCol)) {
      if (data.columnNames().contains('x')) {
        xCol = 'x'
      }
    }
    String groupCol = aes.groupColName ?: aes.fillColName ?: aes.colorColName
    if (groupCol == null || !data.columnNames().contains(groupCol)) {
      if (data.columnNames().contains('group')) {
        groupCol = 'group'
      }
    }

    if (xCol == null || groupCol == null) {
      return data  // No grouping, return unchanged
    }

    // Group rows by x value and dodge within each x group
    Map<Object, List<Map<String, Object>>> byX = data.rows()
        .groupBy { row -> row[xCol] }
        .collectEntries { xVal, rows ->
          [(xVal): rows.collect { new LinkedHashMap<>(it.toMap()) }]
        } as Map<Object, List<Map<String, Object>>>

    List<Map<String, Object>> rows = []
    byX.each { Object xVal, List<Map<String, Object>> bucket ->
      List<Object> groups = new ArrayList<>(new LinkedHashSet<>(bucket.collect { it[groupCol] }))
      int nGroups = groups.size()
      if (nGroups <= 1) {
        rows.addAll(bucket)
        return
      }

      BigDecimal groupWidth = width / nGroups
      Map<Object, BigDecimal> groupOffsets = new LinkedHashMap<>()
      groups.eachWithIndex { Object group, int i ->
        BigDecimal offset = (-width / 2) + (groupWidth / 2) + (i * groupWidth)
        groupOffsets.put(group, offset)
      }

      bucket.each { Map<String, Object> row ->
        Object group = row[groupCol]
        Object rawX = row[xCol]
        if (rawX instanceof Number && groupOffsets.containsKey(group)) {
          row[xCol] = rawX + groupOffsets[group]
        }
        rows << row
      }
    }

    return Matrix.builder().mapList(rows).build()
  }

  /**
   * Dodge2 position - overlap-aware dodging that respects xmin/xmax widths.
   * Mirrors ggplot2's position_dodge2 behavior.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x and group/fill for grouping)
   * @param params Map with optional: width, padding (0-1), reverse
   * @return Matrix with adjusted x, xmin, xmax columns
   */
  static Matrix dodge2(Matrix data, Aes aes, Map params = [:]) {
    BigDecimal widthParam = params.width instanceof Number ? params.width as BigDecimal : null
    BigDecimal padding = params.padding instanceof Number ? params.padding as BigDecimal : 0.1
    boolean reverse = params.reverse ?: false

    String xCol = aes.xColName
    if (xCol == null || !data.columnNames().contains(xCol)) {
      if (data.columnNames().contains('x')) {
        xCol = 'x'
      }
    }
    String groupCol = aes.groupColName ?: aes.fillColName ?: aes.colorColName
    if (groupCol == null || !data.columnNames().contains(groupCol)) {
      if (data.columnNames().contains('group')) {
        groupCol = 'group'
      }
    }

    if (xCol == null) {
      return data
    }

    // Materialize rows into mutable maps for in-place position updates.
    List<Map<String, Object>> rows = []
    data.each { Row row ->
      rows << new LinkedHashMap<>(row.toMap())
    }

    // For categorical x values, build a mapping from category to numeric position
    Map<Object, BigDecimal> categoryPositions = null
    if (xCol != null) {
      boolean hasNonNumericX = rows.any { Map<String, Object> row ->
        def xVal = row[xCol]
        xVal != null && !(xVal instanceof Number)
      }
      if (hasNonNumericX) {
        List<Object> uniqueCategories = rows.collect { it[xCol] }.unique()
        categoryPositions = [:]
        uniqueCategories.eachWithIndex { Object cat, int idx ->
          categoryPositions[cat] = idx as BigDecimal
        }
      }
    }

    // Ensure xmin/xmax exist (use widthParam if provided, otherwise default to x).
    BigDecimal defaultWidth = widthParam ?: 0.9
    rows.each { Map<String, Object> row ->
      if (!row.containsKey('xmin') || !row.containsKey('xmax')) {
        def xVal = row[xCol]
        BigDecimal numericX
        if (xVal instanceof Number) {
          numericX = xVal as BigDecimal
        } else if (categoryPositions != null && categoryPositions.containsKey(xVal)) {
          numericX = categoryPositions[xVal]
        } else {
          // Cannot determine numeric position, skip
          return
        }
        BigDecimal half = defaultWidth / 2
        row['xmin'] = numericX - half
        row['xmax'] = numericX + half
      }
    }

    // Sort by x and group for stable dodging of overlapping intervals.
    Map<Object, BigDecimal> catPos = categoryPositions  // capture for closure
    rows.sort { a, b ->
      BigDecimal ax
      BigDecimal bx
      if (a[xCol] instanceof Number) {
        ax = a[xCol] as BigDecimal
      } else if (catPos != null && catPos.containsKey(a[xCol])) {
        ax = catPos[a[xCol]]
      } else {
        ax = 0
      }
      if (b[xCol] instanceof Number) {
        bx = b[xCol] as BigDecimal
      } else if (catPos != null && catPos.containsKey(b[xCol])) {
        bx = catPos[b[xCol]]
      } else {
        bx = 0
      }
      if (ax != bx) {
        return ax <=> bx
      }
      if (groupCol != null) {
        def ag = a[groupCol]
        def bg = b[groupCol]
        if (ag instanceof Number && bg instanceof Number) {
          return reverse ? (bg <=> ag) : (ag <=> bg)
        }
        if (ag != null && bg != null) {
          return reverse ? (bg.toString() <=> ag.toString()) : (ag.toString() <=> bg.toString())
        }
      }
      return 0
    }

    // Identify overlapping x-intervals and assign group ids.
    List<Integer> xids = findXOverlaps(rows)
    Map<Integer, List<Map<String, Object>>> byXid = [:].withDefault { [] }
    rows.eachWithIndex { Map<String, Object> row, int idx ->
      byXid[xids[idx]] << row
    }

    Map<Integer, BigDecimal> newx = [:]
    Map<Integer, BigDecimal> groupSize = [:]
    byXid.each { Integer xid, List<Map<String, Object>> bucket ->
      // Compute the overall center and total width for each overlap group.
      // Filter to rows with valid xmin/xmax values
      List<BigDecimal> xminValues = bucket.findResults { it['xmin'] instanceof Number ? it['xmin'] as BigDecimal : null } as List<BigDecimal>
      List<BigDecimal> xmaxValues = bucket.findResults { it['xmax'] instanceof Number ? it['xmax'] as BigDecimal : null } as List<BigDecimal>
      if (xminValues.isEmpty() || xmaxValues.isEmpty()) {
        // No valid bounds in this bucket, skip processing
        groupSize[xid] = BigDecimal.ZERO
        return
      }
      BigDecimal minX = xminValues.min()
      BigDecimal maxX = xmaxValues.max()
      BigDecimal center = (minX + maxX) / 2
      newx[xid] = center
      BigDecimal total = 0
      int n = bucket.size()
      // Divide each element's width by the number of overlapping elements.
      // This matches ggplot2's position_dodge2 with preserve="total" (the default),
      // which keeps the total occupied width constant regardless of original widths.
      bucket.each { Map<String, Object> row ->
        BigDecimal xminVal = row['xmin'] instanceof Number ? row['xmin'] as BigDecimal : 0
        BigDecimal xmaxVal = row['xmax'] instanceof Number ? row['xmax'] as BigDecimal : 0
        BigDecimal width = xmaxVal - xminVal
        BigDecimal newWidth = n > 0 ? width / n : 0
        row['new_width'] = newWidth
        total += newWidth
      }
      groupSize[xid] = total
    }

    byXid.each { Integer xid, List<Map<String, Object>> bucket ->
      // Lay out boxes left-to-right within each overlap group.
      BigDecimal centerX = newx[xid]
      BigDecimal size = groupSize[xid]
      if (centerX == null || size == null) {
        // Skip buckets that weren't processed due to missing values
        return
      }
      BigDecimal start = centerX - (size / 2)
      BigDecimal cursor = start
      bucket.each { Map<String, Object> row ->
        BigDecimal newWidth = row['new_width'] instanceof Number ? row['new_width'] as BigDecimal : 0
        row['xmin'] = cursor
        row['xmax'] = cursor + newWidth
        row['x'] = (cursor + cursor + newWidth) / 2
        cursor += newWidth
      }
    }

    if (byXid.any { it.value.size() > 1 }) {
      // Apply padding by shrinking each box around its center.
      rows.each { Map<String, Object> row ->
        if (!(row['new_width'] instanceof Number) || !(row['x'] instanceof Number)) {
          return
        }
        BigDecimal newWidth = row['new_width'] as BigDecimal
        BigDecimal padWidth = newWidth * (1 - padding)
        BigDecimal center = row['x'] as BigDecimal
        row['xmin'] = center - padWidth / 2
        row['xmax'] = center + padWidth / 2
      }
    }

    // Clean up intermediate columns used during width calculations.
    rows.each { row ->
      row.remove('new_width')
    }

    return Matrix.builder().mapList(rows).build()
  }

  /**
   * Find overlapping x intervals and assign group ids.
   */
  private static List<Integer> findXOverlaps(List<Map<String, Object>> rows) {
    int n = rows.size()
    if (n == 0) return []

    List<BigDecimal> start = new ArrayList<>(n)
    List<BigDecimal> end = new ArrayList<>(n)
    List<Boolean> nonzero = new ArrayList<>(n)
    List<Boolean> missing = new ArrayList<>(n)
    rows.each { Map<String, Object> row ->
      BigDecimal xmin = toBigDecimal(row['xmin'])
      BigDecimal xmax = toBigDecimal(row['xmax'])
      start << xmin
      end << xmax
      boolean isMissing = isInvalidForNumericPositioning(xmin) || isInvalidForNumericPositioning(xmax)
      missing << isMissing
      nonzero << (!isMissing && xmin != null && xmax != null && xmin != xmax)
    }

    // Fill missing bounds so overlap detection stays consistent across NA stretches.
    start = forwardBackwardFill(start)
    end = forwardBackwardFill(end)

    // Track the running maximum of previous interval ends to detect new overlap groups.
    List<BigDecimal> endShift = new ArrayList<>(n)
    if (n > 0) {
      endShift << end[0]
      for (int i = 0; i < n - 1; i++) {
        endShift << end[i]
      }
    }
    List<BigDecimal> endPrev = new ArrayList<>(n)
    BigDecimal currentMax = null
    endShift.each { BigDecimal value ->
      if (currentMax == null) {
        currentMax = value
      } else if (value != null) {
        currentMax = currentMax.max(value)
      }
      endPrev << currentMax
    }

    List<Integer> overlaps = new ArrayList<>(n)
    int currentGroupId = 0
    for (int i = 0; i < n; i++) {
      BigDecimal s = start[i]
      BigDecimal ePrev = endPrev[i]
      boolean nz = nonzero[i]
      boolean newGroup = false
      if (s != null && ePrev != null) {
        if (s > ePrev || (s == ePrev && nz)) {
          newGroup = true
        }
      }
      if (newGroup) {
        currentGroupId += 1
      }
      overlaps << currentGroupId
    }

    // Keep missing intervals in their own groups so they don't affect valid ranges.
    int maxOverlap = overlaps.isEmpty() ? 0 : overlaps.max()
    int missingIndex = 0
    for (int i = 0; i < n; i++) {
      if (missing[i]) {
        missingIndex += 1
        overlaps[i] = maxOverlap + missingIndex
      }
    }

    // Re-map group ids into a compact 1..k sequence.
    Map<Integer, Integer> mapping = [:]
    int next = 1
    List<Integer> result = new ArrayList<>(n)
    overlaps.each { Integer val ->
      if (!mapping.containsKey(val)) {
        mapping[val] = next++
      }
      result << mapping[val]
    }
    return result
  }

  /**
   * Check if a value is invalid for numeric positioning.
   * Returns true for null, NaN, or non-Number types (e.g., String).
   * This is used to identify values that cannot participate in numeric position calculations.
   */
  private static boolean isInvalidForNumericPositioning(Object value) {
    if (value == null) {
      return true
    }
    if (value instanceof Number) {
      BigDecimal v = value as BigDecimal
      return v == null
    }
    return true
  }

  private static BigDecimal toBigDecimal(Object value) {
    if (value instanceof Number) {
      try {
        return value as BigDecimal
      } catch (Exception e) {
        return null
      }
    }
    return null
  }

  /**
   * Fill missing (null) values using bidirectional imputation:
   * first forward-fill (propagate last known value forward),
   * then backward-fill (propagate next known value backward).
   * This ensures all nulls are filled if there's at least one non-null value.
   */
  private static List<BigDecimal> forwardBackwardFill(List<BigDecimal> values) {
    List<BigDecimal> result = new ArrayList<>(values)
    // Forward fill: propagate last known value forward
    BigDecimal last = null
    for (int i = 0; i < result.size(); i++) {
      BigDecimal value = result[i]
      if (value != null) {
        last = value
      } else if (last != null) {
        result[i] = last
      }
    }
    // Backward fill: propagate next known value backward (fills leading nulls)
    BigDecimal next = null
    for (int i = result.size() - 1; i >= 0; i--) {
      BigDecimal value = result[i]
      if (value != null) {
        next = value
      } else if (next != null) {
        result[i] = next
      }
    }
    return result
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
    if (yCol == null && data.columnNames().contains('count')) {
      yCol = 'count'
    }
    String groupCol = aes.groupColName ?: aes.fillColName ?: aes.colorColName

    if (xCol == null || yCol == null) {
      return data
    }

    // Group by x value
    Map<Object, List<Map<String, Object>>> byX = data.rows()
        .groupBy { row -> row[xCol] }
        .collectEntries { xVal, rows ->
          [(xVal): rows.collect { new LinkedHashMap<>(it.toMap()) }]
        } as Map<Object, List<Map<String, Object>>>

    // Stack within each x group
    List<Map<String, Object>> results = []
    byX.each { xVal, rows ->
      List<Map<String, Object>> orderedRows = rows
      if (reverse) {
        orderedRows = new ArrayList<>(rows).reverse()
      }
      BigDecimal cumSum = 0

      orderedRows.each { row ->
        Map<String, Object> newRow = new LinkedHashMap<>(row)
        Object yRaw = row[yCol]
        BigDecimal yVal = yRaw instanceof Number ? yRaw as BigDecimal : 0
        newRow['ymin'] = cumSum
        cumSum += yVal
        newRow['ymax'] = cumSum
        BigDecimal yMin = newRow['ymin'] as BigDecimal
        BigDecimal yMax = newRow['ymax'] as BigDecimal
        newRow['y'] = (yMin + yMax) / 2  // Center of bar
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
    Map<Object, BigDecimal> maxByX = new LinkedHashMap<>()
    stacked.each { Row row ->
      Object xVal = row[xCol]
      Object yMaxRaw = row['ymax']
      BigDecimal ymax = yMaxRaw instanceof Number ? yMaxRaw as BigDecimal : 0
      if (!maxByX.containsKey(xVal) || ymax > maxByX[xVal]) {
        maxByX[xVal] = ymax
      }
    }

    // Normalize
    List<Map<String, Object>> results = []
    stacked.each { Row row ->
      Map<String, Object> newRow = new LinkedHashMap<>(row.toMap())
      Object xVal = row[xCol]
      BigDecimal total = maxByX[xVal] ?: 1

      if (total > 0) {
        BigDecimal yMin = (newRow['ymin'] as BigDecimal) / total
        BigDecimal yMax = (newRow['ymax'] as BigDecimal) / total
        newRow['ymin'] = yMin
        newRow['ymax'] = yMax
        newRow['y'] = (yMin + yMax) / 2
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
    BigDecimal width = params.width as BigDecimal ?: 0.4
    BigDecimal height = params.height as BigDecimal ?: 0.4
    Long seed = params.seed as Long

    Random random = seed != null ? new Random(seed) : new Random()

    String xCol = aes.xColName
    String yCol = aes.yColName

    List<Map<String, Object>> results = []
    data.each { Row row ->
      Map<String, Object> newRow = new LinkedHashMap<>(row.toMap())

      if (xCol != null && newRow[xCol] instanceof Number) {
        BigDecimal jitterX = (random.nextDouble() - 0.5) * width
        newRow[xCol] = (newRow[xCol] as BigDecimal) + jitterX
      }

      if (yCol != null && newRow[yCol] instanceof Number) {
        BigDecimal jitterY = (random.nextDouble() - 0.5) * height
        newRow[yCol] = (newRow[yCol] as BigDecimal) + jitterY
      }

      results << newRow
    }

    return Matrix.builder().mapList(results).build()
  }

  /**
   * Nudge position - add a fixed offset to x and/or y.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings
   * @param params Map with optional: x (x nudge), y (y nudge)
   * @return Matrix with nudged positions
   */
  static Matrix nudge(Matrix data, Aes aes, Map params = [:]) {
    BigDecimal nudgeX = params.x instanceof Number ? params.x as BigDecimal : 0
    BigDecimal nudgeY = params.y instanceof Number ? params.y as BigDecimal : 0

    String xCol = aes.xColName
    if (xCol == null && data.columnNames().contains('x')) {
      xCol = 'x'
    }
    String yCol = aes.yColName
    if (yCol == null && data.columnNames().contains('y')) {
      yCol = 'y'
    }

    if (nudgeX == 0 && nudgeY == 0) {
      return data
    }

    List<Map<String, Object>> results = []
    data.each { Row row ->
      Map<String, Object> newRow = new LinkedHashMap<>(row.toMap())

      if (xCol != null && newRow[xCol] instanceof Number) {
        newRow[xCol] = (newRow[xCol] as BigDecimal) + nudgeX
      }
      if (yCol != null && newRow[yCol] instanceof Number) {
        newRow[yCol] = (newRow[yCol] as BigDecimal) + nudgeY
      }
      results << newRow
    }

    return Matrix.builder().mapList(results).build()
  }
}
