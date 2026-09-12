package se.alipsa.matrix.arff

/**
 * Escaping and quoting rules shared by {@link MatrixArffReader} and {@link MatrixArffWriter}.
 *
 * The rules are a compatible superset of Weka's {@code weka.core.Utils.quote} / {@code backQuoteChars} and match the
 * escape set emitted by Weka's writer, so Weka-written values using that set decode identically:
 * <ul>
 *   <li>{@code \ ' " \t \n \r %} are written as {@code \\ \' \" \t \n \r \%}</li>
 *   <li>a value is wrapped in single quotes when it contains any of those characters, any character at or below the
 *       space character (Weka only checks for the space itself, but its tokenizer treats every such character as
 *       whitespace), {@code { } ,} or {@code ?}, or when it is empty</li>
 *   <li>U+001E, which Weka escapes as the six-character text "backslash u001E", is written unchanged: Weka's reader
 *       uses {@code java.io.StreamTokenizer}, which would decode that escape to the literal text {@code u001E}</li>
 *   <li>when reading, {@code \n}, {@code \t} and {@code \r} decode to the control character; any other escaped character
 *       decodes to itself</li>
 * </ul>
 */
final class ArffEscapes {

  private static final char BACKSLASH = '\\'
  private static final char SINGLE_QUOTE = '\''
  private static final char DOUBLE_QUOTE = '"'
  private static final char TAB = '\t'
  private static final char NEWLINE = '\n'
  private static final char CARRIAGE_RETURN = '\r'
  private static final char PERCENT = '%'
  private static final char COMMA = ','
  private static final char OPEN_BRACE = '{'
  private static final char CLOSE_BRACE = '}'
  private static final char SPACE = ' '
  private static final char ESCAPED_TAB = 't'
  private static final char ESCAPED_NEWLINE = 'n'
  private static final char ESCAPED_CARRIAGE_RETURN = 'r'
  private static final String QUESTION_MARK = '?'
  private static final String QUOTE = "'"

  private ArffEscapes() {
    // Utility class
  }

  /**
   * Backslash-escape the characters Weka escapes inside quoted ARFF tokens.
   *
   * @param value the raw value, never null
   * @return the escaped value (no surrounding quotes)
   */
  static String escape(String value) {
    StringBuilder sb = new StringBuilder(value.length() + 8)
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i)
      switch (c) {
        case BACKSLASH -> sb.append(BACKSLASH).append(BACKSLASH)
        case SINGLE_QUOTE -> sb.append(BACKSLASH).append(SINGLE_QUOTE)
        case DOUBLE_QUOTE -> sb.append(BACKSLASH).append(DOUBLE_QUOTE)
        case TAB -> sb.append(BACKSLASH).append(ESCAPED_TAB)
        case NEWLINE -> sb.append(BACKSLASH).append(ESCAPED_NEWLINE)
        case CARRIAGE_RETURN -> sb.append(BACKSLASH).append(ESCAPED_CARRIAGE_RETURN)
        case PERCENT -> sb.append(BACKSLASH).append(PERCENT)
        default -> sb.append(c)
      }
    }
    sb.toString()
  }

  /**
   * Decode the character that followed a backslash inside a quoted ARFF token.
   *
   * @param escaped the character after the backslash
   * @return the decoded character: tab, newline or carriage return for {@code t}, {@code n}, {@code r}; otherwise the
   *         character itself (so {@code \'}, {@code \"}, {@code \\} and {@code \%} decode to the literal character)
   */
  static char unescape(char escaped) {
    switch (escaped) {
      case ESCAPED_TAB -> TAB
      case ESCAPED_NEWLINE -> NEWLINE
      case ESCAPED_CARRIAGE_RETURN -> CARRIAGE_RETURN
      default -> escaped
    }
  }

  /**
   * Whether Weka's {@code Utils.quote} would wrap the value in single quotes.
   *
   * @param value the raw value, never null
   * @return true when the value is empty, is {@code ?}, or contains a character that needs quoting
   */
  static boolean needsQuoting(String value) {
    if (value.isEmpty() || value == QUESTION_MARK) {
      return true
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i)
      if (c <= SPACE || c == BACKSLASH || c == SINGLE_QUOTE || c == DOUBLE_QUOTE || c == PERCENT
          || c == COMMA || c == OPEN_BRACE || c == CLOSE_BRACE) {
        return true
      }
    }
    false
  }

  /**
   * Escape the value and wrap it in single quotes.
   *
   * @param value the raw value, never null
   * @return the quoted, escaped token
   */
  static String quote(String value) {
    QUOTE + escape(value) + QUOTE
  }

  /**
   * Quote the value only when {@link #needsQuoting(String)} says so, as Weka's {@code Utils.quote} does.
   *
   * @param value the raw value, never null
   * @return the value unchanged, or quoted and escaped
   */
  static String quoteIfNeeded(String value) {
    needsQuoting(value) ? quote(value) : value
  }

}
