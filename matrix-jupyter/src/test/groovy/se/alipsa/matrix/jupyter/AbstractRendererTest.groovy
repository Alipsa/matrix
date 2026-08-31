package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.jupyter.render.CharmRenderer

/** Tests optional-class probing through the thread context class loader. */
class AbstractRendererTest {
  @Test
  void probesTheThreadContextClassLoader() {
    ClassLoader original = Thread.currentThread().contextClassLoader
    GroovyClassLoader tccl = new GroovyClassLoader(original)
    tccl.parseClass('package test.tccl\nclass VisibleOnly {}')
    Thread.currentThread().contextClassLoader = tccl
    try {
      assertTrue(new ProbeRenderer().available())
    } finally {
      Thread.currentThread().contextClassLoader = original
      tccl.close()
    }
  }

  @Test
  void reportsMissingClassesWhenTheContextClassLoaderIsUnavailable() {
    ClassLoader original = Thread.currentThread().contextClassLoader
    ProbeRenderer renderer = new ProbeRenderer()
    Thread.currentThread().contextClassLoader = null

    try {
      assertFalse(renderer.available())
      assertEquals('test.tccl.VisibleOnly not on classpath', renderer.unavailableReason())
    } finally {
      Thread.currentThread().contextClassLoader = original
    }
  }

  @Test
  void probesTheRendererLoaderBeforeAContextLoaderThatHidesItsTarget() {
    ClassLoader original = Thread.currentThread().contextClassLoader
    GroovyClassLoader rendererLoader = new GroovyClassLoader(original)
    rendererLoader.parseClass('package test.renderer\nclass RendererOwnedTarget {}')
    Class<?> rendererClass = rendererLoader.parseClass('''package test.renderer
      class RendererOwnedProbe extends se.alipsa.matrix.jupyter.AbstractRenderer {
        String rendererName() { 'RendererOwnedProbe' }
        boolean available() { probe('test.renderer.RendererOwnedTarget') }
        Set<Class<?>> supportedTypes() { [] as Set }
        se.alipsa.matrix.jupyter.MimeBundle render(Object value, se.alipsa.matrix.jupyter.RenderOptions options) {
          se.alipsa.matrix.jupyter.MimeBundle.plain(value.toString())
        }
      }''')
    Thread.currentThread().contextClassLoader = new HidingClassLoader(original, 'test.renderer.RendererOwnedTarget')

    try {
      assertTrue((rendererClass.getDeclaredConstructor().newInstance() as MatrixRenderer).available())
    } finally {
      Thread.currentThread().contextClassLoader = original
      rendererLoader.close()
    }
  }

  private static class ProbeRenderer extends AbstractRenderer {
    @Override String rendererName() { 'ProbeRenderer' }
    @Override boolean available() { probe('test.tccl.VisibleOnly') }
    boolean probeClass(String name) { probe(name) }
    @Override Set<Class<?>> supportedTypes() { [] as Set }
    @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.plain(value.toString()) }
  }

  private static class HidingClassLoader extends ClassLoader {
    private final String hiddenClass

    HidingClassLoader(ClassLoader parent, String hiddenClass) {
      super(parent)
      this.hiddenClass = hiddenClass
    }

    @Override
    Class<?> loadClass(String name) throws ClassNotFoundException {
      if (name == hiddenClass) {
        throw new ClassNotFoundException(name)
      }
      super.loadClass(name)
    }
  }
}
