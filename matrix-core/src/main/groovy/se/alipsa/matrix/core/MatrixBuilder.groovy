package se.alipsa.matrix.core

import se.alipsa.matrix.core.util.ClassUtils

import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import java.sql.ResultSet
import java.sql.ResultSetMetaData

class MatrixBuilder {

  String matrixName
  List<String> headerList
  List<List> columns
  List<Class> dataTypes

  Matrix build() {
    new Matrix(matrixName, headerList, columns, dataTypes)
  }

  MatrixBuilder matrixName(String name) {
    this.matrixName = name
    this
  }

  MatrixBuilder columnNames(List<String> columnNames) {
    headerList = columnNames
    this
  }

  MatrixBuilder columnNames(Set<String> columnNames) {
    headerList = new ArrayList<>(columnNames)
    this
  }

  MatrixBuilder columnNames(String... colNames) {
    columnNames(colNames as List<String>)
  }

  MatrixBuilder columnNames(Matrix template) {
    columnNames(template.columnNames())
  }

  /**
   * Populate the data of the Matrix. Use only one of rows, rowList, columns or data methods i.e.
   * either provide the rows (observations) or the columns (variables) or one of the data methods
   * if you use multiple ones, only the last one will be used.
   *
   * @param columns a list of variables
   * @return this builder
   */
  MatrixBuilder columns(List<List> columns) {
    if (columns.size() > 0 && !columns[0] instanceof List) {
      throw new IllegalArgumentException("The List of columns does not contain lists but ${columns[0].class}")
    }
    this.columns = columns
    this
  }

  MatrixBuilder columns(List... columns) {
    this.columns = columns as List<List>
    this
  }

  MatrixBuilder columns(Map<String, List> columData) {
    List<String> headers = []
    List<List> cols = []
    columData.each { k, v ->
      headers << String.valueOf(k)
      cols << v.collect()
    }
    if (noColumnNames()) {
      columnNames(headers)
    }
    if (noColumns()) {
      columns(cols)
    }
    this
  }

  /**
   * Populate the data of the Matrix. Use only one of rows, rowList, columns or data methods i.e.
   * either provide the rows (observations) or the columns (variables) or one of the data methods
   * if you use multiple ones, only the last one will be used.
   *
   * @param rows a list of observations
   * @return this builder
   */
  MatrixBuilder rows(List<List> rows) {
    this.columns = rows.transpose()
    this
  }

  /**
   * Populate the data of the Matrix. Use only one of rows, rowList, columns or data methods i.e.
   * either provide the rows (observations) or the columns (variables) or one of the data methods
   * if you use multiple ones, only the last one will be used.
   * This method populates column names, types and data so is superior if you have a list of Row
   * instead of only a List of List.
   *
   * @param rows a list of observations
   * @return this builder
   */
  MatrixBuilder rowList(List<Row> rows) {
    if (rows.isEmpty()) {
      return this
    }
    Row row = rows.first()
    columnNames(row.columnNames())
    types(row.types())
    this.rows(rows)
  }

  /**
   * Build a Matrix (Column names, data content, types) from a List of Maps.
   * The types are assigned from the data in the first row. Each map has the column name as key and the data content
   * as value.
   *
   * @param rows
   * @return
   */
  MatrixBuilder mapList(List<Map> rows) {
    if (rows.isEmpty()) {
      return this
    }
    Map row = rows.first()
    columnNames(row.keySet())
    def t = []
    row.each {
      t << it.value.class
    }
    types(t)
    def r = rows.collect { it.values() as List}
    this.rows(r)
  }

  // This allows us to extract ginq results without relying on ginq as a dependency
  MatrixBuilder ginqResult(Object ginqResult) {
    if (ginqResult == null) {
      return this
    }
    if ('QueryableCollection' == ginqResult.class.getSimpleName()) {
      // this is a QueryableCollection (from GINQ) we need to convert it. We can only read it once and
      // some data contains desirable metadata that we want (hence toList() is a no-go)
      List r = []
      ginqResult.each {
        r << it
      }
      Object row = r.first()
      if (row instanceof Row) {
        // This happens when the whole object is returned in ginq
        columnNames(row.columnNames())
        types(row.types())
        rows(r)
      } else if('NamedRecord' == row.class.simpleName) {
        // this happens when a select contains more than one named item
        List<String> colNames = row.getNameList()
        List<Class> t = []
        boolean isTypesCollected = false
        def rowValues = []
        r.each { namedRecord ->
          def vals = []
          colNames.each {
            vals << namedRecord[it]
            if (!isTypesCollected) {
              t << namedRecord[it].class
            }
          }
          isTypesCollected = true
          rowValues << vals
        }
        columnNames(colNames)
        rows(rowValues)
        types(t)
      } else if (row instanceof Collection) {
        // assuming it is some list of data equivalent to a row, not sure if this ever happens
        def ct = []
        r.first().each {
          ct << it.class
        }
        types(ct)
        rows(r)
      } else {
        // This happens when a single variable is selected
        types([r.first().class])
        columns([r])
      }
    } else {
      throw new RuntimeException("Dont know what to do with $ginqResult.class")
    }
  }

  /**
   * Takes a List of objects and turn them into rows using reflection
   * The names of the variables will be the column names sorted alphabetically
   * Example:
   * <code><pre>
   * class Person {
   *   String name
   *   int id
   *
   *   Person(int id, String name) {
   *     this.id = id
   *     this.name = name
   *   }
   * }
   * def observations = [new Person(1, 'Per'), new Person(2, 'Louise')]
   * def m = Matrix.builder().data(observations).build()
   * assert m[0, 'id'] == 1
   * assert m[1, 1} == 'Louise'
   * </pre></code>
   *
   * Note: this is a bit experimental as it uses some reflection magic to guess the structure
   * of the custom objects. If your custom objects ar POJOS it should be fine but if you do something more
   * exotic, you are probably better off converting the data to a List of List of values and use the
   * rows() method instead.
   *
   * @param observations
   * @return
   */
  MatrixBuilder data(List observations) {
    if (observations == null || observations.isEmpty()) {
      throw new IllegalArgumentException("The list of observations contains no data")
    }
    Object o = observations.first()
    List<String> colNames = o.class.declaredFields
        .findAll { !it.synthetic && !Modifier.isStatic(it.modifiers) }
        .collect { it.name }
        .sort()
    List<List> rowList = []
    List row
    observations.each { obs ->
      row = []
      colNames.each { p ->
        row << obs.properties.(p)
      }
      rowList << row
    }
    columnNames(colNames)
    rows(rowList)
    this
  }

  MatrixBuilder data(Map<String, List> columData) {
    columns(columData)
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
    if (noName()) {
      int endIdx = file.name.length()
      if (file.name.contains('.')) {
        endIdx = file.name.lastIndexOf('.')
      }
      matrixName(file.name.substring(0, endIdx))
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
    if (noName()) {
      String fileName = file.getFileName().toString()
      int endIdx = fileName.length()
      if (file.contains('.')) {
        endIdx = fileName.lastIndexOf('.')
      }
      matrixName(fileName.substring(0, endIdx))
    }
    this
  }

  /**
   * The cvs parsing is reasonably fast but rather primitive.
   * For a much more thorough solution, use the CsvImporter in the matrix-csv library.
   *
   * @param url
   * @param delimiter
   * @param stringQuote
   * @param firstRowAsHeader
   * @return
   */
  MatrixBuilder data(URL url, String delimiter = ',', String stringQuote = '', boolean firstRowAsHeader = true) {
    try (InputStream inputStream = url.openStream()) {
      String n = url.getFile() == null ? url.getPath() : url.getFile()
      if (n.contains('/')) {
        n = n.substring(n.lastIndexOf('/') + 1, n.length())
      }
      if (n.contains('.')) {
        n = n.substring(0, n.lastIndexOf('.'))
      }
      data(inputStream, delimiter, stringQuote, firstRowAsHeader)
      if (noName()) {
        matrixName(n)
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
    try (InputStreamReader reader = new InputStreamReader(inputStream)) {
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
        headerNames = (1..maxCols).collect { 'c' + it }
      }
      if (noColumnNames()) {
        columnNames(headerNames)
      }
      rows(data)
      if (noDataTypes()) {
        types([String] * headerNames.size())
      }
    }
    this
  }

  MatrixBuilder data(ResultSet rs) {
    ResultSetMetaData rsmd = rs.getMetaData()
    int ncols = rsmd.getColumnCount()
    List<String> headers = []
    List<Class> columnTypes = []
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
    if (noDataTypes()) {
      types(columnTypes)
    }
    List<List<Object>> rows = []
    while (rs.next()) {
      List row = []
      for (int i = 1; i <= ncols; i++) {
        row.add(rs.getObject(i))
      }
      rows.add(row)
    }
    columns(rows.transpose())
  }

  MatrixBuilder definition(Map<String, Class> namesAndTypes) {
    columnNames(namesAndTypes.keySet() as List)
    types(namesAndTypes.values() as List)
  }


  MatrixBuilder types(List<Class> types) {
    this.dataTypes = ClassUtils.convertPrimitivesToWrapper(types)
    this
  }

  MatrixBuilder types(Class... dataTypes) {
    types(dataTypes as List<Class>)
  }

  MatrixBuilder types(List<Class>... types) {
    if (types.length > 0) {
      this.dataTypes = types[0]
    }
    this
  }

  MatrixBuilder types(Matrix template) {
    types(template.types())
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
    matrixName == null || matrixName.isBlank()
  }
}
