package se.alipsa.matrix

import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.text.DecimalFormat
import java.text.NumberFormat

/**
 * This is essentially a [][] (List<List<?>>) but also has a header
 * and several convenience methods to work with it.
 */
class DataTable {
    private NumberFormat numberFormat = DecimalFormat.getInstance()

    private List<String> headerList
    private List<List<?>> rowList
    private List<Class<?>> columnTypes
    // Copy of the data i column format
    private List<List<Object>> columnList;

    static DataTable create(List<String> headerList, List<List<?>> rowList, List<Class<?>>... dataTypesOpt) {
        DataTable table = new DataTable();
        table.headerList = Collections.unmodifiableList(headerList.collect())
        table.rowList = Collections.unmodifiableList(rowList.collect())
        if (dataTypesOpt.length > 0) {
            table.columnTypes = Collections.unmodifiableList(dataTypesOpt[0])
            if (headerList.size() != table.columnTypes.size()) {
                throw new IllegalArgumentException("Number of columns ({$headerList.size()}) differs from number of datatypes provided (${table.columnType.size()})")
            }
        } else {
            table.columnTypes = Collections.unmodifiableList([Object.class] * headerList.size())
        }
        table.createColumnList()
        return table
    }

    static DataTable create(List<List<?>> rowList, List<Class<?>>... dataTypesOpt) {
        def headerList = []
        for (int i = 0; i < rowList[0].size(); i++) {
            headerList.add(String.valueOf(i))
        }
        return create(headerList, rowList, dataTypesOpt)
    }

    static DataTable create(Map<String, List<?>> map, List<Class<?>>... dataTypesOpt) {
        List<String> header = map.keySet().collect()
        List<?> rows = map.values().collect()
        return create(header, rows, dataTypesOpt)
    }

    static DataTable create(ResultSet rs) {
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
        DataTable table = new DataTable()
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

    private DataTable() {}

    List<String> getColumnNames() {
        return headerList
    }

    List<?> getColumn(String columnName) {
        return columnList.get(headerList.indexOf(columnName))
    }

    List<?> getColumn(int index) {
        return columnList.get(index);
    }

    List<?> getRow(int index) {
        return rowList.get(index)
    }

    List<List<?>> getMatrix() {
        return rowList;
    }

    Class<?> getColumnType(int i) {
        return columnTypes[i]
    }

    Class<?> getColumnType(String columnName) {
        return columnTypes[headerList.indexOf(columnName)]
    }


    DataTable transpose()  {
        return create(headerList, columnList, columnTypes);
    }

    int columnCount() {
        return headerList.size()
    }

    int rowCount() {
        return rowList.size()
    }

    Object get(int row, int column) {
        return rowList.get(row).get(column)
    }

    private void createColumnList() {
        List<List<Object>> columns = new ArrayList<>();
        for (List<Object> row : rowList) {
            if (columns.size() == 0) {
                for (int i = 0; i < row.size(); i++) {
                    columns.add(new ArrayList<>());
                }
            }
            for (int j = 0; j < row.size(); j++) {
                columns.get(j).add(row.get(j));
            }
        }
        columnList = Collections.unmodifiableList(columns);
    }

    /**
     * Set the decimal formatter to use in convenience methods getting data as double or float.
     * Once set, the decimal formatter is no longer mutable i.e. a change to the decimal formatter
     * after it has been set in the table does not affect the decimal formatter in the table.
     *
     * @param numberFormat the {@link java.text.NumberFormat} to use when converting Strings to Float and Double
     */
    void setNumberFormat(NumberFormat numberFormat) {
        this.numberFormat = (NumberFormat)numberFormat.clone();
    }
}
