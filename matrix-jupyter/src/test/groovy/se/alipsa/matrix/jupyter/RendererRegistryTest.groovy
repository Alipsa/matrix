package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

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
    assertTrue(registry.active().every { it.mimeUsable })
    assertTrue(bundle['text/html'].contains('>1</td>'))
    assertTrue(registry.describe().contains('active:  CoreRenderer → text/html'))
  }

  @Test
  void returnsNullForValuesWithoutARenderer() {
    assertNull(RendererRegistry.instance.render(new Object()))
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
}
