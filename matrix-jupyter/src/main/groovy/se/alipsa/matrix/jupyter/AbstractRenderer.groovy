package se.alipsa.matrix.jupyter

/** Common optional-class probing and plain-text fallback for renderers. */
abstract class AbstractRenderer implements MatrixRenderer {
  protected volatile String missingClass

  protected boolean probe(String className) {
    List<ClassLoader> loaders = []
    [getClass().classLoader, Thread.currentThread().contextClassLoader].each { ClassLoader loader ->
      if (loader != null && !loaders.any { ClassLoader known -> known.is(loader) }) {
        loaders << loader
      }
    }
    for (ClassLoader loader : loaders) {
      try {
        loader.loadClass(className)
        missingClass = null
        return true
      } catch (Throwable ignored) {
        // Try the remaining deployment loader.
      }
    }
    missingClass = className
    false
  }

  @Override
  String unavailableReason() { "${missingClass ?: 'required class'} not on classpath" }

  protected MimeBundle failed(Object value, Throwable error) {
    MimeBundle.plain("${value}\nRendering failed in ${rendererName()}: ${error.message ?: error.class.name}")
  }
}
