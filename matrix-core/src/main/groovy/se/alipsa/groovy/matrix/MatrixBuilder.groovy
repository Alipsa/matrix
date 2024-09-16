package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic
import se.alipsa.groovy.matrix.util.ClassUtils

import java.nio.file.Files
import java.nio.file.Path
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

  /**
   * The cvs parsing is fast but rather primitive. For a much more thorough solution, use the CsvImporter in the
   * matrix-csv library.
   *
   * @param file
   * @param delimiter
   * @param stringQuote
   * @param firstRowAsHeader
   * @return
   */
  MatrixBuilder data(File file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    data(Files.newInputStream(file.toPath()), delimiter, stringQuote, firstRowAsHeader)
    if(noName()) {
      int endIdx = file.name.length()
      if (file.name.contains('.')) {
        endIdx = file.name.lastIndexOf('.')
      }
      name(file.name.substring(0, endIdx))
    }
    this
  }

  /**
   * The cvs parsing is fast but rather primitive. For a much more thorough solution, use the CsvImporter in the
   * matrix-csv library.
   *
   * @param file
   * @param delimiter
   * @param stringQuote
   * @param firstRowAsHeader
   * @return
   */
  MatrixBuilder data(Path file, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    data(Files.newInputStream(file), delimiter, stringQuote, firstRowAsHeader)
    if(noName()) {
      String fileName = file.getFileName().toString()
      int endIdx = fileName.length()
      if (file.contains('.')) {
        endIdx = fileName.lastIndexOf('.')
      }
      name(fileName.substring(0, endIdx))
    }
    this
  }

  /**
   * The cvs parsing is fast but rather primitive. For a much more thorough solution, use the CsvImporter in the
   * matrix-csv library.
   *
   * @param url
   * @param delimiter
   * @param stringQuote
   * @param firstRowAsHeader
   * @return
   */
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
    this
  }

  /**
   * The cvs parsing is fast but rather primitive. For a much more thorough solution, use the CsvImporter in the
   * matrix-csv library.
   *
   * @param inputStream
   * @param delimiter
   * @param stringQuote
   * @param firstRowAsHeader
   * @return
   */
  MatrixBuilder data(InputStream inputStream, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    try(InputStreamReader reader = new InputStreamReader(inputStream)) {
      List<List<String>> data = []
      final boolean stripQuotes = stringQuote != ''
      int maxCols = 0
      for (line in reader.readLines()) {
        List<String> row = []
        // if there are empty columns in the end, split does not take those into account unless we set the limit to -1
        def values = line.split(delimiter, -1)
        maxCols = Math.max(maxCols, values.length)
        for (val in values) {
          String value
          if (stripQuotes) {
            value = (val.replaceAll(~/^$stringQuote|$stringQuote$/, '').trim())
          } else {
            value = val.trim()
          }
          row.add(value)
        }
        data.add(row)
      }
      List<String> headerNames

      if (firstRowAsHeader) {
        headerNames = data.remove(0)
      } else {
        headerNames = (1..maxCols).collect {'c' + it}
      }
      if (noColumnNames()) {
        columnNames(headerNames)
      }
      rows(data)
      if(noDataTypes()) {
        types([String] * headerNames.size())
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
      types(columnTypes)
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


  MatrixBuilder types(List<Class<?>> types) {
    this.dataTypes = ClassUtils.convertPrimitivesToWrapper(types)
    this
  }

  MatrixBuilder types(Class<?>... dataTypes) {
    types(dataTypes as List<Class<?>>)
  }

  MatrixBuilder types(List<Class<?>>... types) {
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
