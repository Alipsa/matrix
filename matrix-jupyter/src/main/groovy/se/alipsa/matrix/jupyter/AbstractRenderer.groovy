package se.alipsa.matrix.jupyter

/** Common optional-class probing and plain-text fallback for renderers. */
@SuppressWarnings('ClassForName') // false avoids class initialization while probing optional modules
abstract class AbstractRenderer implements MatrixRenderer {
  protected String missingClass

  protected boolean probe(String className) {
    try {
      Class.forName(className, false, MatrixRenderer.classLoader)
      true
    } catch (Throwable ignored) {
      try {
        ClassLoader tccl = Thread.currentThread().contextClassLoader
        if (tccl != null) {
          Class.forName(className, false, tccl)
          return true
        }
      } catch (Throwable ignoredAgain) { }
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
