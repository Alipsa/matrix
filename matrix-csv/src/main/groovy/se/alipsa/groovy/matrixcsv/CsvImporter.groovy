package se.alipsa.groovy.matrixcsv

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import se.alipsa.groovy.matrix.Matrix

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class CsvImporter {

  static Matrix importCsv(InputStream is, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8, String tableName = '') {
    try (CSVParser parser = CSVParser.parse(is, charset, format)) {
      return parse(tableName, parser, firstRowAsHeader)
    }
  }

  static Matrix importCsv(URL url, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) {
    try (CSVParser parser = CSVParser.parse(url, charset, format)) {
      return parse(tableName(url), parser, firstRowAsHeader)
    }
  }

  static Matrix importCsv(File file, CSVFormat format, boolean firstRowAsHeader = true, Charset charset = StandardCharsets.UTF_8) {
    try (CSVParser parser = CSVParser.parse(file, charset, format)) {
      return parse(tableName(file), parser, firstRowAsHeader)
    }
  }

  private static Matrix parse(String tableName, CSVParser parser, boolean firstRowAsHeader) {
    List<List<String>> rows = parser.records*.toList()
    int rowCount = 0
    int ncols = rows[0].size()
    for (List<String> row in rows) {
      if (row.size() != ncols) {
        throw new IllegalArgumentException("This csv file does not have an equal number of columns on each row, error on row $rowCount: extected $ncols but was ${row.size()}")
      }
      rowCount++
    }
    List<String> headerRow = []
    if (parser.headerNames != null && parser.headerNames.size() > 0) {
      headerRow = parser.headerNames
    } else if (firstRowAsHeader) {
        headerRow = rows.remove(0)
    } else {
        for (int i = 0;i < ncols; i++) {
          headerRow << "c" + i
        }
    }
    //println(headerRow)
    //rows.each {println(it)}
    List<Class<?>> types = [String] * ncols
    //println types
    return Matrix.create(tableName, headerRow, rows, types)
  }

  static String tableName(URL url) {
    def name = url.getFile() == null ? url.getPath() : url.getFile()
    if (name.contains('/')) {
      name = name.substring(name.lastIndexOf('/') + 1, name.length())
    }
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    return name
  }

  static String tableName(File file) {
    def name = file.getName()
    if (name.contains('.')) {
      name = name.substring(0, name.lastIndexOf('.'))
    }
    return name
  }
}
