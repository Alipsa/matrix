package se.alipsa.matrix.jupyter.kernel

import org.dflib.jjava.jupyter.Extension
import org.dflib.jjava.jupyter.kernel.BaseKernel
import org.dflib.jjava.jupyter.kernel.display.DisplayData
import org.dflib.jjava.jupyter.kernel.display.RenderContext
import org.dflib.jjava.jupyter.kernel.display.Renderer
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType

import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.jupyter.ActiveRenderer
import se.alipsa.matrix.jupyter.MimeBundle
import se.alipsa.matrix.jupyter.RendererRegistry

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

/** Thin jjava adapter for the host-neutral Matrix renderer registry. */
@SuppressWarnings(['FieldName', 'IfStatementBraces'])
class MatrixJupyterExtension implements Extension {
  private static final String LIST_SEPARATOR = ', '
  private static final String TEXT_PLAIN = 'text/plain'
  private static final Logger log = Logger.getLogger(MatrixJupyterExtension)
  private static final Map<BaseKernel, Map<Class<?>, String>> attached = Collections.synchronizedMap(new WeakHashMap<>())
  // jjava registrations cannot be removed, so retain this state across uninstall/install cycles.
  private static final Map<BaseKernel, Map<Class<?>, String>> registeredTypes = Collections.synchronizedMap(new WeakHashMap<>())
  private static final Map<BaseKernel, Long> kernelOrders = Collections.synchronizedMap(new WeakHashMap<>())
  private static final AtomicLong nextKernelOrder = new AtomicLong()
  private static final Map<String, MIMEType> mimeTypes = ['text/html': MIMEType.TEXT_HTML,
                                                          'image/svg+xml': MIMEType.IMAGE_SVG,
                                                          (TEXT_PLAIN): MIMEType.TEXT_PLAIN].asImmutable()

  @Override
  void install(BaseKernel kernel) {
    Map<Class<?>, String> registered = registeredTypes.computeIfAbsent(kernel) { new ConcurrentHashMap<Class<?>, String>() }
    attached[kernel] = registered
    kernelOrders.computeIfAbsent(kernel) { nextKernelOrder.incrementAndGet() }
    registerNewTypes(kernel, registered)
  }

  @Override
  void uninstall(BaseKernel kernel) { attached.remove(kernel) }

  /** Reload providers and register newly available types with each installed kernel. */
  static void refresh() {
    RendererRegistry.instance.reload()
    Map<BaseKernel, Map<Class<?>, String>> snapshot
    synchronized (attached) { snapshot = new LinkedHashMap<>(attached) }
    snapshot.each { BaseKernel kernel, Map<Class<?>, String> registered -> registerNewTypes(kernel, registered) }
  }

  /** @return the registry report annotated with registration state for each attached kernel. */
  static String describe() {
    Map<BaseKernel, Map<Class<?>, String>> snapshot
    synchronized (attached) { snapshot = new LinkedHashMap<>(attached) }
    if (snapshot.isEmpty()) return RendererRegistry.instance.describe()

    List<BaseKernel> kernels = snapshot.keySet().toList().sort { BaseKernel kernel -> kernelOrders[kernel] }
    Map<BaseKernel, String> labels = [:]
    kernels.eachWithIndex { BaseKernel kernel, int index ->
      String label = "kernel#${index + 1}@${Integer.toHexString(System.identityHashCode(kernel))}"
      labels[kernel] = label
    }
    RendererRegistry.instance.describe { ActiveRenderer source, List<ActiveRenderer> active ->
      kernels.collect { BaseKernel kernel -> kernelStatus(labels[kernel], snapshot[kernel], source, active) }
    }
  }

  private static void registerNewTypes(BaseKernel kernel, Map<Class<?>, String> registered) {
    Renderer renderer = kernel.renderer
    RendererRegistry.instance.active().each { ActiveRenderer source ->
      source.supportedTypes.each { Class<?> type -> registerTypeIfAbsent(kernel, renderer, type, source, registered) }
    }
  }

  private static <T> void registerTypeIfAbsent(BaseKernel kernel, Renderer renderer, Class<T> type, ActiveRenderer source,
                                                Map<Class<?>, String> registered) {
    registered.computeIfAbsent(type, { Class<?> ignored ->
      registerType(kernel, renderer, type, source) ? source.providerClassName : null
    } as Function<Class<?>, String>)
  }

  private static <T> boolean registerType(BaseKernel kernel, Renderer renderer, Class<T> type, ActiveRenderer source) {
    MIMEType preferred = toMimeType(source)
    if (preferred == null) return false
    String preferredMime = source.preferredMime
    renderer.createRegistration(type).preferring(preferred).supporting(MIMEType.TEXT_PLAIN).register { T value, RenderContext context ->
      if (!attached.containsKey(kernel)) return
      MimeBundle bundle = null
      boolean attempted = false
      String missing = null
      String staleMime = null
      Closure<MimeBundle> once = {
        if (!attempted) {
          bundle = RendererRegistry.instance.render(value)
          attempted = true
        }
        bundle
      }
      Closure<String> missingRendererNote = {
        if (missing == null) {
          missing = "${value}\nNo Matrix renderer currently handles ${value.class.name}; call MatrixJupyterExtension.refresh()"
          log.warn("Renderer registration for ${source.renderer.rendererName()} outlived its renderer for ${value.class.name}")
        }
        missing
      }
      context.renderIfRequested(preferred, { MIMEType mime, DisplayData out ->
        MimeBundle rendered = once()
        if (rendered == null) {
          missingRendererNote()
        } else {
          Object data = rendered.get(preferredMime)
          if (data != null) {
            out.putData(mime, data)
          } else {
            staleMime = "${value}\nMatrix renderer for ${value.class.name} no longer produces its registered ${preferredMime} payload; restart the kernel to register its current MIME type"
            log.warn("Renderer registration for ${source.renderer.rendererName()} uses ${preferredMime}, but the current renderer no longer produces that MIME for ${value.class.name}")
          }
        }
      } as BiConsumer<MIMEType, DisplayData>)
      context.renderIfRequested(MIMEType.TEXT_PLAIN, { ->
        Object plain = attempted ? (staleMime ?: bundle?.get(TEXT_PLAIN)) : RendererRegistry.instance.plainText(value)
        plain != null ? plain : missingRendererNote()
      } as Supplier<Object>)
    }
    true
  }

  private static MIMEType toMimeType(ActiveRenderer source) {
    if (!source.mimeUsable) return null
    MIMEType known = mimeTypes[source.preferredMime]
    if (known != null) return known
    try {
      MIMEType.parse(source.preferredMime)
    } catch (Exception error) {
      log.warn("Renderer ${source.renderer.rendererName()} declared unusable preferred MIME '${source.preferredMime}'", error)
      null
    }
  }

  private static String kernelStatus(String label, Map<Class<?>, String> registered,
                                     ActiveRenderer source, List<ActiveRenderer> active) {
    List<Class<?>> owned = source.supportedTypes.findAll { registered[it] == source.providerClassName }.toList()
    if (owned.size() == source.supportedTypes.size()) {
      "${label}: registered"
    } else if (owned.isEmpty()) {
      "${label}: NOT registered"
    } else {
      List<String> missing = source.supportedTypes.findAll { !owned.contains(it) }.collect { Class<?> type ->
        String owner = registered[type]
        ActiveRenderer current = active.find { it.providerClassName == owner }
        owner ? "${type.simpleName} owned by ${current?.renderer?.rendererName() ?: owner}".toString() :
            "${type.simpleName} not registered".toString()
      }
      "${label}: partially registered — owns ${owned*.simpleName.join(LIST_SEPARATOR)}; ${missing.join(LIST_SEPARATOR)}"
    }
  }
}
