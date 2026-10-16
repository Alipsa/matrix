package se.alipsa.matrix.arff

import groovy.transform.PackageScope

import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.StandardCharsets

/** Per-write encoding validator with allocation-free scans for the common charsets. */
@PackageScope
final class ArffEncoding {
  private final Charset charset
  private final CharsetEncoder encoder

  ArffEncoding(Charset charset) {
    this.charset = charset
    encoder = charset.newEncoder()
  }

  void check(CharSequence text, String subject) {
    if (!canEncode(text)) {
      fail(subject)
    }
  }

  void checkCell(CharSequence text, String path, int row) {
    if (!canEncode(text)) {
      fail("Column '$path' row $row contains characters that")
    }
  }

  private void fail(String subject) {
    throw new IllegalArgumentException("$subject cannot be encoded in ${charset.name()}")
  }

  private boolean canEncode(CharSequence text) {
    if (charset == StandardCharsets.UTF_8) {
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i)
        if (Character.isHighSurrogate(c)) {
          if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i))) {
            return false
          }
        } else if (Character.isLowSurrogate(c)) {
          return false
        }
      }
      return true
    }
    if (charset == StandardCharsets.ISO_8859_1 || charset == StandardCharsets.US_ASCII) {
      int limit = charset == StandardCharsets.ISO_8859_1 ? 0xFF : 0x7F
      for (int i = 0; i < text.length(); i++) {
        if (text.charAt(i) > limit) {
          return false
        }
      }
      return true
    }
    encoder.canEncode(text)
  }
}
