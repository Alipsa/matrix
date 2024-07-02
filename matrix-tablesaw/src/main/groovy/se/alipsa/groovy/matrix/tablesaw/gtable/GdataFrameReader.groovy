package se.alipsa.groovy.matrix.tablesaw.gtable


import tech.tablesaw.io.DataFrameReader
import tech.tablesaw.io.ReadOptions
import tech.tablesaw.io.ReaderRegistry

import java.sql.ResultSet
import java.sql.SQLException

class GdataFrameReader extends DataFrameReader {

  GdataFrameReader(ReaderRegistry registry) {
    super(registry)
  }

  Gtable url(String url) {
    Gtable.create(super.url(url))
  }

  Gtable url(URL url) {
    Gtable.create(super.url(url))
  }

  Gtable string(String s, String fileExtension) {
    Gtable.create(super.string(s, fileExtension))
  }

  Gtable file(String file) {
    Gtable.create(super.file(file))
  }

  Gtable file(File file) {
    Gtable.create(super.file(file))
  }

  def <T extends ReadOptions> Gtable usingOptions(T options) {
    Gtable.create(super.usingOptions(options))
  }

  Gtable usingOptions(ReadOptions.Builder builder) {
    Gtable.create(super.usingOptions(builder))
  }

  Gtable db(ResultSet resultSet) throws SQLException {
    Gtable.create(super.db(resultSet))
  }

  Gtable db(ResultSet resultSet, String tableName) throws SQLException {
    Gtable.create(super.db(resultSet, tableName))
  }
}
