package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

/** Parsed platform-independent glob root, relative matcher and maximum walk depth. */
@PackageScope
final class GlobSpec {
  private static final String SLASH = '/'
  private static final String ESCAPE = '\\'
  private static final int NOT_FOUND = -1

  final String root
  final String pattern
  final int depth
  final boolean glob

  private GlobSpec(String root, String pattern, int depth, boolean glob) {
    this.root = root
    this.pattern = pattern
    this.depth = depth
    this.glob = glob
  }

  static GlobSpec parse(String source, String separator) {
    if (source == null || source.empty) { throw new IllegalArgumentException('Glob pattern cannot be empty') }
    boolean windows = separator == ESCAPE
    String text = windows ? source.replace(ESCAPE, SLASH) : source
    if (windows && text ==~ /^[A-Za-z]:[^\/].*/) { throw new IllegalArgumentException("Drive-relative glob not supported: $source") }
    boolean escaped = false
    boolean escapePresent = false
    int first = NOT_FOUND
    int lastSlash = NOT_FOUND
    for (int index = 0; index < text.length(); index++) {
      char ch = text.charAt(index)
      if (escaped) { escaped = false; continue }
      if (ch == ESCAPE as char) { escaped = true; escapePresent = true; continue }
      if (ch in ['*' as char, '?' as char, '[' as char, '{' as char]) { first = index; break }
      if (ch == SLASH as char) { lastSlash = index }
    }
    if (first < 0 && !escapePresent) { return new GlobSpec(text, '', 0, false) }
    if (first < 0) { lastSlash = text.lastIndexOf(SLASH) }
    String prefix = lastSlash < 0 ? '.' : text.substring(0, lastSlash == 0 || lastSlash == 2 && text.charAt(1) == ':' as char ? lastSlash + 1 : lastSlash)
    String relative = text.substring(lastSlash + 1)
    StringBuilder literal = new StringBuilder()
    escaped = false
    prefix.each { String ch ->
      if (escaped) { literal.append(ch); escaped = false }
      else if (ch == ESCAPE) { escaped = true }
      else { literal.append(ch) }
    }
    int segments = 1
    int braces = 0
    boolean bracket = false
    boolean unlimited = false
    escaped = false
    for (int index = 0; index < relative.length(); index++) {
      char ch = relative.charAt(index)
      if (escaped) { escaped = false; continue }
      if (ch == ESCAPE as char) { escaped = true; continue }
      if (ch == '[' as char) { bracket = true }
      if (ch == ']' as char) { bracket = false }
      if (bracket) { continue }
      if (ch == '{' as char) { braces++ }
      if (ch == '}' as char) { braces-- }
      if (ch == SLASH as char) { segments++; if (braces > 0) { unlimited = true } }
      if (ch == '*' as char && index + 1 < relative.length() && relative.charAt(index + 1) == '*' as char) { unlimited = true }
    }
    new GlobSpec(literal.toString(), relative, unlimited ? Integer.MAX_VALUE : segments, true)
  }
}
