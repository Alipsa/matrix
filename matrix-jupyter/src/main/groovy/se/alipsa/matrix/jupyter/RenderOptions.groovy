package se.alipsa.matrix.jupyter

/** Immutable rendering options. */
class RenderOptions {
  static volatile RenderOptions defaults = new RenderOptions()
  final Integer maxRows
  final Integer maxColumns
  final boolean fromHead
  final Map<String, String> attr
  final int width
  final int height

  RenderOptions(Integer maxRows = 50, Integer maxColumns = 50, boolean fromHead = true,
                Map<String, String> attr = [:], int width = 800, int height = 600) {
    this.maxRows = maxRows
    this.maxColumns = maxColumns
    this.fromHead = fromHead
    this.attr = Collections.unmodifiableMap(new LinkedHashMap<>(attr ?: [:]))
    this.width = width
    this.height = height
  }
}
