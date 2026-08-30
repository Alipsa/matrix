package se.alipsa.matrix.jupyter

/** Immutable facts for one unavailable or broken renderer provider. */
class SkippedRenderer {
  final String rendererName
  final String providerClassName
  final String preferredMime
  final Boolean mimeUsable
  final String reason

  SkippedRenderer(String rendererName, String providerClassName, String preferredMime, Boolean mimeUsable, String reason) {
    this.rendererName = rendererName
    this.providerClassName = providerClassName
    this.preferredMime = preferredMime
    this.mimeUsable = mimeUsable
    this.reason = reason
  }
}
