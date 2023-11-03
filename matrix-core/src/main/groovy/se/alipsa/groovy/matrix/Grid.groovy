package se.alipsa.groovy.matrix

import java.text.NumberFormat

// TODO: Consider making grids uniformly Typed e.g. Grid<Double> = new Grid<>()
//   or perhaps making another class (TypedGrid for that),
//   maybe even a NumberGrid where the type is <T extends Number>
class Grid {

    List<List<?>> data

    Grid() {
        data = new ArrayList<>()
    }

    Grid(List<List<?>> data) {
        if (isValid(data)) {
            this.data = data
        } else if (data instanceof List) {
            this.data = [data]
        } else {
            throw new IllegalArgumentException("data is invalid")
        }
    }

    Grid(int nrow) {
        data = new ArrayList<>(nrow)
    }

    Grid(int nrow, int ncol) {
        data = new ArrayList<>(nrow)
        for (row in 1..nrow) {
            data << new ArrayList<>(ncol)
        }
    }

    Grid(Number value, int nrow, int ncol) {
        data = new ArrayList<>(nrow)
        for (row in 1..nrow) {
            data << [value]*ncol
        }
    }

    List getAt(int row) {
        return data[row]
    }

    def getAt(int row, int column) {
        return data[row][column]
    }

    def leftShift(List row) {
        data << row
    }

    boolean add(List<?> row) {
        data.add(row)
    }

    boolean addAll(List<List<?>> grid) {
        data.addAll(grid)
    }

    Grid plus(List<?> row) {
        def grid = new Grid()
        grid.addAll(data)
        grid.add(row)
        return grid
    }

    String plus(String str) {
        data.toString() + str
    }

    void putAt(List<Integer> rowColumn, Object value) {
        def row = data.get(rowColumn[0])
        Integer column = rowColumn[1]
        row.set(column, value)
    }

    String toString() {
        data.toString()
    }

    /**
     *
     * @return the list of rows in the grid
     * Mutable, i.e. changes to the result is reflected in the Grid
     */
    List<List<?>> getData() {
        return data
    }

    /**
     *
     * @return the list of rows in the grid
     * Mutable, i.e. changes to the result is reflected in the Grid
     */
    List<List<Object>> getRowList() {
        return data
    }

    Grid replaceRow(int index, List<?> row) {
        def r = data.get(index)
        r.clear()
        r.addAll(row)
        return this
    }

    Iterator<List<?>> iterator() {
        return data.iterator()
    }

    static Grid convert(Grid grid, Integer colNum, Class<? extends Number> type, NumberFormat format = null) {
        return new Grid(convert(grid.data, [colNum], type, format))
    }

    static List<List<?>> convert(List<List<?>> rowList, Integer colNum, Class<? extends Number> type, NumberFormat format = null) {
        if (colNum == null) return null
        def m = clone(rowList)
        def value
        for (int r = 0; r < m.size(); r++) {
            value = m[r][colNum]
            if (value == null) continue
            m[r][colNum] = ValueConverter.convert(value, type, null, format)
        }
        return m
    }

    static Grid convert(Grid grid, List<Integer> colNums, Class<? extends Number> type, NumberFormat format = null) {
        return new Grid(convert(grid.data, colNums, type, format))
    }

    static List<List<?>> convert(List<List<?>> rowList, List<Integer> colNums, Class<? extends Number> type, NumberFormat format = null) {
        def m = clone(rowList)
        def value
        for (int r = 0; r < m.size(); r++) {
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

    static Grid convert(Grid grid, int colNum, Closure converter) {
        return new Grid(convert(grid.data, colNum, converter))
    }

    static List<List<?>> convert(List<List<?>> rowList, int colNum, Closure converter) {
        def m = clone(rowList)
        def value
        for (int r = 0; r < m.size(); r++) {
            value = m[r][colNum]
            if (value == null) continue
            m[r][colNum] = converter.call(value)
        }
        return m
    }

    static List<List<?>> clone(List<List<?>> rowList) {
        List<List<?>> copy = new ArrayList<>(rowList.size())
        for (row in rowList) {
            copy.add(row.collect())
        }
        return copy
    }

    static Grid transpose(Grid grid) {
        return new Grid(transpose(grid.data))
    }

    static List<List<?>> transpose(List<List<?>> rowList) {
        if (rowList == null) return null
        return rowList.transpose()
    }

    static boolean isValid(Grid grid) {
        return isValid(grid.data)
    }

    static boolean isValid(Object rowList) {
        if (rowList == null) {
            return false
        }
        if (!List.isInstance(rowList)) {
            return false
        }
        int numCols
        int prevNumCols = -1
        List list = rowList as List
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
