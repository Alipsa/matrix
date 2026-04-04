package se.alipsa.matrix.core.util

import static se.alipsa.matrix.core.util.ClassUtils.primitiveWrapper

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.ValueConverter

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Internal helper for CSV formatting operations.
 */
@CompileStatic
class CsvHelper {

  static String buildCsvRow(List<?> values, String quoteString, String delimiter, String rowDelimiter, List<Class> types, boolean customQuoteString) {
    StringBuilder rowBuilder = new StringBuilder()
    int max = values.size()
    for (int i = 0; i < max; i++) {
      Class type = (types != null && i < types.size()) ? types[i] : null
      rowBuilder.append(escapeCsvValue(values[i], quoteString, delimiter, rowDelimiter, type, customQuoteString))
      if (i < max - 1) {
        rowBuilder.append(delimiter)
      }
    }
    rowBuilder.toString()
  }

  static String escapeCsvValue(Object value, String quoteString, String delimiter, String rowDelimiter, Class type, boolean customQuoteString) {
    String stringValue = ValueConverter.asString(value) ?: ''
    if (quoteString == null || quoteString.isEmpty()) {
      return stringValue
    }

    boolean shouldQuote = false

    if (customQuoteString && type == String && stringValue && !stringValue.isBlank()) {
      shouldQuote = true
    }

    if (stringValue.contains(quoteString) ||
        (delimiter != null && stringValue.contains(delimiter)) ||
        (rowDelimiter != null && stringValue.contains(rowDelimiter)) ||
        stringValue.contains('\n') ||
        stringValue.contains('\r')) {
      shouldQuote = true
    }

    if (!shouldQuote) {
      return stringValue
    }
    String escaped = stringValue.replace(quoteString, quoteString + quoteString)
    "${quoteString}${escaped}${quoteString}"
  }

  static String typeLabel(Class type) {
    if (type == null) {
      return 'Object'
    }
    if (type.isPrimitive()) {
      return primitiveWrapper(type).simpleName
    }
    if (type == String || type == BigDecimal || type == BigInteger || type == Number || type == Object) {
      return type.simpleName
    }
    String pkg = type.package?.name ?: ''
    if (pkg.startsWith('java.lang') || pkg.startsWith('java.math') || pkg.startsWith('java.time') || pkg.startsWith('java.util') || pkg.startsWith('java.io') || pkg.startsWith('java.net')) {
      return type.simpleName
    }
    type.name
  }

  @SuppressWarnings('Instanceof')
  static String serializeMetadataValue(Object value) {
    if (value == null) {
      return ''
    }
    if (value instanceof LocalDate) {
      return value.toString()
    }
    if (value instanceof LocalDateTime) {
      return value.toString()
    }
    value.toString()
  }

}
