package se.alipsa.matrix.charts

import se.alipsa.matrix.core.Matrix;

/**
 * Represents a chart in some form.
 * A chart can be exported into various formats using the Plot class e.g:
 * <code>
 * AreaChart chart = new AreaChart(table);
 * inout.view(Plot.jfx(chart))
 * </code>
 */
abstract class Chart<T extends Chart> {

  protected String title
  protected String xAxisTitle = ""
  protected String yAxisTitle = ""

  protected List categorySeries
  protected List<List> valueSeries
  protected List<String> valueSeriesNames

  protected AxisScale xAxisScale = null
  protected AxisScale yAxisScale = null

  protected Legend legend;

  protected Style style = new Style()

  String getTitle() {
    return title;
  }

  List getCategorySeries() {
    return categorySeries;
  }

  List<List> getValueSeries() {
    return valueSeries;
  }

  static void validateSeries(Matrix[] series) {
    int idx = 0
    if (series == null || series.length == 0) {
      throw new IllegalArgumentException("The series contains no data")
    }

    Matrix firstTable = series[0]
    Class firstColumn = firstTable.type(0)
    Class secondColumn = firstTable.type(1)
    if (firstTable.columnCount() != 2) {
      throw new IllegalArgumentException("Table " + idx + "(" + firstTable.matrixName + ") does not contain 2 columns.")
    }

    for (table in series) {
      if (idx == 0) {
        idx++
        continue
      }
      if (table.columnCount() != 2) {
        throw new IllegalArgumentException("Table " + idx + "(" + table.matrixName + ") does not contain 2 columns.")
      }
      Class col0Type = table.type(0)
      Class col1Type = table.type(1)
      if (DataType.differs(firstColumn, col0Type)) {
        throw new IllegalArgumentException("Column mismatch in series $idx. First series has type $firstColumn.name " +
                "in the first column but this series has $col0Type")
      }
      if (DataType.differs(secondColumn, col1Type)) {
        throw new IllegalArgumentException("Column mismatch in series $idx. First series has type $secondColumn.name " +
                "in the second column but this series has $col1Type")
      }
      idx++
    }
  }

  String getxAxisTitle() {
    return xAxisTitle
  }

  String getyAxisTitle() {
    return yAxisTitle
  }

  AxisScale getxAxisScale() {
    return xAxisScale
  }

  T setXAxisScale(AxisScale xAxisScale) {
    this.xAxisScale = xAxisScale
    this as T
  }

  T setXAxisScale(BigDecimal start, BigDecimal end, BigDecimal step) {
    this.xAxisScale = new AxisScale(start, end, step)
    this as T
  }

  AxisScale getyAxisScale() {
    return yAxisScale
  }

  T setYAxisScale(AxisScale yAxisScale) {
    this.yAxisScale = yAxisScale
    this as T
  }

  T setYAxisScale(BigDecimal start, BigDecimal end, BigDecimal step) {
    this.yAxisScale = new AxisScale(start, end, step)
    this as T
  }

  T setTitle(String title) {
    this.title = title
    this as T
  }

  T setXAxisTitle(String xAxisTitle) {
    this.xAxisTitle = xAxisTitle
    this as T
  }

  T setYAxisTitle(String yAxisTitle) {
    this.yAxisTitle = yAxisTitle
    this as T
  }

  T setCategorySeries(List<?> categorySeries) {
    this.categorySeries = categorySeries
    this as T
  }

  T setValueSeries(List<List> valueSeries) {
    this.valueSeries = valueSeries
    this as T
  }

  List<?> getValueSerie(int index) {
    valueSeries[index]
  }

  List<String> getValueSeriesNames() {
    return valueSeriesNames
  }

  T setValueSeriesNames(List<String> valueSeriesNames) {
    this.valueSeriesNames = valueSeriesNames
    this as T
  }

  Style getStyle() {
    return style
  }

  Legend getLegend() {
    return legend
  }

  T setLegend(Legend legend) {
    this.legend = legend
    this as T
  }

  @Override
  String toString() {
    return title + ", " + categorySeries.size() + " categories, " + valueSeries.size() + " value series"
  }
}
