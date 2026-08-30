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
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

/** Thin jjava adapter for the host-neutral Matrix renderer registry. */
@SuppressWarnings(['FieldName', 'IfStatementBraces'])
class MatrixJupyterExtension implements Extension {
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

  /** @return the registry report; kernel attachment diagnostics are intentionally lightweight. */
  static String describe() { RendererRegistry.instance.describe() }

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
      Closure<MimeBundle> once = { bundle != null ? bundle : (bundle = RendererRegistry.instance.render(value)) }
      context.renderIfRequested(preferred, { MIMEType mime, DisplayData out ->
        Object data = once()?.get(preferredMime)
        if (data != null) out.putData(mime, data)
      } as BiConsumer<MIMEType, DisplayData>)
      context.renderIfRequested(MIMEType.TEXT_PLAIN, { ->
        Object plain = once()?.get('text/plain')
        plain != null ? plain : "${value}\nNo Matrix renderer is currently attached; call MatrixJupyterExtension.refresh()"
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
}
