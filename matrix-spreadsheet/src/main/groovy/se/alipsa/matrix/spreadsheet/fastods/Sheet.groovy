package se.alipsa.matrix.spreadsheet.fastods

class Sheet extends ArrayList<List<?>> {

  String name

  String getName() {
    name
  }

  String getSheetName() {
    name
  }

  void setName(String name) {
    this.name = name
  }

  void setName(Integer name) {
    this.name = String.valueOf(name)
  }

  void setName(Number name) {
    this.name = String.valueOf(name.intValue())
  }
}
