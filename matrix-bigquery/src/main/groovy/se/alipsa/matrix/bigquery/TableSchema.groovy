package se.alipsa.matrix.bigquery

import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.Table

/**
 * Holds the table and schema created for a Matrix-backed BigQuery table.
 */
class TableSchema {

  Table table
  Schema schema

  TableSchema() {
  }

  TableSchema(Table table, Schema schema) {
    this.table = table
    this.schema = schema
  }

}
