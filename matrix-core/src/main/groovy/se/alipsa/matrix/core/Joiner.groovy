package se.alipsa.matrix.core

/**
 * Join operations for combining matrices on one or more key columns.
 *
 * <p>The current implementation provides hash-based merge helpers for inner and
 * left joins while preserving the column order expected by existing callers.</p>
 */
class Joiner {

    /**
     * Merges two matrices on specified columns using a hash-based join algorithm.
     * Complexity is O(n + m) where n = x.rowCount() and m = y.rowCount().
     *
     * @param x the Matrix to start from
     * @param y the Matrix to join to x
     * @param by a map of the x and y columns to join on e.g. [x:'id', y: 'GUID']
     * @param all true for a left join, false for an inner join
     * @return a new matrix with the two ones joined
     */
    static Matrix merge(Matrix x, Matrix y, Map<String, String> by, boolean all = false) {
        def xColIndex = x.columnIndex(by.x)
        def yColIndex = y.columnIndex(by.y)
        if (xColIndex < 0) {
            throw new IllegalArgumentException("Join column '${by.x}' does not exist in matrix '${x.matrixName}'")
        }
        if (yColIndex < 0) {
            throw new IllegalArgumentException("Join column '${by.y}' does not exist in matrix '${y.matrixName}'")
        }
        def nYcol = y.columnCount()

        // Build a hash map from y indexed by the join column for O(1) lookup
        Map<Object, List> yIndex = [:]
        y.each { List yRow ->
            def key = yRow[yColIndex]
            if (!yIndex.containsKey(key)) {
                yIndex[key] = yRow
            }
            // Note: only keep first match per key (matches original behavior)
        }

        // Join x with y using the hash map
        def resultRows = []
        x.each { List row ->
            def key = row[xColIndex]
            List yRow = yIndex[key]
            if (yRow != null) {
                def yr = yRow.collect()
                yr.remove(yColIndex)
                resultRows << [*row, *yr]
            } else if (all) {
                // append nulls when we are doing a left join and we have no match
                resultRows << [*row, *([null] * (nYcol - 1))]
            }
        }

        def yColNames = y.columnNames().collect()
        yColNames.remove(yColIndex)
        return Matrix.builder().matrixName(x.matrixName).columnNames([*x.columnNames(), *yColNames]).rows(resultRows).build()
    }

    /**
     *
     * @param x the Matrix to start from
     * @param y the Matrix to join to x
     * @param by the column to join on
     * @param all true for a left join, false for an inner join
     * @return a new matrix with the two ones joined
     */
    static Matrix merge(Matrix x, Matrix y, String by, boolean all = false) {
        return merge(x, y, [x: by, y: by], all)
    }
}
