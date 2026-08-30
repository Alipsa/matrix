package se.alipsa.matrix.jupyter.render

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.Structure
import se.alipsa.matrix.core.Summary
import se.alipsa.matrix.jupyter.AbstractRenderer
import se.alipsa.matrix.jupyter.MimeBundle
import se.alipsa.matrix.jupyter.RenderOptions

/** Renders matrix-core tabular values as escaped HTML tables. */
@SuppressWarnings(['IfStatementBraces', 'DuplicateStringLiteral', 'UnnecessaryCollectCall'])
class CoreRenderer extends AbstractRenderer {
  @Override String rendererName() { 'CoreRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [Matrix, Grid, Row, Column, Summary, Structure] as LinkedHashSet }

  @Override
  MimeBundle render(Object value, RenderOptions options) {
    Matrix matrix = toMatrix(value)
    int totalRows = matrix.rowCount()
    int totalColumns = matrix.columnCount()
    Matrix rendered = matrix
    boolean columnsTruncated = options.maxColumns != null && totalColumns > options.maxColumns
    if (columnsTruncated) rendered = matrix.selectColumns(matrix.columnNames().take(options.maxColumns))
    boolean rowsTruncated = options.maxRows != null && totalRows > options.maxRows
    Map<String, String> attributes = new LinkedHashMap<>(options.attr)
    List<String> notices = []
    if (rowsTruncated || columnsTruncated) {
      if (rowsTruncated) notices << "showing ${options.maxRows} of ${totalRows} rows".toString()
      if (columnsTruncated) notices << "${options.maxColumns} of ${totalColumns} columns".toString()
      attributes.caption = [attributes.caption, notices.join(', ')].findAll { it }.join(' — ')
    }
    String html = rendered.toHtml(attributes, options.maxRows, options.fromHead)
    MimeBundle.html(html, plainText(matrix, value, options, notices))
  }

  @Override
  String plainText(Object value) {
    plainText(value, RenderOptions.defaults)
  }

  @Override
  String plainText(Object value, RenderOptions options) {
    plainText(toMatrix(value), value, options, [])
  }

  private static String plainText(Matrix matrix, Object value, RenderOptions options, List<String> notices) {
    if (!(value instanceof Matrix || value instanceof Row || value instanceof Column || value instanceof Grid || value instanceof Summary || value instanceof Structure)) return value.toString()
    Matrix rendered = options.maxColumns != null && matrix.columnCount() > options.maxColumns ?
        matrix.selectColumns(matrix.columnNames().take(options.maxColumns)) : matrix
    int rows = Math.min(options.maxRows != null ? options.maxRows : rendered.rowCount(), rendered.rowCount())
    String title = rendered.toString() + '\n'
    String suffix = notices ? "\n${notices.join(', ')}" : ''
    String body = options.fromHead ? rendered.head(rows) : rendered.tail(rows)
    title + body + suffix
  }

  private static Matrix toMatrix(Object value) {
    if (value instanceof Matrix) return (Matrix) value
    if (value instanceof Row) {
      Row row = (Row) value
      return Matrix.builder().columns(row.columnNames().withIndex().collectEntries { String name, int index -> [(name): [row[index]]] }).build()
    }
    if (value instanceof Column) {
      Column column = (Column) value
      return Matrix.builder().columns([(column.name ?: 'c1'): column.toList()]).build()
    }
    if (value instanceof Grid) return Matrix.builder().data((Grid) value).build()
    if (value instanceof Summary) return summaryMatrix((Summary) value)
    if (value instanceof Structure) return structureMatrix((Structure) value)
    throw new IllegalArgumentException("Unsupported core value ${value.class.name}")
  }

  private static Matrix summaryMatrix(Summary summary) {
    Map<String, Map<String, ?>> data = summary.getData()
    List<String> keys = data.values().collectMany { it.keySet() }.unique()
    Map<String, List> columns = [variable: data.keySet().toList()]
    keys.each { String key -> columns[key] = data.values().collect { Map<String, ?> values -> values[key] } as List }
    Matrix.builder().columns(columns).build()
  }

  private static Matrix structureMatrix(Structure structure) {
    Map<String, List<String>> data = structure.getData()
    Matrix.builder().columns([variable: data.keySet().toList(), descriptors: data.values().collect { it.join(', ') }]).build()
  }
}
