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
import se.alipsa.matrix.jupyter.SkippedRenderer

import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

/** Thin jjava adapter for the host-neutral Matrix renderer registry. */
@SuppressWarnings(['FieldName', 'IfStatementBraces'])
class MatrixJupyterExtension implements Extension {
  private static final String LIST_SEPARATOR = ', '
  private static final Logger log = Logger.getLogger(MatrixJupyterExtension)
  private static final Map<BaseKernel, Map<Class<?>, String>> attached = Collections.synchronizedMap(new WeakHashMap<>())
  private static final Map<String, MIMEType> mimeTypes = ['text/html': MIMEType.TEXT_HTML,
                                                          'image/svg+xml': MIMEType.IMAGE_SVG,
                                                          'text/plain': MIMEType.TEXT_PLAIN].asImmutable()

  @Override
  void install(BaseKernel kernel) {
    Map<Class<?>, String> registered = attached.computeIfAbsent(kernel) { new ConcurrentHashMap<Class<?>, String>() }
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
    List<ActiveRenderer> active = RendererRegistry.instance.active()
    List<SkippedRenderer> skipped = RendererRegistry.instance.skipped()
    Map<BaseKernel, Map<Class<?>, String>> snapshot
    synchronized (attached) { snapshot = new LinkedHashMap<>(attached) }
    if (snapshot.isEmpty()) return RendererRegistry.instance.describe()

    List<BaseKernel> kernels = snapshot.keySet().toList().sort { BaseKernel kernel ->
      Integer.toUnsignedLong(System.identityHashCode(kernel))
    }
    Map<BaseKernel, String> labels = [:]
    kernels.eachWithIndex { BaseKernel kernel, int index ->
      String label = "kernel#${index + 1}@${Integer.toHexString(System.identityHashCode(kernel))}"
      labels[kernel] = label
    }
    StringBuilder result = new StringBuilder('matrix-jupyter renderers\n')
    active.each { ActiveRenderer source ->
      String types = source.supportedTypes*.simpleName.join(LIST_SEPARATOR)
      result.append("  active:  ${source.renderer.rendererName()} → ${source.preferredMime}      (${types})\n")
      if (!source.mimeUsable) result.append("             unsupported-mime — '${source.preferredMime}' is not a type/subtype string\n")
      source.shadowedBy.each { Class<?> type, String owner ->
        result.append("             shadowed for ${type.simpleName} by ${owner}\n")
      }
      kernels.each { BaseKernel kernel -> appendKernelStatus(result, labels[kernel], snapshot[kernel], source, active) }
    }
    skipped.each { SkippedRenderer source ->
      result.append("  skipped: ${source.rendererName} → ${source.preferredMime ?: '?'} — ${source.reason}\n")
    }
    result.append('Grabbed a module after first render?\n  in a notebook:  MatrixJupyterExtension.refresh()\n  otherwise:      RendererRegistry.instance.reload()')
    result.toString()
  }

  private static void registerNewTypes(BaseKernel kernel, Map<Class<?>, String> registered) {
    Renderer renderer = kernel.renderer
    RendererRegistry.instance.active().each { ActiveRenderer source ->
      source.supportedTypes.each { Class<?> type -> registerTypeIfAbsent(renderer, type, source, registered) }
    }
  }

  private static <T> void registerTypeIfAbsent(Renderer renderer, Class<T> type, ActiveRenderer source,
                                                Map<Class<?>, String> registered) {
    registered.computeIfAbsent(type, { Class<?> ignored ->
      registerType(renderer, type, source) ? source.providerClassName : null
    } as Function<Class<?>, String>)
  }

  private static <T> boolean registerType(Renderer renderer, Class<T> type, ActiveRenderer source) {
    MIMEType preferred = toMimeType(source)
    if (preferred == null) return false
    String preferredMime = source.preferredMime
    renderer.createRegistration(type).preferring(preferred).supporting(MIMEType.TEXT_PLAIN).register { T value, RenderContext context ->
      MimeBundle bundle = null
      String missing = null
      Closure<MimeBundle> once = { bundle != null ? bundle : (bundle = RendererRegistry.instance.render(value)) }
      Closure<String> missingNote = {
        if (missing == null) {
          missing = "${value}\nNo Matrix renderer currently handles ${value.class.name}; call MatrixJupyterExtension.refresh()"
          log.warn("Renderer registration for ${source.renderer.rendererName()} outlived its renderer for ${value.class.name}")
        }
        missing
      }
      context.renderIfRequested(preferred, { MIMEType mime, DisplayData out ->
        Object data = once()?.get(preferredMime)
        if (data != null) {
          out.putData(mime, data)
        } else {
          missingNote()
        }
      } as BiConsumer<MIMEType, DisplayData>)
      context.renderIfRequested(MIMEType.TEXT_PLAIN, { ->
        Object plain = bundle != null ? bundle.get('text/plain') : RendererRegistry.instance.plainText(value)
        plain != null ? plain : missingNote()
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

  private static void appendKernelStatus(StringBuilder result, String label, Map<Class<?>, String> registered,
                                         ActiveRenderer source, List<ActiveRenderer> active) {
    List<Class<?>> owned = source.supportedTypes.findAll { registered[it] == source.providerClassName }.toList()
    if (owned.size() == source.supportedTypes.size()) {
      result.append("             ${label}: registered\n")
    } else if (owned.isEmpty()) {
      result.append("             ${label}: NOT registered\n")
    } else {
      List<String> missing = source.supportedTypes.findAll { !owned.contains(it) }.collect { Class<?> type ->
        String owner = registered[type]
        ActiveRenderer current = active.find { it.providerClassName == owner }
        "${type.simpleName} owned by ${current?.renderer?.rendererName() ?: owner}".toString()
      }
      result.append("             ${label}: partially registered — owns ${owned*.simpleName.join(LIST_SEPARATOR)}; ${missing.join(LIST_SEPARATOR)}\n")
    }
  }
}
