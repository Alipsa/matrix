package se.alipsa.matrix

import static se.alipsa.matrix.ValueConverter.*

class Matrix {

    static BigDecimal sum(List<List<?>> matrix, int colNum) {
        def s = 0g
        def value
        for (row in matrix) {
            value = row[colNum]
            if (value instanceof Number) {
                s = s + value
            }
        }
        return s
    }

    static List<List<?>> cast(List<List<?>> matrix, int colNum, Class<? extends Number> type) {
        def m = clone(matrix)
        def value
        if (type == BigDecimal.class) {
            for (int r = 0; r < m.size(); r ++) {
                value = m[r][colNum]
                if (value == null) continue
                m[r][colNum] = toBigDecimal(value)
            }
        } else if (type == Double.class) {
            for (int r = 0; r < m.size(); r ++) {
                value = m[r][colNum]
                if (value == null) continue
                m[r][colNum] = toDouble(value)
            }
        } else {
            throw new RuntimeException("Only cast to BigDecimal and Double are implemented")
        }
        return m
    }

    static List<List<?>> cast(List<List<?>> matrix, int colNum, Closure converter) {
        def m = clone(matrix)
        def value
        for (int r = 0; r < m.size(); r ++) {
            value = m[r][colNum]
            if (value == null) continue
            m[r][colNum] = converter.call(value)
        }
        return m
    }

    static List<List<?>> clone(List<List<?>> matrix) {
        List<List<?>> copy = new ArrayList<>(matrix.size())
        for (row in matrix) {
            copy.add(row.collect())
        }
        return copy
    }

    static List<List<?>> transpose(List<List<?>> matrix) {
        if (matrix == null) return null
        return matrix.transpose()
    }

    static boolean isValid(Object matrix) {
        if (matrix == null) {
            return false
        }
        if (!List.isInstance(matrix)) {
            return false
        }
        int numCols
        int prevNumCols = -1
        List list = matrix as List
        for (row in list) {
            if (!List.isInstance(row)) {
                return false
            }
            numCols = (row as List).size()
            if (prevNumCols == -1) {
                prevNumCols = numCols
            }
            if (numCols != prevNumCols) {
                return false
            }
            prevNumCols = numCols
        }
        return true
    }
}
