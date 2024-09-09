package se.alipsa.groovy.matrix

import se.alipsa.groovy.matrix.util.ClassUtils

import java.nio.file.Files
import java.sql.ResultSet
import java.sql.ResultSetMetaData

class MatrixBuilder {

  String name
  List<String> headerList
  List<List<?>> columns
  List<Class<?>> dataTypes

  Matrix build() {
    if (dataTypes != null && headerList == null) {
      headerList = (1..dataTypes.size()).collect{'c' + it}
    }
    if (columns != null && headerList == null) {
      headerList = (1..columns.size()).collect{'c' + it}
    }
    if (headerList != null && dataTypes == null) {
      dataTypes = [Object]*headerList.size()
    }

    if (columns == null) {
      int ncol = headerList == null ? (dataTypes == null ? 0 : dataTypes.size()): headerList.size()
      columns = []
      for(int i = 0; i < ncol; i++) {
        columns << []
      }
    }
    dataTypes = dataTypes ?: []

    if (dataTypes.size() > columns.size()) {
      for (int i = 0; i < dataTypes.size() - columns.size(); i++) {
        columns << []
      }
    }
    //println "Creating a matrix with name: '$name', ${headerList.size()} headers, ${columns.size()} columns, ${dataTypes.size()} types"
    new Matrix(name, headerList ?: [], columns, dataTypes)
  }

  MatrixBuilder name(String name) {
    this.name = name
    this
  }

  MatrixBuilder columnNames(List<String> columnNames) {
    headerList = columnNames
    this
  }

  /**
   * Populate the data of the Matrix. Use only one of rows, columns or data methods i.e.
   * either provide the rows (observations) or the columns (variables) or one of the data methods
   * if you use multiple ones, only the ast one will be used.
   *
   * @param columns a list of variables
   * @return this builder
   */
  MatrixBuilder columns(List<List<?>> columns) {
    this.columns = columns
    this
  }

  MatrixBuilder columns(Map<String, List<?>> columData) {
    def headers = []
    List<List<?>> cols = []
    columData.each {k, v ->
      headers << String.valueOf(k)
      cols << v.collect()
    }
    columnNames(headers)
    columns(cols)
    this
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
    name(file.name.substring(0, file.name.lastIndexOf('.')))
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
      name(n)
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
      columnNames(headerNames)
      rows(data)
      dataTypes([String] * headerNames.size())
    }
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
    columnNames(headers)
    dataTypes(columnTypes)
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

  MatrixBuilder dataTypes(List<Class<?>>... types) {
    if (types.length > 0) {
      this.dataTypes = types[0]
    }
    this
  }

}
