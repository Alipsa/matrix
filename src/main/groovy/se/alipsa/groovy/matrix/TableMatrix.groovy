package se.alipsa.groovy.matrix

import groovyjarjarantlr4.v4.runtime.misc.NotNull

import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

/**
 * This is essentially a [][] (List<List<?>>) but also has a header
 * and several convenience methods to work with it.
 */
class TableMatrix {

    private List<String> headerList
    private List<List<?>> rowList
    private List<Class<?>> columnTypes
    // Copy of the data i column format
    private List<List<Object>> columnList

    static TableMatrix create(List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt) {
        TableMatrix table = new TableMatrix()
        table.headerList = Collections.unmodifiableList(headerList.collect())
        table.rowList = Collections.unmodifiableList(rowList.collect())
        if (dataTypesOpt.length > 0) {
            table.columnTypes = Collections.unmodifiableList(dataTypesOpt[0])
            if (headerList.size() != table.columnTypes.size()) {
                throw new IllegalArgumentException("Number of columns ({$headerList.size()}) differs from number of datatypes provided (${table.columnTypes.size()})")
            }
        } else {
            table.columnTypes = Collections.unmodifiableList([Object.class] * headerList.size())
        }
        table.createColumnList()
        return table
    }

    static TableMatrix create(File file, String delimiter = ',') {
        def data = file.readLines()*.split(delimiter) as List<List<?>>
        def headerNames = data[0] as List<String>
        def matrix = data[1..data.size()-1]
        create(headerNames, matrix, [String]*headerNames.size())
    }

    static TableMatrix create(List<List<?>> rowList) {
        def headerList = []
        for (int i = 0; i < rowList[0].size(); i++) {
            headerList.add("v$i")
        }
        return create(headerList, rowList)
    }

    static TableMatrix create(Map<String, List<?>> map, List<Class<?>>... dataTypesOpt) {
        List<String> header = map.keySet().collect()
        List<?> columns = map.values().collect()
        return create(header, columns.transpose(), dataTypesOpt)
    }

    static TableMatrix create(ResultSet rs) {
        ResultSetMetaData rsmd = rs.getMetaData()
        int ncols = rsmd.getColumnCount()
        List<String> headers = []
        List<Class<?>> columnTypes = []
        for (int i = 1; i <= ncols; i++) {
            headers.add(rsmd.getColumnName(i))
            try {
                columnTypes.add(Class.forName(rsmd.getColumnClassName(i)))
            } catch (ClassNotFoundException e) {
                System.err.println "Failed to load class ${rsmd.getColumnClassName(i)}, setting type to Object; ${e.toString()}"
                columnTypes.add(Object.class)
            }
        }
        TableMatrix table = new TableMatrix()
        table.headerList = Collections.unmodifiableList(headers)
        table.columnTypes = Collections.unmodifiableList(columnTypes)
        List<List<Object>> rows = []
        while (rs.next()) {
            List<?> row = []
            for (int i = 1; i <= ncols; i++) {
                row.add(rs.getObject(i))
            }
            rows.add(row)
        }
        table.rowList = Collections.unmodifiableList(rows)
        table.createColumnList()
        return table
    }

    private TableMatrix() {}

    List<String> columnNames() {
        return headerList
    }

    List<?> column(String columnName) {
        return columnList.get(headerList.indexOf(columnName))
    }

    List<?> column(int index) {
        return columnList.get(index)
    }

    List<?> row(int index) {
        return rowList.get(index)
    }

    List<List<?>> rows(List<Integer> index) {
        def rows = []
        index.each {
            rows.add(row((int)it))
        }
        return rows
    }

    List<List<?>> matrix() {
        return rowList
    }

    Class<?> columnType(int i) {
        return columnTypes[i]
    }

    Class<?> columnType(String columnName) {
        return columnTypes[headerList.indexOf(columnName)]
    }

    List<String> columnTypeNames() {
        List<String> types = new ArrayList<>()
        columnTypes.each {types.add(it.getSimpleName())}
        return types
    }


    TableMatrix transpose(List<String> header = headerList)  {
        return create(header, columnList, columnTypes)
    }

    int columnCount() {
        return headerList.size()
    }

    int rowCount() {
        return rowList.size()
    }

    Object get(int row, int column) {
        //return rowList.get(row).get(column)
        return rowList[row][column]
    }

    /**
     * Enable the use of square bracket to reference a column, e.g. table[0, 1] for the 2:nd column of the first observation
     * @return the value corresponding to the row and column indexes supplied
     */
    Object getAt(int row, int column) {
        return get(row, column)
    }

    /**
     * Enable the use of square bracket to reference a column, e.g. table[0, "salary"] for the salary for the first observation
     * @return the value corresponding to the row and column name supplied
     */
    Object getAt(int row, String columnName) {
        return get(row, headerList.indexOf(columnName))
    }

    /**
     * Enable the use of square bracket to reference a column, e.g. table["salary"] for the salary column
     * @return the column corresponding to the column name supplied
     */
    List<?> getAt(String columnName) {
        return column(columnName)
    }

    /**
     * Enable the use of square bracket to reference a column, e.g. table[2] for the 3:rd column
     * @return the column corresponding to the column index supplied
     */
    List<?> getAt(int columnIndex) {
        return column(columnIndex)
    }

    private void createColumnList() {
        List<List<Object>> columns = new ArrayList<>()
        for (List<Object> row : rowList) {
            if (columns.size() == 0) {
                for (int i = 0; i < row.size(); i++) {
                    columns.add(new ArrayList<>())
                }
            }
            for (int j = 0; j < row.size(); j++) {
                columns.get(j).add(row.get(j))
            }
        }
        columnList = Collections.unmodifiableList(columns)
    }

    @Override
    String toString() {
        return "${rowCount()} obs * ${columnCount()} vars; " + String.join(", ", headerList)
    }

    String head(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n') {
        StringBuilder sb = new StringBuilder()
        def nRows = Math.min(rows, rowCount())
        if (includeHeader) {
            sb.append(String.join(delimiter, headerList)).append(lineEnding)
        }
        for (int i = 0; i < nRows; i++) {
            def row = ListConverter.convert(row(i), String.class)
            sb.append(String.join(delimiter, row )).append(lineEnding)
        }
        return sb.toString()
    }

    String tail(int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n') {
        StringBuilder sb = new StringBuilder()
        def nRows = Math.min(rows, rowCount())
        if (includeHeader) {
            sb.append(String.join(delimiter, headerList)).append(lineEnding)
        }
        for (int i = 0; i < nRows; i++) {
            def row = ListConverter.convert(row(rowCount() -1 -i), String.class)
            sb.append(String.join(delimiter, row )).append(lineEnding)
        }
        return sb.toString()
    }

    String content( boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n') {
        return head(rowCount(), includeHeader, delimiter, lineEnding)
    }

    List<Integer> columnIndexes(List<String> colNames) {
        List<Integer> colNums = []
        colNames.each {
            colNums.add(headerList.indexOf(it))
        }
        return colNums
    }

    TableMatrix convert(Map<String, Class<?>> columnTypes, DateTimeFormatter dateTimeFormatter = null, NumberFormat numberFormat = null) {
        def convertedColumns = []
        def convertedTypes = []
        for (int i = 0; i < columnCount(); i++) {
            String colName = headerList[i]
            if (columnTypes[colName]) {
                convertedColumns.add(ListConverter.convert(
                        column(i),
                        columnTypes[colName] as Class<Object>,
                        dateTimeFormatter,
                        numberFormat)
                )
                convertedTypes.add(columnTypes[colName])
            } else {
                convertedColumns.add(column(i))
                convertedTypes.add(columnType(i))
            }
        }
        def convertedRows = Matrix.transpose(convertedColumns)
        return create(headerList, convertedRows, convertedTypes)
    }

    TableMatrix convert(String colName, Class<?> type, Closure converter) {
        return convert(columnNames().indexOf(colName), type, converter)
    }

    /**
     *
     * @param colNum the column index for the column to convert
     * @param type the class the column will be converted to
     * @param converter as closure converting each value in the designated column
     * @return a new TableMatrix
     */
    TableMatrix convert(int colNum, Class<?> type, Closure converter) {
        def convertedColumns = []
        def convertedTypes = []
        def col = []
        for (int i = 0; i < columnCount(); i++) {
            if (colNum == i) {
                column(i).each { col.add(converter.call(it)) }
                convertedColumns.add(col)
                convertedTypes.add(type)
            } else {
                convertedColumns.add(column(i))
                convertedTypes.add(columnType(i))
            }
        }
        def convertedRows = Matrix.transpose(convertedColumns)
        return create(headerList, convertedRows, convertedTypes)
    }

    /**
     * def table = TableMatrix.create([
     *      'place': [1, 2, 3],
     *      'firstname': ['Lorena', 'Marianne', 'Lotte'],
     *      'start': ['2021-12-01', '2022-07-10', '2023-05-27']
     *  ],
     *      [int, String, String]
     *  )
     *  // This will select the second and third row and return it in a new TableMatrix
     *  TableMatrix subSet = table.subset('place', { it > 1 })
     * @param columnName the name of the column to use to select matches
     * @param condition a closure containing the condition for what to retain
     * @return new TableMatrix with only the rows that matched the criteria
     */
    TableMatrix subset(@NotNull String columnName, @NotNull Closure condition) {
        def rows = rows(column(columnName).findIndexValues(condition) as List<Integer>)
        return create(headerList, rows, columnTypes)
    }
}
