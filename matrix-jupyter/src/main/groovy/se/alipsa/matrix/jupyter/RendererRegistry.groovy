package se.alipsa.matrix.jupyter

import se.alipsa.matrix.core.util.Logger

import java.util.concurrent.ConcurrentHashMap

/** Discovers MatrixRenderer services and dispatches values to their renderer. */
@SuppressWarnings(['PropertyName', 'IfStatementBraces', 'ExplicitLinkedHashMapInstantiation', 'UnnecessaryCollectCall'])
class RendererRegistry {
  private static final Logger log = Logger.getLogger(RendererRegistry)
  static final RendererRegistry instance = new RendererRegistry()
  private final Object lock = new Object()
  private volatile boolean loaded
  private volatile List<ActiveRenderer> activeRenderers = []
  private volatile List<SkippedRenderer> skippedRenderers = []
  private final Map<Class<?>, MatrixRenderer> dispatch = new ConcurrentHashMap<>()

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
      MimeBundle.plain("${value}\nRendering failed in ${renderer.rendererName()}: ${error.message ?: error.class.name}")
    }
  }

  /** Produce a host-neutral diagnostic report. */
  String describe() {
    load()
    StringBuilder result = new StringBuilder('matrix-jupyter renderers\n')
    activeRenderers.each { ActiveRenderer source ->
      String types = source.supportedTypes.collect { it.simpleName }.join(', ')
      result.append("  active:  ${source.renderer.rendererName()} → ${source.preferredMime}      (${types})\n")
      if (!source.mimeUsable) result.append("             unsupported-mime — '${source.preferredMime}' is not a type/subtype string\n")
      source.shadowedBy.each { Class<?> type, String owner ->
        result.append("             shadowed for ${type.simpleName} by ${owner}\n")
      }
    }
    skippedRenderers.each { SkippedRenderer source ->
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
        ServiceLoader.load(MatrixRenderer, loader).stream().forEach { provider ->
          String name = provider.type().name
          if (seen.add(name)) discoverProvider(provider, name, found, missed, routes)
        }
      } catch (Throwable error) {
        log.warn("Could not discover matrix-jupyter renderers from ${loader}", error)
      }
    }
    activeRenderers = Collections.unmodifiableList(found)
    skippedRenderers = Collections.unmodifiableList(missed)
    dispatch.clear(); dispatch.putAll(routes)
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
      String reason = error.message ?: error.class.name
      log.warn("Could not load renderer ${providerName}", error)
      missed << new SkippedRenderer(display, providerName, null, null, reason)
    }
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
