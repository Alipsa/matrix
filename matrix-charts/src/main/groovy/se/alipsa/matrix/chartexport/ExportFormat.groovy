package se.alipsa.matrix.chartexport

enum ExportFormat {

  PNG,
  JPEG,
  PDF,
  SVG

  static ExportFormat fromFile(File file) {
    if (file == null) {
      return SVG
    }
    String name = file.name
    int dot = name.lastIndexOf('.')
    if (dot < 0) {
      return SVG
    }
    fromExtension(name.substring(dot + 1))
  }

  static ExportFormat fromExtension(String ext) {
    if (ext == null) {
      return SVG
    }
    switch (ext.toLowerCase(Locale.ROOT)) {
      case 'png' -> PNG
      case 'jpg', 'jpeg' -> JPEG
      case 'pdf' -> PDF
      default -> SVG
    }
  }

}
