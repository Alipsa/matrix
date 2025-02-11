package se.alipsa.matrix.bigquery

import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.StandardSQLTypeName
import se.alipsa.matrix.core.ValueConverter

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TypeMapper {

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

  static def convert(Object value, LegacySQLTypeName type) {
    if (value == null) return null
    switch (type) {
      case LegacySQLTypeName.BIGNUMERIC -> ValueConverter.asBigDecimal(value)
      case LegacySQLTypeName.BOOLEAN -> ValueConverter.asBoolean(value)
        //case LegacySQLTypeName.GEOGRAPHY -> Object
      case LegacySQLTypeName.INTEGER -> ValueConverter.asLong(value)
        //case LegacySQLTypeName.INTERVAL -> Duration
        //case LegacySQLTypeName.JSON ->
      case LegacySQLTypeName.NUMERIC -> ValueConverter.asBigDecimal(value)
        //case LegacySQLTypeName.RANGE -> Range
        //case LegacySQLTypeName.RECORD todo: this could probably be converted to a Map
      case LegacySQLTypeName.STRING -> ValueConverter.asString(value)
      case LegacySQLTypeName.TIMESTAMP -> convertToInstant(value)
      //case LegacySQLTypeName.TIME -> LocalTime
      default -> ValueConverter.asString(value)
    }
  }

  static def convertToInstant(Object value) {
    if (value == null) return null
    def epochMillis = (ValueConverter.asBigDecimal(value) * 1000).longValue()
    Instant.ofEpochMilli(epochMillis)
  }
}
