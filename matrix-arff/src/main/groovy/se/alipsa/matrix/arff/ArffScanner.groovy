package se.alipsa.matrix.arff

import groovy.transform.PackageScope

/**
 * Quote-aware scanning helpers shared by the ARFF reader: decoding a quoted token and locating characters that sit
 * outside single- or double-quoted tokens. All lookups run the same private scan so there is exactly one quote/escape
 * state machine for these operations.
 */
@PackageScope
final class ArffScanner {

  private static final char BACKSLASH_CHAR = '\\'
  private static final char SINGLE_QUOTE_CHAR = '\''
  private static final char DOUBLE_QUOTE_CHAR = '"'

  private ArffScanner() {
    // Utility class
  }

  /** A decoded quoted token plus the index just past its closing quote. */
  @PackageScope
  static final class QuotedToken {
    final String value
    final int end

    QuotedToken(String value, int end) {
      this.value = value
      this.end = end
    }
  }

  /** Result of one scan: the indexes of the target character outside quotes, and whether a quote was left open. */
  private static final class Scan {
    final List<Integer> matches
    final boolean unterminatedQuote

    Scan(List<Integer> matches, boolean unterminatedQuote) {
      this.matches = matches
      this.unterminatedQuote = unterminatedQuote
    }
  }

  @PackageScope
  static boolean isQuoteChar(char c) {
    c == SINGLE_QUOTE_CHAR || c == DOUBLE_QUOTE_CHAR
  }

  /**
   * Read a quoted token starting at {@code start} (which must hold the opening quote), decoding the escape set emitted
   * by Weka's writer. Returns null when the closing quote is missing.
   */
  @PackageScope
  static QuotedToken readQuotedToken(String text, int start) {
    char quoteChar = text.charAt(start)
    StringBuilder sb = new StringBuilder()
    boolean escape = false
    for (int i = start + 1; i < text.length(); i++) {
      char c = text.charAt(i)
      if (escape) {
        sb.append(ArffEscapes.unescape(c))
        escape = false
        continue
      }
      if (c == BACKSLASH_CHAR) {
        escape = true
        continue
      }
      if (c == quoteChar) {
        return new QuotedToken(sb.toString(), i + 1)
      }
      sb.append(c)
    }
    null
  }

  /**
   * Index of the first {@code target} at or after {@code from} that is outside a quoted token, or -1. Quote state is
   * tracked from the start of {@code text}, so {@code from} is only a lower bound for the returned index.
   */
  @PackageScope
  static int indexOfOutsideQuotes(String text, char target, int from) {
    Integer match = scan(text, target).matches.find { Integer index -> index >= from }
    match == null ? -1 : match
  }

  /** Indexes of every {@code target} outside quoted tokens, in encounter order. */
  @PackageScope
  static List<Integer> indexesOutsideQuotes(String text, char target) {
    scan(text, target).matches
  }

  /** Index of the last {@code target} outside a quoted token, or -1. */
  @PackageScope
  static int lastIndexOfOutsideQuotes(String text, char target) {
    List<Integer> matches = scan(text, target).matches
    matches.isEmpty() ? -1 : matches.last()
  }

  /** Whether {@code text} opens a quoted token that is never closed. */
  @PackageScope
  static boolean hasUnterminatedQuote(String text) {
    scan(text, SINGLE_QUOTE_CHAR).unterminatedQuote
  }

  private static Scan scan(String text, char target) {
    List<Integer> matches = []
    boolean inQuote = false
    boolean escape = false
    char quoteChar = 0
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i)
      if (inQuote) {
        if (escape) {
          escape = false
        } else if (c == BACKSLASH_CHAR) {
          escape = true
        } else if (c == quoteChar) {
          inQuote = false
        }
        continue
      }
      if (isQuoteChar(c)) {
        inQuote = true
        quoteChar = c
        continue
      }
      if (c == target) {
        matches.add(i)
      }
    }
    new Scan(matches, inQuote)
  }

}
