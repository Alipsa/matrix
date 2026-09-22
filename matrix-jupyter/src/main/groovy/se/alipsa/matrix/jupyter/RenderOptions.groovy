package se.alipsa.matrix.jupyter

/** Immutable rendering options. */
@SuppressWarnings('IfStatementBraces')
class RenderOptions {
  static volatile RenderOptions defaults = new RenderOptions()
  final Integer maxRows
  final Integer maxColumns
  final boolean fromHead
  final Map<String, String> attr
  final int width
  final int height

  /**
   * Create rendering options.
   *
   * @param maxRows maximum rows to render, or null for no limit (default 50)
   * @param maxColumns maximum columns to render, or null for no limit (default 50)
   * @param fromHead render rows from the start when true, from the end when false (default true)
   * @param attr HTML table attributes passed to Matrix.toHtml (default empty)
   * @param width chart width in pixels (default 800)
   * @param height chart height in pixels (default 600)
   * @throws IllegalArgumentException when a limit is negative or a size is not positive
   */
  RenderOptions(Integer maxRows = 50, Integer maxColumns = 50, boolean fromHead = true,
                Map<String, String> attr = [:], int width = 800, int height = 600) {
    if (maxRows != null && maxRows < 0) throw new IllegalArgumentException("maxRows must be null or >= 0, was ${maxRows}")
    if (maxColumns != null && maxColumns < 0) throw new IllegalArgumentException("maxColumns must be null or >= 0, was ${maxColumns}")
    if (width <= 0) throw new IllegalArgumentException("width must be > 0, was ${width}")
    if (height <= 0) throw new IllegalArgumentException("height must be > 0, was ${height}")
    this.maxRows = maxRows
    this.maxColumns = maxColumns
    this.fromHead = fromHead
    this.attr = Collections.unmodifiableMap(new LinkedHashMap<>(attr ?: [:]))
    this.width = width
    this.height = height
  }
}
