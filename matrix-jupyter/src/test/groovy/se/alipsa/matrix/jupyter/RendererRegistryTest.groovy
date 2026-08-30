package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

import java.nio.file.Files

/** Tests for the host-neutral service registry's core dispatch path. */
class RendererRegistryTest {
  @Test
  void discoversCoreRendererAndDispatchesMatrixSubtypes() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()
    Matrix matrix = Matrix.builder().columns(value: [1]).build()

    MimeBundle bundle = registry.render(matrix)

    assertNotNull(bundle)
    assertTrue(registry.active().any { it.renderer.rendererName() == 'CoreRenderer' })
    assertTrue(registry.active().find { it.renderer.rendererName() == 'CoreRenderer' }.mimeUsable)
    assertTrue(bundle['text/html'].contains('>1</td>'))
    assertTrue(registry.describe().contains('active:  CoreRenderer → text/html'))
  }

  @Test
  void returnsNullForValuesWithoutARenderer() {
    assertNull(RendererRegistry.instance.render(new Object()))
  }

  @Test
  void dispatchesValuesThroughTheirImplementedInterface() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()

    MimeBundle bundle = registry.render(new InterfaceValue())

    assertEquals('<b>interface</b>', bundle['text/html'])
  }

  @Test
  void dispatchesSubclassValuesThroughTheirSuperclass() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()

    MimeBundle bundle = registry.render(new ChildValue())

    assertEquals('<b>parent</b>', bundle['text/html'])
  }

  @Test
  void unionsTcclProvidersAndDeduplicatesProvidersVisibleToBothLoaders() {
    File descriptor = Files.createTempFile('matrix-jupyter-renderers-', '.service').toFile()
    descriptor.text = '''se.alipsa.matrix.jupyter.DedupeRenderer
se.alipsa.matrix.jupyter.TcclOnlyRenderer
'''
    ClassLoader original = Thread.currentThread().contextClassLoader
    DedupeRenderer.instances = 0
    Thread.currentThread().contextClassLoader = new ServiceResourceClassLoader(original, descriptor.toURI().toURL())

    try {
      RendererRegistry registry = RendererRegistry.instance
      registry.reload()

      assertTrue(registry.active().any { it.renderer.rendererName() == 'TcclOnlyRenderer' })
      assertEquals('<b>tccl</b>', registry.render(new TcclOnlyValue())['text/html'])
      assertEquals(1, DedupeRenderer.instances)
    } finally {
      Thread.currentThread().contextClassLoader = original
      descriptor.delete()
      RendererRegistry.instance.reload()
    }
  }

  @Test
  void degradesThrowingRenderersToPlainText() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()

    MimeBundle bundle = registry.render(new FailingValue())

    assertEquals(['text/plain'], bundle.keySet().toList())
    assertTrue(bundle['text/plain'].contains('Rendering failed in FailingRenderer: intentional failure'))
  }

  @Test
  void reportsUnavailableProvidersWithoutInspectingTheirTypes() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()

    SkippedRenderer skipped = registry.skipped().find { it.rendererName == 'UnavailableRenderer' }

    assertNotNull(skipped)
    assertEquals('optional test dependency missing', skipped.reason)
    assertTrue(registry.describe().contains('skipped: UnavailableRenderer → text/html — optional test dependency missing'))
  }

  @Test
  void retainsTheFirstRendererForDuplicateTypesAndReportsTheShadow() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()

    ActiveRenderer duplicate = registry.active().find { it.renderer.rendererName() == 'DuplicateMatrixRenderer' }

    assertNotNull(duplicate)
    assertEquals('se.alipsa.matrix.jupyter.render.CoreRenderer', duplicate.shadowedBy[Matrix])
    assertTrue(registry.render(Matrix.builder().columns(value: [1]).build())['text/html'].contains('>1</td>'))
    assertTrue(registry.describe().contains('shadowed for Matrix by se.alipsa.matrix.jupyter.render.CoreRenderer'))
  }

  @Test
  void retainsMalformedMimeRenderersForHostNeutralConsumers() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()

    ActiveRenderer invalid = registry.active().find { it.renderer.rendererName() == 'InvalidMimeRenderer' }
    MimeBundle bundle = registry.render(new InvalidMimeValue())

    assertNotNull(invalid)
    assertTrue(!invalid.mimeUsable)
    assertEquals('<b>available</b>', bundle['text/html'])
    assertTrue(registry.describe().contains("unsupported-mime — 'not a MIME type'"))
  }

  @Test
  void normalizesMissingMimeToHtmlForHostNeutralConsumers() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()

    ActiveRenderer normalized = registry.active().find { it.renderer.rendererName() == 'NullMimeRenderer' }
    MimeBundle bundle = registry.render(new NullMimeValue())

    assertEquals('text/html', normalized.preferredMime)
    assertTrue(normalized.mimeUsable)
    assertEquals('<b>normalized</b>', bundle['text/html'])
  }

  @Test
  void isolatesBrokenProvidersWithoutAbortingLaterDiscovery() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()

    SkippedRenderer constructorFailure = registry.skipped().find { it.providerClassName.endsWith('ThrowingConstructorRenderer') }
    SkippedRenderer availabilityFailure = registry.skipped().find { it.providerClassName.endsWith('ThrowingAvailabilityRenderer') }

    assertEquals('ThrowingConstructorRenderer', constructorFailure.rendererName)
    assertEquals('constructor failure', constructorFailure.reason)
    assertNull(constructorFailure.preferredMime)
    assertNull(constructorFailure.mimeUsable)
    assertEquals('ThrowingAvailabilityRenderer', availabilityFailure.rendererName)
    assertEquals('availability failure', availabilityFailure.reason)
    assertTrue(registry.active().any { it.renderer.rendererName() == 'DedupeRenderer' })
  }

  @Test
  void exposesImmutableDiscoveryAndShadowFacts() {
    RendererRegistry registry = RendererRegistry.instance
    registry.reload()
    ActiveRenderer duplicate = registry.active().find { it.renderer.rendererName() == 'DuplicateMatrixRenderer' }

    assertThrows(UnsupportedOperationException) { registry.active().clear() }
    assertThrows(UnsupportedOperationException) { duplicate.supportedTypes.clear() }
    assertThrows(UnsupportedOperationException) { duplicate.shadowedBy.clear() }
  }

  private static class ServiceResourceClassLoader extends ClassLoader {
    private static final String SERVICE = 'META-INF/services/se.alipsa.matrix.jupyter.MatrixRenderer'
    private final URL descriptor

    ServiceResourceClassLoader(ClassLoader parent, URL descriptor) {
      super(parent)
      this.descriptor = descriptor
    }

    @Override
    Enumeration<URL> getResources(String name) throws IOException {
      name == SERVICE ? Collections.enumeration([descriptor]) : super.getResources(name)
    }
  }
}
