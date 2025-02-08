package se.alipsa.matrix.bigquery

import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.StandardSQLTypeName

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TypeMapper {

  // TODO: check each one and finish mapping
  static Class convertType(LegacySQLTypeName typeName) {
    return switch (typeName) {
      case LegacySQLTypeName.BIGNUMERIC -> BigDecimal
      case LegacySQLTypeName.BOOLEAN -> Boolean
      case LegacySQLTypeName.BYTES -> byte[]
      case LegacySQLTypeName.DATE -> LocalDate
      case LegacySQLTypeName.DATETIME -> LocalDateTime
      case LegacySQLTypeName.FLOAT -> Double
        //case LegacySQLTypeName.GEOGRAPHY -> Object
      case LegacySQLTypeName.INTEGER -> Long
        //case LegacySQLTypeName.INTERVAL -> Duration
        //case LegacySQLTypeName.JSON ->
      case LegacySQLTypeName.NUMERIC -> BigDecimal
        //case LegacySQLTypeName.RANGE -> Range
        //case LegacySQLTypeName.RECORD
      case LegacySQLTypeName.STRING -> String
      case LegacySQLTypeName.TIMESTAMP -> Instant
      case LegacySQLTypeName.TIME -> LocalTime
      default -> Object
    }
  }

  static StandardSQLTypeName toStandardSqlType(Class cls) {
    switch (cls) {
      case BigDecimal -> StandardSQLTypeName.BIGNUMERIC
      case BigInteger, Integer, Long -> StandardSQLTypeName.INT64
      case Boolean -> StandardSQLTypeName.BOOL
      case Byte -> StandardSQLTypeName.BYTES
      case Double, Float -> StandardSQLTypeName.FLOAT64
      case LocalDate, Date -> StandardSQLTypeName.DATE
      case LocalDateTime -> StandardSQLTypeName.DATETIME
      case LocalTime -> StandardSQLTypeName.TIME
      case String -> StandardSQLTypeName.STRING
      case Timestamp, Instant -> StandardSQLTypeName.TIMESTAMP
      default -> StandardSQLTypeName.STRING
    }
  }
}
