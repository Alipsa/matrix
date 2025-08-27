package se.alipsa.matrix.bigquery

import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.Table
import groovy.transform.CompileStatic

@CompileStatic
class TableSchema {
  Table table
  Schema schema

  TableSchema() {}

  TableSchema(Table table, Schema schema) {
    this.table = table
    this.schema = schema
  }
}
