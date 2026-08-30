package se.alipsa.matrix.jupyter

/** Ordered MIME data returned by a Matrix renderer. */
class MimeBundle extends LinkedHashMap<String, Object> {
  private MimeBundle(String richMime, Object richData, String plainFallback) {
    if (richMime != null) {
      put(richMime, richData)
    }
    put('text/plain', plainFallback)
  }

  /** Create an HTML bundle. */
  static MimeBundle html(String html, String plainFallback) { new MimeBundle('text/html', html, plainFallback) }

  /** Create an SVG bundle. */
  static MimeBundle svg(String svg, String plainFallback) { new MimeBundle('image/svg+xml', svg, plainFallback) }

  /** Create a plain-text-only bundle. */
  static MimeBundle plain(String plainFallback) { new MimeBundle(null, null, plainFallback) }
}
