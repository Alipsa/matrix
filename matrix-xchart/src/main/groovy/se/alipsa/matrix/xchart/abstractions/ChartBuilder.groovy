package se.alipsa.matrix.xchart.abstractions

import se.alipsa.matrix.core.Matrix

/**
 * Deferred common configuration for Matrix XChart convenience builders.
 * An XChart instance is created only when a concrete builder calls {@code build()}.
 *
 * @param <B> concrete builder type
 */
abstract class ChartBuilder<B extends ChartBuilder<B>> {

  protected final Matrix data
  protected String chartTitle
  protected Integer chartWidth
  protected Integer chartHeight
  protected String xColumn
  protected List<String> yColumns = []
  protected String xLabel
  protected String yLabel

  protected ChartBuilder(Matrix data) {
    if (data == null) {
      throw new IllegalArgumentException('data cannot be null')
    }
    this.data = data
  }

  B title(String title) { chartTitle = title; this as B }
  B width(int width) { chartWidth = positive('width', width); this as B }
  B height(int height) { chartHeight = positive('height', height); this as B }
  B size(int widthPixels, int heightPixels) { width(widthPixels); height(heightPixels); this as B }
  B x(String columnName) { xColumn = requireName('x', columnName); this as B }
  B y(String... columnNames) {
    if (columnNames == null || columnNames.length == 0) {
      throw new IllegalArgumentException('y requires at least one column')
    }
    yColumns = columnNames.collect { String name -> requireName('y', name) }
    this as B
  }
  B xAxisTitle(String title) { xLabel = title; this as B }
  B yAxisTitle(String title) { yLabel = title; this as B }

  protected void requireColumn(String name) {
    if (data.columnIndex(name) < 0) {
      throw new IllegalArgumentException("Unknown column '$name'")
    }
  }

  protected void requireNumeric(String name) {
    requireColumn(name)
    if (!Number.isAssignableFrom(data.type(name))) {
      throw new IllegalArgumentException("Column '$name' must be numeric")
    }
  }

  protected void requireXAndY() {
    if (xColumn == null) {
      throw new IllegalStateException('x(...) must be called before build()')
    }
    if (yColumns.isEmpty()) {
      throw new IllegalStateException('y(...) must be called before build()')
    }
  }

  protected void applyTo(AbstractChart chart) {
    if (chartTitle != null) {
      chart.title = chartTitle
    }
    if (xLabel != null) {
      chart.setXLabel(xLabel)
    }
    if (yLabel != null) {
      chart.setYLabel(yLabel)
    }
  }

  private static int positive(String name, int value) {
    if (value <= 0) {
      throw new IllegalArgumentException("$name must be positive")
    }
    value
  }

  private static String requireName(String method, String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("$method column cannot be blank")
    }
    name
  }
}
