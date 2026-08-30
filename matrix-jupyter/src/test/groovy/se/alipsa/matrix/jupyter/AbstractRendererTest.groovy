package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.jupyter.render.CharmRenderer

import java.util.concurrent.Callable
import java.util.concurrent.Executors

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
    Thread.currentThread().contextClassLoader = new HidingClassLoader(original)

    try {
      assertTrue(new CharmRenderer().available())
    } finally {
      Thread.currentThread().contextClassLoader = original
    }
  }

  @Test
  void synchronizesConcurrentProbesBeforeUpdatingTheDiagnostic() {
    ProbeRenderer renderer = new ProbeRenderer()
    def executor = Executors.newFixedThreadPool(2)
    try {
      def results = executor.invokeAll([
          { renderer.probeClass('test.missing.First') } as Callable<Boolean>,
          { renderer.probeClass('test.missing.Second') } as Callable<Boolean>
      ])

      assertFalse(results*.get().any())
      assertTrue(renderer.unavailableReason() in ['test.missing.First not on classpath', 'test.missing.Second not on classpath'])
    } finally {
      executor.shutdownNow()
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
    HidingClassLoader(ClassLoader parent) {
      super(parent)
    }

    @Override
    Class<?> loadClass(String name) throws ClassNotFoundException {
      if (name == 'se.alipsa.matrix.charm.Chart') {
        throw new ClassNotFoundException(name)
      }
      super.loadClass(name)
    }
  }
}
