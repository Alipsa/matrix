package se.alipsa.groovy.charts

import se.alipsa.groovy.matrix.Matrix

class BoxChart extends Chart {

  static BoxChart create(String title, Matrix data, String categoryColumnName, String valueColumn) {
    Map<String, Class> conversion = [:]
    conversion.put(categoryColumnName, String)
    List<?> groupColumn = data.convert(conversion).column(categoryColumnName)
    List<?>[] valueColumns = [data.column(valueColumn)] as List<?>[]
    BoxChart chart = new BoxChart()
    chart.title = title
    chart.categorySeries = groupColumn
    chart.valueSeries = valueColumns
    chart.xAxisTitle = categoryColumnName
    chart.yAxisTitle = valueColumn
    return chart
  }
}
