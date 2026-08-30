package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.lang.GroovyClassLoader

import org.junit.jupiter.api.Test

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
    }
  }

  private static class ProbeRenderer extends AbstractRenderer {
    @Override String rendererName() { 'ProbeRenderer' }
    @Override boolean available() { probe('test.tccl.VisibleOnly') }
    @Override Set<Class<?>> supportedTypes() { [] as Set }
    @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.plain(value.toString()) }
  }
}
