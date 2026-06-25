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
    if (ext.isEmpty()) {
      throw new IllegalArgumentException(supportedExtensionsMessage())
    }
    switch (ext.toLowerCase(Locale.ROOT)) {
      case 'png' -> PNG
      case 'jpg', 'jpeg' -> JPEG
      case 'pdf' -> PDF
      case 'svg' -> SVG
      default -> throw new IllegalArgumentException(supportedExtensionsMessage())
    }
  }

  private static String supportedExtensionsMessage() {
    'Supported extensions are .png, .jpg/.jpeg, .pdf, and .svg'
  }

}
