package se.alipsa.matrix.jupyter.kernel

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

import org.dflib.jjava.jupyter.kernel.BaseKernel
import org.dflib.jjava.jupyter.kernel.JupyterIO
import org.dflib.jjava.jupyter.kernel.LanguageInfo
import org.dflib.jjava.jupyter.kernel.ReplacementOptions
import org.dflib.jjava.jupyter.kernel.comm.CommManager
import org.dflib.jjava.jupyter.kernel.display.DisplayData
import org.dflib.jjava.jupyter.kernel.display.Renderer
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType
import org.dflib.jjava.jupyter.kernel.util.StringStyler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.jupyter.ActiveRenderer
import se.alipsa.matrix.jupyter.CustomMimeValue
import se.alipsa.matrix.jupyter.DisappearingRenderer
import se.alipsa.matrix.jupyter.DisappearingValue
import se.alipsa.matrix.jupyter.FailingValue
import se.alipsa.matrix.jupyter.LazyRenderer
import se.alipsa.matrix.jupyter.LazyValue
import se.alipsa.matrix.jupyter.NullMimeValue
import se.alipsa.matrix.jupyter.RendererRegistry

import java.lang.reflect.Field
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Function

/** Tests the jjava adapter against a real renderer without starting a Jupyter process. */
class MatrixJupyterExtensionTest {
  @AfterEach
  void uninstallAttachedKernels() {
    ['attached', 'registeredTypes', 'kernelOrders'].each { String name -> clearStateMap(name) }
    Field sequence = MatrixJupyterExtension.getDeclaredField('nextKernelOrder')
    sequence.accessible = true
    (sequence.get(null) as AtomicLong).set(0)
  }

  @Test
  void installsHtmlRenderingAndReportsKernelRegistration() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    Matrix matrix = Matrix.builder().columns(value: [1]).build()

    extension.install(kernel)
    DisplayData rendered = kernel.renderer.renderAs(matrix, 'text/html')

    assertTrue(rendered.getData(MIMEType.TEXT_HTML).contains('>1</td>'))
    assertTrue(MatrixJupyterExtension.describe().contains('kernel#1@'))
    assertTrue(MatrixJupyterExtension.describe().contains('CoreRenderer'))
    extension.uninstall(kernel)
    assertEquals(MatrixJupyterExtension.describe(), se.alipsa.matrix.jupyter.RendererRegistry.instance.describe())
  }

  @Test
  void preservesMatrixContentForPlainOnlyRequests() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    Matrix matrix = Matrix.builder().columns(value: [1, 2]).build()

    extension.install(kernel)
    DisplayData rendered = kernel.renderer.renderAs(matrix, 'text/plain')

    assertEquals(matrix.content(), rendered.getData(MIMEType.TEXT_PLAIN))
    extension.uninstall(kernel)
  }

  @Test
  void installsSvgRenderingForCharmCharts() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    Matrix data = Matrix.builder().columns(x: [1, 2], y: [2, 4]).build()
    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
    }.build()

    extension.install(kernel)
    DisplayData rendered = kernel.renderer.renderAs(chart, 'image/svg+xml')

    assertTrue(rendered.getData(MIMEType.IMAGE_SVG).contains('<svg'))
    extension.uninstall(kernel)
  }

  @Test
  void keepsRegistrationsIndependentForEachKernel() {
    TestKernel first = new TestKernel()
    TestKernel second = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    Matrix matrix = Matrix.builder().columns(value: [1]).build()

    extension.install(first)
    extension.install(second)

    assertTrue(first.renderer.renderAs(matrix, 'text/html').getData(MIMEType.TEXT_HTML).contains('>1</td>'))
    assertTrue(second.renderer.renderAs(matrix, 'text/html').getData(MIMEType.TEXT_HTML).contains('>1</td>'))
    String description = MatrixJupyterExtension.describe()
    assertTrue(description.contains('kernel#1@'))
    assertTrue(description.contains('kernel#2@'))

    extension.uninstall(first)
    assertTrue(second.renderer.renderAs(matrix, 'text/html').getData(MIMEType.TEXT_HTML).contains('>1</td>'))
    extension.uninstall(second)
  }

  @Test
  void skipsInvalidMimeRegistrationsWithoutAffectingCoreRendering() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    Matrix matrix = Matrix.builder().columns(value: [1]).build()

    extension.install(kernel)

    assertTrue(kernel.renderer.renderAs(matrix, 'text/html').getData(MIMEType.TEXT_HTML).contains('>1</td>'))
    String description = MatrixJupyterExtension.describe()
    assertTrue(description.contains('InvalidMimeRenderer'))
    assertTrue(description.contains('unsupported-mime'))
    assertTrue(description.contains('NOT registered'))
    extension.uninstall(kernel)
  }

  @Test
  void refreshDoesNotAppendDuplicateRenderFunctions() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()

    extension.install(kernel)
    int registrations = registrationCount(kernel.renderer)
    MatrixJupyterExtension.refresh()
    MatrixJupyterExtension.refresh()

    assertEquals(registrations, registrationCount(kernel.renderer))
    extension.uninstall(kernel)
  }

  @Test
  void reinstallDoesNotAppendDuplicateRenderFunctions() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()

    extension.install(kernel)
    int registrations = registrationCount(kernel.renderer)
    extension.uninstall(kernel)
    extension.install(kernel)

    assertEquals(registrations, registrationCount(kernel.renderer))
    extension.uninstall(kernel)
  }

  @Test
  void uninstallDisablesItsExistingHostRegistrations() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    Matrix matrix = Matrix.builder().columns(value: [1]).build()

    extension.install(kernel)
    extension.uninstall(kernel)

    assertNull(kernel.renderer.renderAs(matrix, 'text/html').getData(MIMEType.TEXT_HTML))
  }

  @Test
  void registersMissingMimeRenderersAsHtml() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()

    extension.install(kernel)
    DisplayData rendered = kernel.renderer.renderAs(new NullMimeValue(), 'text/html')

    assertEquals('<b>normalized</b>', rendered.getData(MIMEType.TEXT_HTML))
    extension.uninstall(kernel)
  }

  @Test
  void registersValidNonStandardMimeRenderers() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()

    extension.install(kernel)
    DisplayData rendered = kernel.renderer.renderAs(new CustomMimeValue(), 'application/vnd.matrix-widget+json')

    assertEquals('{"kind":"matrix-widget"}', rendered.getData(MIMEType.parse('application/vnd.matrix-widget+json')))
    extension.uninstall(kernel)
  }

  @Test
  void degradesRichRendererFailuresAndUsesThePlainTextFallback() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()

    extension.install(kernel)
    DisplayData rich = kernel.renderer.renderAs(new FailingValue(), 'text/html')
    DisplayData plain = kernel.renderer.renderAs(new FailingValue(), 'text/plain')

    assertNull(rich.getData(MIMEType.TEXT_HTML))
    assertTrue(plain.getData(MIMEType.TEXT_PLAIN).contains('failing value'))
    assertFalse(plain.getData(MIMEType.TEXT_PLAIN).contains('Rendering failed'))
    extension.uninstall(kernel)
  }

  @Test
  void rendersPlainOnlyRequestsWithoutProducingARichPayload() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    LazyRenderer.renderCalls = 0

    extension.install(kernel)
    DisplayData rendered = kernel.renderer.renderAs(new LazyValue(), 'text/plain')

    assertEquals('lazy plain text', rendered.getData(MIMEType.TEXT_PLAIN))
    assertEquals(0, LazyRenderer.renderCalls)
    extension.uninstall(kernel)
  }

  @Test
  void givesPlainTextRefreshGuidanceForStaleRegistrations() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    DisappearingRenderer.present = true
    RendererRegistry.instance.reload()
    extension.install(kernel)

    try {
      DisappearingRenderer.present = false
      RendererRegistry.instance.reload()

      DisplayData rich = kernel.renderer.renderAs(new DisappearingValue(), 'text/html')
      DisplayData plain = kernel.renderer.renderAs(new DisappearingValue(), 'text/plain')

      assertNull(rich.getData(MIMEType.TEXT_HTML))
      assertTrue(plain.getData(MIMEType.TEXT_PLAIN).contains('No Matrix renderer currently handles'))
      assertTrue(plain.getData(MIMEType.TEXT_PLAIN).contains('MatrixJupyterExtension.refresh()'))
    } finally {
      DisappearingRenderer.present = true
      RendererRegistry.instance.reload()
      extension.uninstall(kernel)
    }
  }

  @Test
  void explainsWhenARendererChangesItsRegisteredMime() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    DisappearingRenderer.present = true
    DisappearingRenderer.activeMime = 'text/html'
    RendererRegistry.instance.reload()
    extension.install(kernel)

    try {
      DisappearingRenderer.activeMime = 'image/svg+xml'
      RendererRegistry.instance.reload()

      DisplayData rendered = kernel.renderer.renderAs(new DisappearingValue(), 'text/html')

      assertNull(rendered.getData(MIMEType.TEXT_HTML))
      assertTrue(rendered.getData(MIMEType.TEXT_PLAIN).contains('no longer produces its registered text/html payload'))
      assertTrue(rendered.getData(MIMEType.TEXT_PLAIN).contains('restart the kernel'))
      assertFalse(rendered.getData(MIMEType.TEXT_PLAIN).contains('No Matrix renderer currently handles'))
    } finally {
      DisappearingRenderer.activeMime = 'text/html'
      RendererRegistry.instance.reload()
      extension.uninstall(kernel)
    }
  }

  @Test
  void refreshRegistersRenderersThatAppearAfterInstallation() {
    TestKernel kernel = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    DisappearingRenderer.present = false
    RendererRegistry.instance.reload()
    extension.install(kernel)

    try {
      DisappearingRenderer.present = true
      MatrixJupyterExtension.refresh()

      DisplayData rendered = kernel.renderer.renderAs(new DisappearingValue(), 'text/html')

      assertEquals('<b>present</b>', rendered.getData(MIMEType.TEXT_HTML))
    } finally {
      DisappearingRenderer.present = true
      RendererRegistry.instance.reload()
      extension.uninstall(kernel)
    }
  }

  @Test
  void refreshBringsEarlierKernelsUpToDateWithoutReregisteringLaterOnes() {
    TestKernel first = new TestKernel()
    TestKernel second = new TestKernel()
    MatrixJupyterExtension extension = new MatrixJupyterExtension()
    DisappearingRenderer.present = false
    RendererRegistry.instance.reload()
    extension.install(first)

    try {
      DisappearingRenderer.present = true
      RendererRegistry.instance.reload()
      extension.install(second)

      String description = MatrixJupyterExtension.describe()
      List<String> status = rendererStatusLines(description, 'DisappearingRenderer')
      assertEquals(2, status.size())
      assertTrue(status.any { it.contains('NOT registered') })
      assertTrue(status.any { it.endsWith(': registered') })
      assertEquals(description, MatrixJupyterExtension.describe())

      MatrixJupyterExtension.refresh()

      assertEquals('<b>present</b>', first.renderer.renderAs(new DisappearingValue(), 'text/html').getData(MIMEType.TEXT_HTML))
      assertEquals('<b>present</b>', second.renderer.renderAs(new DisappearingValue(), 'text/html').getData(MIMEType.TEXT_HTML))
    } finally {
      DisappearingRenderer.present = true
      RendererRegistry.instance.reload()
      extension.uninstall(first)
      extension.uninstall(second)
    }
  }

  @Test
  void describesUnregisteredTypesWithoutANullOwner() {
    ActiveRenderer source = new ActiveRenderer(new TestStatusRenderer(), TestStatusRenderer.name,
        'text/html', true, [Matrix, FailingValue] as Set<Class<?>>)
    Map<Class<?>, String> registered = [(Matrix): TestStatusRenderer.name]
    def method = MatrixJupyterExtension.getDeclaredMethod('kernelStatus', String, Map, ActiveRenderer, List)
    method.accessible = true

    String status = method.invoke(null, 'kernel#test', registered, source, [source]) as String

    assertTrue(status.contains('Matrix'))
    assertTrue(status.contains('FailingValue not registered'))
    assertFalse(status.contains('owned by null'))
  }

  private static List<String> rendererStatusLines(String description, String rendererName) {
    int start = description.indexOf("  active:  ${rendererName}")
    int nextActive = description.indexOf('\n  active:', start + 1)
    int nextSkipped = description.indexOf('\n  skipped:', start + 1)
    int end = [nextActive, nextSkipped].findAll { it >= 0 }.min() ?: description.length()
    description.substring(start, end).readLines().findAll { it.contains('kernel#') }
  }

  private static int registrationCount(Renderer renderer) {
    Field field = Renderer.getDeclaredField('renderFunctions')
    field.accessible = true
    Map<Class<?>, List<?>> registrations = field.get(renderer) as Map<Class<?>, List<?>>
    registrations.values()*.size().sum(0) as int
  }

  private static void clearStateMap(String name) {
    Field field = MatrixJupyterExtension.getDeclaredField(name)
    field.accessible = true
    Map<?, ?> state = field.get(null) as Map<?, ?>
    synchronized (state) {
      state.clear()
    }
  }

  private static class TestStatusRenderer implements se.alipsa.matrix.jupyter.MatrixRenderer {
    @Override String rendererName() { 'TestStatusRenderer' }
    @Override boolean available() { true }
    @Override Set<Class<?>> supportedTypes() { [Matrix, FailingValue] as Set<Class<?>> }
    @Override se.alipsa.matrix.jupyter.MimeBundle render(Object value, se.alipsa.matrix.jupyter.RenderOptions options) {
      se.alipsa.matrix.jupyter.MimeBundle.plain(value.toString())
    }
  }

  private static class TestKernel extends BaseKernel {
    TestKernel() {
      super('test', '1', new LanguageInfo('Groovy', '5', 'text/x-groovy', '.groovy', 'groovy', null, 'script'), [],
          null, new JupyterIO(StandardCharsets.UTF_8), new CommManager(), new Renderer(), null, null, false,
          new StringStyler('', '', '', { Integer ignored -> '' } as Function<Integer, String>))
    }

    @Override
    protected Object doEval(String code) { null }

    @Override
    DisplayData inspect(String code, int cursorPosition, boolean detailLevel) { new DisplayData() }

    @Override
    ReplacementOptions complete(String code, int cursorPosition) { null }

    @Override
    String isComplete(String code) { IS_COMPLETE_YES }
  }
}
