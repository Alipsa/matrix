package se.alipsa.matrix.jupyter

import se.alipsa.matrix.core.util.Logger

/** Discovers MatrixRenderer services and dispatches values to their renderer. */
@SuppressWarnings(['PropertyName', 'IfStatementBraces', 'ExplicitLinkedHashMapInstantiation', 'UnnecessaryCollectCall'])
class RendererRegistry {
  private static final Logger log = Logger.getLogger(RendererRegistry)
  // A malformed service declaration can make ServiceLoader repeatedly throw without advancing.
  // Stop after a bounded number of failures so discovery cannot hang indefinitely.
  private static final int MAX_CONSECUTIVE_DISCOVERY_FAILURES = 100
  static final RendererRegistry instance = new RendererRegistry()
  private final Object lock = new Object()
  private volatile boolean loaded
  private volatile List<ActiveRenderer> activeRenderers = []
  private volatile List<SkippedRenderer> skippedRenderers = []
  private volatile Map<Class<?>, MatrixRenderer> dispatch = [:].asImmutable()

  /** @return available renderers from the current discovery pass */
  List<ActiveRenderer> active() { load(); activeRenderers }
  /** @return unavailable renderers from the current discovery pass */
  List<SkippedRenderer> skipped() { load(); skippedRenderers }

  /** Re-run discovery using this artifact's loader and the current TCCL. */
  void reload() { synchronized (lock) { discover(null) } }
  /** Re-run discovery using a host-supplied loader in addition to this artifact's loader. */
  void reload(ClassLoader loader) { synchronized (lock) { discover(loader) } }

  /** Render a value, or return null when no renderer handles it. */
  MimeBundle render(Object value, RenderOptions options = RenderOptions.defaults) {
    if (value == null) return null
    load()
    MatrixRenderer renderer = findRenderer(value.class)
    if (renderer == null) return null
    try {
      renderer.render(value, options ?: RenderOptions.defaults)
    } catch (Throwable error) {
      log.warn("Renderer ${renderer.rendererName()} failed for ${value.class.name}", error)
      MimeBundle.plain(failureText(value, renderer, error))
    }
  }

  /** Return a renderer's plain-text fallback without producing a rich payload. */
  String plainText(Object value, RenderOptions options = RenderOptions.defaults) {
    if (value == null) return null
    load()
    MatrixRenderer renderer = findRenderer(value.class)
    if (renderer == null) return null
    try {
      renderer.plainText(value, options ?: RenderOptions.defaults)
    } catch (Throwable error) {
      log.warn("Renderer ${renderer.rendererName()} failed to produce plain text for ${value.class.name}", error)
      failureText(value, renderer, error)
    }
  }

  /** Produce a host-neutral diagnostic report. */
  String describe() { describe(null) }

  /**
   * Produce a diagnostic report with optional lines for each active renderer.
   *
   * @param annotations receives an active renderer and the current active-renderer snapshot
   * @return diagnostic report
   */
  String describe(Closure<Collection<String>> annotations) {
    List<ActiveRenderer> active
    List<SkippedRenderer> skipped
    synchronized (lock) {
      load()
      active = activeRenderers
      skipped = skippedRenderers
    }
    formatDescription(active, skipped, annotations)
  }

  private static String formatDescription(List<ActiveRenderer> active, List<SkippedRenderer> skipped,
                                          Closure<Collection<String>> annotations) {
    StringBuilder result = new StringBuilder('matrix-jupyter renderers\n')
    active.each { ActiveRenderer source ->
      String types = source.supportedTypes.collect { it.simpleName }.join(', ')
      result.append("  active:  ${source.renderer.rendererName()} → ${source.preferredMime}      (${types})\n")
      if (!source.mimeUsable) result.append("             unsupported-mime — '${source.preferredMime}' is not a type/subtype string\n")
      source.shadowedBy.each { Class<?> type, String owner ->
        result.append("             shadowed for ${type.simpleName} by ${owner}\n")
      }
      annotations?.call(source, active)?.each { String line -> result.append("             ${line}\n") }
    }
    skipped.each { SkippedRenderer source ->
      result.append("  skipped: ${source.rendererName} → ${source.preferredMime ?: '?'} — ${source.reason}\n")
    }
    result.append('Grabbed a module after first render?\n  in a notebook:  MatrixJupyterExtension.refresh()\n  otherwise:      RendererRegistry.instance.reload()')
    result.toString()
  }

  private void load() { if (!loaded) synchronized (lock) { if (!loaded) discover(null) } }

  private void discover(ClassLoader extraLoader) {
    List<ActiveRenderer> found = []
    List<SkippedRenderer> missed = []
    Map<Class<?>, MatrixRenderer> routes = new LinkedHashMap<>()
    Set<String> seen = new LinkedHashSet<>()
    List<ClassLoader> loaders = []
    [MatrixRenderer.classLoader, Thread.currentThread().contextClassLoader, extraLoader].each { ClassLoader loader ->
      if (loader != null && !loaders.any { it.is(loader) }) loaders << loader
    }
    loaders.each { ClassLoader loader ->
      try {
        Iterator<ServiceLoader.Provider<MatrixRenderer>> providers = ServiceLoader.load(MatrixRenderer, loader).stream().iterator()
        int consecutiveFailures = 0
        while (consecutiveFailures < MAX_CONSECUTIVE_DISCOVERY_FAILURES) {
          try {
            if (!providers.hasNext()) break
            ServiceLoader.Provider<MatrixRenderer> provider = providers.next()
            String name = provider.type().name
            if (seen.add(name)) discoverProvider(provider, name, found, missed, routes)
            consecutiveFailures = 0
          } catch (ServiceConfigurationError error) {
            consecutiveFailures++
            log.warn("Could not discover one matrix-jupyter renderer from ${loader}", error)
          }
        }
        if (consecutiveFailures == MAX_CONSECUTIVE_DISCOVERY_FAILURES) {
          log.warn("Stopped renderer discovery from ${loader} after ${MAX_CONSECUTIVE_DISCOVERY_FAILURES} consecutive service configuration failures")
        }
      } catch (Throwable error) {
        log.warn("Could not discover matrix-jupyter renderers from ${loader}", error)
      }
    }
    activeRenderers = Collections.unmodifiableList(found)
    skippedRenderers = Collections.unmodifiableList(missed)
    dispatch = Collections.unmodifiableMap(new LinkedHashMap<>(routes))
    loaded = true
  }

  private void discoverProvider(ServiceLoader.Provider<MatrixRenderer> provider, String providerName,
                                List<ActiveRenderer> found, List<SkippedRenderer> missed,
                                Map<Class<?>, MatrixRenderer> routes) {
    String display = providerName.tokenize('.').last()
    try {
      MatrixRenderer renderer = provider.get()
      display = renderer.rendererName()
      if (!renderer.available()) {
        missed << new SkippedRenderer(display, providerName, normalize(renderer.preferredMime()), mimeUsable(normalize(renderer.preferredMime())), renderer.unavailableReason())
        return
      }
      String mime = normalize(renderer.preferredMime())
      boolean usable = mimeUsable(mime)
      if (!usable) log.warn("Renderer ${display} declared unsupported preferred MIME '${mime}'")
      Set<Class<?>> types = renderer.supportedTypes()
      Map<Class<?>, String> shadows = [:]
      types.each { Class<?> type ->
        MatrixRenderer winner = routes.putIfAbsent(type, renderer)
        if (winner != null) {
          shadows[type] = winner.class.name
          log.warn("Renderer ${display} is shadowed for ${type.name} by ${winner.rendererName()}")
        }
      }
      found << new ActiveRenderer(renderer, providerName, mime, usable, types, shadows)
    } catch (Throwable error) {
      String reason = failureReason(error)
      log.warn("Could not load renderer ${providerName}", error)
      missed << new SkippedRenderer(display, providerName, null, null, reason)
    }
  }

  private static String failureReason(Throwable error) {
    String message = error.message
    String causeMessage = error.cause?.message
    if (message && causeMessage && !message.contains(causeMessage)) return "${message}: ${causeMessage}"
    message ?: causeMessage ?: error.class.name
  }

  private static String failureText(Object value, MatrixRenderer renderer, Throwable error) {
    "${value}\nRendering failed in ${renderer.rendererName()}: ${error.message ?: error.class.name}"
  }

  private static String normalize(String mime) { mime?.trim() ?: 'text/html' }
  private static boolean mimeUsable(String mime) { mime ==~ '^[^\\s/;]+/[^\\s/;]+$' }

  private MatrixRenderer findRenderer(Class<?> type) {
    MatrixRenderer cached = dispatch[type]
    if (cached != null) return cached
    Class<?> current = type
    while (current != null) {
      cached = dispatch[current]
      if (cached != null) return cached
      MatrixRenderer fromInterface = current.interfaces.collect { findInterfaceRenderer(it) }.find { it != null }
      if (fromInterface != null) return fromInterface
      current = current.superclass
    }
    null
  }

  private MatrixRenderer findInterfaceRenderer(Class<?> type) {
    MatrixRenderer direct = dispatch[type]
    if (direct != null) return direct
    type.interfaces.collect { findInterfaceRenderer(it) }.find { it != null }
  }
}
