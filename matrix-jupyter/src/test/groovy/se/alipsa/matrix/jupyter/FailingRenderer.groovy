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
  static String activeMime = 'text/html'

  @Override String rendererName() { 'DisappearingRenderer' }
  @Override boolean available() { present }
  @Override Set<Class<?>> supportedTypes() { [DisappearingValue] as Set<Class<?>> }
  @Override String preferredMime() { activeMime }
  @Override String unavailableReason() { 'test renderer was removed' }
  @Override MimeBundle render(Object value, RenderOptions options) {
    activeMime == 'image/svg+xml' ? MimeBundle.svg('<svg/>', 'present') : MimeBundle.html('<b>present</b>', 'present')
  }
}

/** Value handled by {@link DisappearingRenderer}. */
class DisappearingValue {
  final String description = 'disappearing renderer test value'
}

/** Interface used to test registry interface dispatch. */
interface InterfaceRenderable { }

/** Test-only renderer registered for {@link InterfaceRenderable}. */
class InterfaceRenderer implements MatrixRenderer {
  @Override String rendererName() { 'InterfaceRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [InterfaceRenderable] as Set<Class<?>> }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.html('<b>interface</b>', 'interface') }
}

/** Value handled through its {@link InterfaceRenderable} implementation. */
class InterfaceValue implements InterfaceRenderable {
  final String description = 'interface renderer test value'
}

/** Base value used to test superclass dispatch. */
class ParentValue {
  final String description = 'parent renderer test value'
}

/** Subclass rendered through its {@link ParentValue} superclass. */
class ChildValue extends ParentValue {
  final String childDescription = 'child renderer test value'
}

/** Test-only renderer registered for {@link ParentValue}. */
class ParentRenderer implements MatrixRenderer {
  @Override String rendererName() { 'ParentRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [ParentValue] as Set<Class<?>> }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.html('<b>parent</b>', 'parent') }
}

/** Value whose renderer is advertised only by a temporary TCCL service resource. */
class TcclOnlyValue {
  final String description = 'TCCL-only renderer test value'
}

/** Test-only renderer discovered from a service resource visible only through the TCCL. */
class TcclOnlyRenderer implements MatrixRenderer {
  @Override String rendererName() { 'TcclOnlyRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [TcclOnlyValue] as Set<Class<?>> }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.html('<b>tccl</b>', 'tccl') }
}

/** Value used to assert provider deduplication across discovery loaders. */
class DedupeValue {
  final String description = 'deduplicated renderer test value'
}

/** Test-only renderer whose instance count records ServiceLoader deduplication. */
class DedupeRenderer implements MatrixRenderer {
  static int instances

  DedupeRenderer() {
    instances++
  }

  @Override String rendererName() { 'DedupeRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [DedupeValue] as Set<Class<?>> }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.html('<b>dedupe</b>', 'dedupe') }
}

/** Test-only provider that fails before a renderer instance is available. */
class ThrowingConstructorRenderer implements MatrixRenderer {
  ThrowingConstructorRenderer() {
    throw new IllegalStateException('constructor failure')
  }

  @Override String rendererName() { 'ThrowingConstructorRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [] as Set<Class<?>> }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.plain('unreachable') }
}

/** Test-only provider whose availability check fails. */
class ThrowingAvailabilityRenderer implements MatrixRenderer {
  @Override String rendererName() { 'ThrowingAvailabilityRenderer' }
  @Override boolean available() { throw new IllegalStateException('availability failure') }
  @Override Set<Class<?>> supportedTypes() { [] as Set<Class<?>> }
  @Override MimeBundle render(Object value, RenderOptions options) { MimeBundle.plain('unreachable') }
}

/** Value used to assert that plain-only rendering does not invoke rich rendering. */
class LazyValue {
  final String description = 'lazy renderer test value'
}

/** Test-only renderer that counts rich rendering calls. */
class LazyRenderer implements MatrixRenderer {
  static int renderCalls

  @Override String rendererName() { 'LazyRenderer' }
  @Override boolean available() { true }
  @Override Set<Class<?>> supportedTypes() { [LazyValue] as Set<Class<?>> }
  @Override String preferredMime() { 'image/svg+xml' }
  @Override String plainText(Object value, RenderOptions options) { 'lazy plain text' }
  @Override MimeBundle render(Object value, RenderOptions options) {
    renderCalls++
    MimeBundle.svg('<svg/>', plainText(value, options))
  }
}
