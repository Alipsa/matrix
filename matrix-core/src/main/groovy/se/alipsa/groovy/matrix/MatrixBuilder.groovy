package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic
import se.alipsa.groovy.matrix.util.ClassUtils

import java.nio.file.Files
import java.sql.ResultSet
import java.sql.ResultSetMetaData

@CompileStatic
class MatrixBuilder {

  String name
  List<String> headerList
  List<List<?>> columns
  List<Class<?>> dataTypes

  Matrix build() {
    new Matrix(name, headerList, columns, dataTypes)
  }

  MatrixBuilder name(String name) {
    this.name = name
    this
  }

  MatrixBuilder columnNames(List<String> columnNames) {
    headerList = columnNames
    this
  }

  MatrixBuilder columnNames(String... colNames) {
    columnNames(colNames as List<String>)
  }

  /**
   * Populate the data of the Matrix. Use only one of rows, columns or data methods i.e.
   * either provide the rows (observations) or the columns (variables) or one of the data methods
   * if you use multiple ones, only the last one will be used.
   *
   * @param columns a list of variables
   * @return this builder
   */
  MatrixBuilder columns(List<List<?>> columns) {
    this.columns = columns
    this
  }

  MatrixBuilder columns(List<?>... columns) {
    this.columns = columns as List<List<?>>
    this
  }

  MatrixBuilder columns(Map<String, List<?>> columData) {
    List<String> headers = []
    List<List<?>> cols = []
    columData.each {k, v ->
      headers << String.valueOf(k)
      cols << v.collect()
    }
    if(noColumnNames()) {
      columnNames(headers)
    }
    if(noColumns()) {
      columns(cols)
    }
    this
  }

  MatrixBuilder data(Map<String, List<?>> columData) {
    columns(columData)
  }

  /**
   * Populate the data of the Matrix. Use only one of rows, columns or data methods i.e.
   * either provide the rows (observations) or the columns (variables) or one of the data methods
   * if you use multiple ones, only the ast one will be used.
   *
   * @param rows a list of observations
   * @return this builder
   */
  MatrixBuilder rows(List<List<?>> rows) {
    this.columns = rows.transpose()
    this
  }

  MatrixBuilder data(Grid grid) {
    rows(grid.data)
  }

  MatrixBuilder data(File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    data(Files.newInputStream(file.toPath()), delimiter, stringQuote, firstRowAsHeader)
    if(noName()) {
      name(file.name.substring(0, file.name.lastIndexOf('.')))
    }
    this
  }

  MatrixBuilder data(URL url, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    try(InputStream inputStream = url.openStream()) {
      String n = url.getFile() == null ? url.getPath() : url.getFile()
      if (n.contains('/')) {
        n = n.substring(n.lastIndexOf('/') + 1, n.length())
      }
      if (n.contains('.')) {
        n = n.substring(0, n.lastIndexOf('.'))
      }
      data(inputStream, delimiter, stringQuote, firstRowAsHeader)
      if(noName()) {
        name(n)
      }
    }
  }

  MatrixBuilder data(InputStream inputStream, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    try(InputStreamReader reader = new InputStreamReader(inputStream)) {
      List<List<String>> data = []
      final boolean stripQuotes = stringQuote != ''
      for (line in reader.readLines()) {
        List<String> row = []
        for (val in line.split(delimiter)) {
          if (stripQuotes) {
            row.add(val.replaceAll(~/^$stringQuote|$stringQuote$/, '').trim())
          } else {
            row.add(val.trim())
          }
        }
        data.add(row)
      }
      List<String> headerNames

      if (firstRowAsHeader) {
        headerNames = data[0] as List<String>
        data = data[1..data.size() - 1]
      } else {
        headerNames = []
      }
      if (noColumnNames()) {
        columnNames(headerNames)
      }
      rows(data)
      if(noDataTypes()) {
        dataTypes([String] * headerNames.size())
      }
    }
    this
  }

  MatrixBuilder data(ResultSet rs) {
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
    if (noColumnNames()) {
      columnNames(headers)
    }
    if(noDataTypes()) {
      dataTypes(columnTypes)
    }
    List<List<Object>> rows = []
    while (rs.next()) {
      List<?> row = []
      for (int i = 1; i <= ncols; i++) {
        row.add(rs.getObject(i))
      }
      rows.add(row)
    }
    columns(rows.transpose())
  }


  MatrixBuilder dataTypes(List<Class<?>> types) {
    this.dataTypes = ClassUtils.convertPrimitivesToWrapper(types)
    this
  }

  MatrixBuilder dataTypes(Class<?>... types) {
    dataTypes(types as List<Class<?>>)
  }

  MatrixBuilder dataTypes(List<Class<?>>... types) {
    if (types.length > 0) {
      this.dataTypes = types[0]
    }
    this
  }

  private boolean noColumnNames() {
    headerList == null || headerList.isEmpty()
  }

  private boolean noColumns() {
    columns == null || columns.isEmpty()
  }

  private boolean noDataTypes() {
    dataTypes == null || dataTypes.isEmpty()
  }

  private boolean noName() {
    name == null || name.isBlank()
  }

}
