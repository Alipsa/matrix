package se.alipsa.matrix.charts

import se.alipsa.matrix.core.Matrix;


class PieChart extends Chart {

  static PieChart create(String title, List<?> groupCol, List<?> numberCol){
    PieChart chart = new PieChart()
    chart.categorySeries = groupCol
    chart.valueSeries = [numberCol]
    chart.title = title
    return chart
  }

  static PieChart create(Matrix table, String groupColName, String numberColName){
    return create(table.matrixName, table, groupColName, numberColName)
  }

  static PieChart create(String title, Matrix table, String groupColName, String numberColName){
    return create(title, table.column(groupColName), table.column(numberColName))
  }
}
