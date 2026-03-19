package se.alipsa.matrix.bigquery

import groovy.transform.CompileStatic

import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.Table

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
