package se.alipsa.matrix.jupyter

/** Immutable facts for one available renderer. */
class ActiveRenderer {
  final MatrixRenderer renderer
  final String providerClassName
  final String preferredMime
  final boolean mimeUsable
  final Set<Class<?>> supportedTypes
  final Map<Class<?>, String> shadowedBy

  ActiveRenderer(MatrixRenderer renderer, String providerClassName, String preferredMime, boolean mimeUsable,
                 Set<Class<?>> supportedTypes, Map<Class<?>, String> shadowedBy = [:]) {
    this.renderer = renderer
    this.providerClassName = providerClassName
    this.preferredMime = preferredMime
    this.mimeUsable = mimeUsable
    this.supportedTypes = Collections.unmodifiableSet(new LinkedHashSet<>(supportedTypes))
    this.shadowedBy = Collections.unmodifiableMap(new LinkedHashMap<>(shadowedBy))
  }
}
