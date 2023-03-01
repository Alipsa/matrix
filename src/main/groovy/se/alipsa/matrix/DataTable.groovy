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
    // Copy of the data i column format
    private List<List<Object>> columnList;

    static DataTable create(List<String> headerList, List<List<?>> rowList) {
        DataTable table = new DataTable();
        table.headerList = Collections.unmodifiableList(headerList.collect())
        table.rowList = Collections.unmodifiableList(rowList.collect())
        table.createColumnList()
        return table
    }

    static DataTable create(List<List<?>> rowList) {
        def headerList = []
        for (int i = 0; i < rowList[0].size(); i++) {
            headerList.add(String.valueOf(i))
        }
        return create(headerList, rowList)
    }

    static DataTable create(Map<String, List<?>> map) {
        List<String> header = map.keySet().collect()
        List<?> rows = map.values().collect()
        return create(header, rows)
    }

    static DataTable create(ResultSet rs) {
        ResultSetMetaData rsmd = rs.getMetaData()
        int ncols = rsmd.getColumnCount()
        List<String> headers = []
        for (int i = 1; i <= ncols; i++) {
            headers.add(rsmd.getColumnName(i))
        }
        DataTable table = new DataTable()
        table.headerList = Collections.unmodifiableList(headers)

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


    DataTable transpose()  {
        return create(headerList, columnList);
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


}
