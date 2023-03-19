package se.alipsa.groovy.matrix

import java.text.NumberFormat

class Matrix {

    static List<List<?>> convert(List<List<?>> matrix, Integer colNum, Class<? extends Number> type, NumberFormat format = null) {
        return convert(matrix, [colNum], type, format)
    }

    static List<List<?>> convert(List<List<?>> matrix, List<Integer> colNums, Class<? extends Number> type, NumberFormat format = null) {
        def m = clone(matrix)
        def value
        for (int r = 0; r < m.size(); r ++) {
            for (int c in colNums) {
                value = m[r][c]
                if (value == null) continue
                if (format == null) {
                    m[r][c] = ValueConverter.convert(value, type)
                } else {
                    m[r][c] = ValueConverter.convert(value, type, null, format)
                }
            }
        }
        return m
    }

    static List<List<?>> convert(List<List<?>> matrix, int colNum, Class<? extends Number> type, NumberFormat format = null) {
        def m = clone(matrix)
        def value
        for (int r = 0; r < m.size(); r ++) {
            value = m[r][colNum]
            if (value == null) continue
            m[r][colNum] = ValueConverter.convert(value, type, null, format)
        }
        return m
    }

    static List<List<?>> convert(List<List<?>> matrix, int colNum, Closure converter) {
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
