package se.alipsa.matrix.jupyter

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.regex.Pattern

/** Verifies that the host-neutral renderer layer does not link against jjava. */
class HostNeutralRegistryTest {
  @Test
  void rendersWithJjavaHiddenFromAChildFirstRendererLoader() {
    URL[] urls = System.getProperty('java.class.path').split(Pattern.quote(File.pathSeparator)).collect {
      new File(it).toURI().toURL()
    } as URL[]
    NoJjavaChildFirstLoader loader = new NoJjavaChildFirstLoader(urls, getClass().classLoader)
    Matrix matrix = Matrix.builder().columns(value: [1]).build()

    try {
      Class<?> registryType = loader.loadClass('se.alipsa.matrix.jupyter.RendererRegistry')
      Object registry = registryType.getMethod('getInstance').invoke(null)
      Method render = registryType.methods.find { it.name == 'render' && it.parameterCount == 1 }
      Object bundle = render.invoke(registry, matrix)

      assertNotNull(bundle)
      assertTrue((bundle as Map)['text/html'].contains('>1</td>'))
    } finally {
      loader.close()
    }
  }

  private static class NoJjavaChildFirstLoader extends URLClassLoader {
    NoJjavaChildFirstLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent)
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(name)) {
        if (name.startsWith('org.dflib.jjava.')) {
          throw new ClassNotFoundException(name)
        }
        Class<?> loaded = findLoadedClass(name)
        if (loaded == null && name.startsWith('se.alipsa.matrix.jupyter.')) {
          try {
            loaded = findClass(name)
          } catch (ClassNotFoundException ignored) {
            // Test fixtures remain optional when the test runtime omits them.
          }
        }
        if (loaded == null) {
          loaded = super.loadClass(name, false)
        }
        if (resolve) {
          resolveClass(loaded)
        }
        loaded
      }
    }
  }
}
