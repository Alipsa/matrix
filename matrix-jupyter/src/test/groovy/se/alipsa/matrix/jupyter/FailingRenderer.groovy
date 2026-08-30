package se.alipsa.matrix.jupyter

import se.alipsa.matrix.core.Matrix

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

/** Test-only unavailable renderer that verifies discovery gating. */
class UnavailableRenderer implements MatrixRenderer {
  @Override String rendererName() { 'UnavailableRenderer' }
  @Override boolean available() { false }
  @Override Set<Class<?>> supportedTypes() { throw new AssertionError('must not be called') }
  @Override String unavailableReason() { 'optional test dependency missing' }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.plain(value.toString()) }
}

/** Test-only duplicate renderer used to verify first-provider precedence. */
class DuplicateMatrixRenderer implements MatrixRenderer {
  @Override String rendererName() { 'DuplicateMatrixRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [Matrix] as Set<Class<?>> }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.plain('duplicate') }
}

/** Test-only renderer with a malformed MIME declaration. */
class InvalidMimeRenderer implements MatrixRenderer {
  @Override String rendererName() { 'InvalidMimeRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [InvalidMimeValue] as Set<Class<?>> }
  @Override String preferredMime() { 'not a MIME type' }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.html('<b>available</b>', 'available') }
}

/** Value handled by {@link InvalidMimeRenderer}. */
class InvalidMimeValue {
  final String description = 'invalid MIME test value'
}

/** Test-only renderer that relies on the default HTML MIME normalization. */
class NullMimeRenderer implements MatrixRenderer {
  @Override String rendererName() { 'NullMimeRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [NullMimeValue] as Set<Class<?>> }
  @Override String preferredMime() { null }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.html('<b>normalized</b>', 'normalized') }
}

/** Value handled by {@link NullMimeRenderer}. */
class NullMimeValue {
  final String description = 'null MIME test value'
}

/** Test-only renderer with a valid non-standard MIME declaration. */
class CustomMimeRenderer implements MatrixRenderer {
  static final String MIME = 'application/vnd.matrix-widget+json'

  @Override String rendererName() { 'CustomMimeRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [CustomMimeValue] as Set<Class<?>> }
  @Override String preferredMime() { MIME }
  @Override MimeBundle render(Object value, RenderOptions options) {
    MimeBundle bundle = MimeBundle.plain('custom MIME value')
    bundle.put(MIME, '{"kind":"matrix-widget"}')
    bundle
  }
}

/** Value handled by {@link CustomMimeRenderer}. */
class CustomMimeValue {
  final String description = 'custom MIME test value'
}

/** Test-only renderer that can disappear after it has been registered. */
class DisappearingRenderer implements MatrixRenderer {
  static boolean present = true

  @Override String rendererName() { 'DisappearingRenderer' }
  @Override boolean available() { present }
  @Override Set<Class<?>> supportedTypes() { [DisappearingValue] as Set<Class<?>> }
  @Override String unavailableReason() { 'test renderer was removed' }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.html('<b>present</b>', 'present') }
}

/** Value handled by {@link DisappearingRenderer}. */
class DisappearingValue {
  final String description = 'disappearing renderer test value'
}
