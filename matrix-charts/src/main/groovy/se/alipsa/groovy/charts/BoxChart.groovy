package se.alipsa.groovy.charts

import se.alipsa.groovy.matrix.ListConverter
import se.alipsa.groovy.matrix.Matrix

class BoxChart extends Chart {

  static BoxChart create(String title, Matrix data, String categoryColumnName, String valueColumn) {

    def groups = data.split(categoryColumnName).sort()
    BoxChart chart = new BoxChart()
    chart.title = title
    chart.categorySeries = ListConverter.toStrings(groups.keySet())
    chart.valueSeries = groups.values().collect {it[valueColumn] - null}
    chart.valueSeriesNames = chart.categorySeries as List<String>
    chart.xAxisTitle = categoryColumnName
    chart.yAxisTitle = valueColumn
    return chart
  }

  static BoxChart create(title, Matrix data, List<String> columnNames) {
    BoxChart chart = new BoxChart()
    chart.title = title
    chart.categorySeries = columnNames
    def valueColumns = []
    columnNames.each {
      valueColumns << data[it] - null
    }
    chart.valueSeries = valueColumns
    chart.valueSeriesNames = columnNames
    chart.xAxisTitle = 'X'
    chart.yAxisTitle = 'Y'
    return chart
  }
}
