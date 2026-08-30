package se.alipsa.matrix.jupyter

/** Service-provider interface for values rendered by matrix-jupyter. */
interface MatrixRenderer {
  /** @return a human-readable renderer name */
  String rendererName()
  /** @return true when all optional target classes are present */
  boolean available()
  /** @return handled types */
  Set<Class<?>> supportedTypes()
  /** @return reason this renderer is unavailable */
  default String unavailableReason() { 'available() returned false' }
  /** @return richest MIME type emitted by this renderer */
  default String preferredMime() { 'text/html' }
  /** @return options-aware plain-text fallback without rendering the rich payload */
  default String plainText(Object value, RenderOptions options) { value.toString() }
  /** Render a supported value. */
  MimeBundle render(Object value, RenderOptions options)
}
