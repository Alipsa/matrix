package se.alipsa.matrix.gg.aes

class Aes {

  String xColName
  List xColumn
  String yColName
  String colorColName

  Aes(List<String> colNames) {
    if(colNames.size() > 0) {
      xColName = colNames[0]
    }
    if(colNames.size() > 1) {
      yColName = colNames[1]
    }
  }

  Aes(List<String> colNames, String colorColumn) {
    this(colNames)
    this.colorColName = colorColumn
  }

  Aes(Map params) {
    if (params.xCol instanceof List) {
      xColumn = params.xCol
    } else {
      xColName = params.xCol
    }
    yColName = params.yCol
    colorColName = params.colorCol
  }

  @Override
  String toString() {
    return "Aes(xCol=$xColName, yCol=$yColName, colorCol=$colorColName)"
  }
}
