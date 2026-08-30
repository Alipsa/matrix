package se.alipsa.matrix.jupyter

/** Test-only renderer that exercises registry failure handling. */
class FailingRenderer implements MatrixRenderer {
  @Override String rendererName() { 'FailingRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [FailingValue] as Set<Class<?>> }
  @Override MimeBundle render(Object value, RenderOptions options) { throw new IllegalStateException('intentional failure') }
}

/** Value handled by {@link FailingRenderer}. */
class FailingValue {
  @Override String toString() { 'failing value' }
}
