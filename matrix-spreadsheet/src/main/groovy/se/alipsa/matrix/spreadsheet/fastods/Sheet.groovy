package se.alipsa.matrix.spreadsheet.fastods

class Sheet extends ArrayList<List<?>> {

  Object name

  Object getName() {
    name
  }

  String getSheetName() {
    String.valueOf(name)
  }

  void setName(String name) {
    this.name = name
  }

  void setName(Integer name) {
    this.name = name
  }

  void setName(Number name) {
    this.name = name.intValue()
  }
}
