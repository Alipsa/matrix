package se.alipsa.groovy.matrix

class Joiner {

    /**
     * Note: The implementation is embarrassingly crude and will be terrible for large matrices
     *
     * @param x the Matrix to start from
     * @param y the Matrix to join to x
     * @param by a map of the x and y columns to join on e.g. [x:'id', y: 'GUID']
     * @param all true for a left join, false for an inner join
     * @return a new matrix with the two ones joined
     */
    // TODO: investigate if it is possible to use GINQ for this
    static Matrix merge(Matrix x, Matrix y, Map<String, String> by, boolean all = false) {
        def xColIndex = x.columnIndex(by.x)
        def yColIndex = y.columnIndex(by.y)
        def resultRows = []
        def nYcol = y.columnCount()
        x.each{ List row->
            boolean added = false
            // maybe it would be faster to copy y and then remove the rows that are found so the
            // search space gets smaller for each hit
            for (List yRow in y) {
                if (row[xColIndex] == yRow[yColIndex]) {
                    def yr = yRow.collect()
                    yr.remove(yColIndex)
                    resultRows << [*row, *yr]
                    added = true
                    break
                }
            }
            if (!added && all) {
                // append nulls when we are doing a left join and we have no match
                resultRows << [*row, *([null]*(nYcol-1))]
            }
        }
        def yColNames = y.columnNames().collect()
        yColNames.remove(yColIndex)
        return Matrix.create(x.name, [*x.columnNames(), *yColNames], resultRows)
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
