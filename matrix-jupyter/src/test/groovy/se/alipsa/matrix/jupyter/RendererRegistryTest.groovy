package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull
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
}
