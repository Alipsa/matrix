package se.alipsa.matrix.jupyter

/** Common optional-class probing and plain-text fallback for renderers. */
abstract class AbstractRenderer implements MatrixRenderer {
  protected String missingClass

  protected boolean probe(String className) {
    try {
      ClassLoader tccl = Thread.currentThread().contextClassLoader
      if (tccl == null) {
        missingClass = className
        return false
      }
      tccl.loadClass(className)
      true
    } catch (Throwable ignored) {
      missingClass = className
      false
    }
  }

  @Override
  String unavailableReason() { "${missingClass ?: 'required class'} not on classpath" }

  protected MimeBundle failed(Object value, Throwable error) {
    MimeBundle.plain("${value}\nRendering failed in ${rendererName()}: ${error.message ?: error.class.name}")
  }
}
