package se.alipsa.matrix.charm.sf

import groovy.transform.CompileStatic

import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.Locale

/**
 * Minimal WKT reader for Simple Features geometries.
 * Supports POINT, LINESTRING, POLYGON, and MULTI* variants with optional SRID prefix.
 */
@CompileStatic
class WktReader {

  private static final Pattern SRID_PATTERN =
      Pattern.compile('^\\s*SRID\\s*=\\s*(\\d+)\\s*;\\s*(.*)$', Pattern.CASE_INSENSITIVE | Pattern.DOTALL)

  /**
   * Parse a WKT string into an {@link SfGeometry}.
   *
   * @param wkt the WKT string (optionally prefixed with SRID=####;)
   * @return parsed geometry
   */
  static SfGeometry parse(String wkt) {
    if (wkt == null || wkt.trim().isEmpty()) {
      throw new IllegalArgumentException('WKT must not be blank')
    }

    Integer srid = null
    String input = wkt
    Matcher sridMatcher = SRID_PATTERN.matcher(input)
    if (sridMatcher.matches()) {
      srid = Integer.parseInt(sridMatcher.group(1))
      input = sridMatcher.group(2)
    }

    WktTokenizer tokenizer = new WktTokenizer(input)
    String typeName = tokenizer.readWord('geometry type')
    SfType type = toType(typeName)
    tokenizer.skipDimensions()

    if (tokenizer.matchWord('EMPTY')) {
      return new SfGeometry(type, [], srid, true)
    }

    SfGeometry geometry = parseGeometry(tokenizer, type, srid)
    if (tokenizer.hasNext()) {
      throw new IllegalArgumentException("Unexpected token '${tokenizer.peek()?.text}' after geometry")
    }
    return geometry
  }

  private static SfGeometry parseGeometry(WktTokenizer tokenizer, SfType type, Integer srid) {
    switch (type) {
      case SfType.POINT:
        return parsePointGeometry(tokenizer, type, srid)
      case SfType.LINESTRING:
        return parseLineStringGeometry(tokenizer, type, srid)
      case SfType.POLYGON:
        return parsePolygonGeometry(tokenizer, type, srid)
      case SfType.MULTIPOINT:
        return parseMultiPointGeometry(tokenizer, type, srid)
      case SfType.MULTILINESTRING:
        return parseMultiLineStringGeometry(tokenizer, type, srid)
      case SfType.MULTIPOLYGON:
        return parseMultiPolygonGeometry(tokenizer, type, srid)
      case SfType.GEOMETRYCOLLECTION:
        return parseGeometryCollectionGeometry(tokenizer, type, srid)
      default:
        throw new IllegalArgumentException("Unsupported geometry type: $type")
    }
  }

  private static SfGeometry parsePointGeometry(WktTokenizer tokenizer, SfType type, Integer srid) {
    tokenizer.expect(TokenType.LPAREN)
    SfPoint point = parsePoint(tokenizer)
    tokenizer.expect(TokenType.RPAREN)
    SfShape shape = new SfShape(type, [new SfRing([point])])
    return new SfGeometry(type, [shape], srid, false)
  }

  private static SfGeometry parseLineStringGeometry(WktTokenizer tokenizer, SfType type, Integer srid) {
    tokenizer.expect(TokenType.LPAREN)
    List<SfPoint> points = parsePointList(tokenizer)
    tokenizer.expect(TokenType.RPAREN)
    SfShape shape = new SfShape(type, [new SfRing(points)])
    return new SfGeometry(type, [shape], srid, false)
  }

  private static SfGeometry parsePolygonGeometry(WktTokenizer tokenizer, SfType type, Integer srid) {
    tokenizer.expect(TokenType.LPAREN)
    List<SfRing> rings = []
    rings << parseRing(tokenizer, false)
    while (tokenizer.match(TokenType.COMMA)) {
      rings << parseRing(tokenizer, true)
    }
    tokenizer.expect(TokenType.RPAREN)
    SfShape shape = new SfShape(type, rings)
    return new SfGeometry(type, [shape], srid, false)
  }

  private static SfGeometry parseMultiPointGeometry(WktTokenizer tokenizer, SfType type, Integer srid) {
    tokenizer.expect(TokenType.LPAREN)
    List<SfShape> shapes = []
    if (tokenizer.peekType() == TokenType.LPAREN) {
      shapes << parsePointShapeWithParens(tokenizer)
      while (tokenizer.match(TokenType.COMMA)) {
        shapes << parsePointShapeWithParens(tokenizer)
      }
    } else {
      shapes << parsePointShape(tokenizer)
      while (tokenizer.match(TokenType.COMMA)) {
        shapes << parsePointShape(tokenizer)
      }
    }
    tokenizer.expect(TokenType.RPAREN)
    return new SfGeometry(type, shapes, srid, false)
  }

  private static SfGeometry parseMultiLineStringGeometry(WktTokenizer tokenizer, SfType type, Integer srid) {
    tokenizer.expect(TokenType.LPAREN)
    List<SfShape> shapes = []
    shapes << parseLineStringShape(tokenizer)
    while (tokenizer.match(TokenType.COMMA)) {
      shapes << parseLineStringShape(tokenizer)
    }
    tokenizer.expect(TokenType.RPAREN)
    return new SfGeometry(type, shapes, srid, false)
  }

  private static SfGeometry parseMultiPolygonGeometry(WktTokenizer tokenizer, SfType type, Integer srid) {
    tokenizer.expect(TokenType.LPAREN)
    List<SfShape> shapes = []
    shapes << parsePolygonShape(tokenizer)
    while (tokenizer.match(TokenType.COMMA)) {
      shapes << parsePolygonShape(tokenizer)
    }
    tokenizer.expect(TokenType.RPAREN)
    return new SfGeometry(type, shapes, srid, false)
  }

  private static SfGeometry parseGeometryCollectionGeometry(WktTokenizer tokenizer, SfType type, Integer srid) {
    tokenizer.expect(TokenType.LPAREN)
    List<SfShape> shapes = []
    if (tokenizer.matchWord('EMPTY')) {
      tokenizer.expect(TokenType.RPAREN)
      return new SfGeometry(type, [], srid, true)
    }

    SfGeometry first = parseSubGeometry(tokenizer, srid)
    if (first != null) {
      shapes.addAll(first.shapes)
    }
    while (tokenizer.match(TokenType.COMMA)) {
      SfGeometry nextGeom = parseSubGeometry(tokenizer, srid)
      if (nextGeom != null) {
        shapes.addAll(nextGeom.shapes)
      }
    }
    tokenizer.expect(TokenType.RPAREN)
    return new SfGeometry(type, shapes, srid, shapes.isEmpty())
  }

  private static SfGeometry parseSubGeometry(WktTokenizer tokenizer, Integer srid) {
    String typeName = tokenizer.readWord('geometry type')
    SfType type = toType(typeName)
    tokenizer.skipDimensions()
    if (tokenizer.matchWord('EMPTY')) {
      return new SfGeometry(type, [], srid, true)
    }
    SfGeometry geometry = parseGeometry(tokenizer, type, srid)
    if (geometry.type == SfType.GEOMETRYCOLLECTION) {
      return geometry
    }
    List<SfShape> typed = geometry.shapes.collect { shape ->
      shape.type == null ? new SfShape(type, shape.rings) : shape
    }
    return new SfGeometry(type, typed, srid, geometry.empty)
  }

  private static SfShape parsePointShapeWithParens(WktTokenizer tokenizer) {
    tokenizer.expect(TokenType.LPAREN)
    SfPoint point = parsePoint(tokenizer)
    tokenizer.expect(TokenType.RPAREN)
    return new SfShape(SfType.POINT, [new SfRing([point])])
  }

  private static SfShape parsePointShape(WktTokenizer tokenizer) {
    SfPoint point = parsePoint(tokenizer)
    return new SfShape(SfType.POINT, [new SfRing([point])])
  }

  private static SfShape parseLineStringShape(WktTokenizer tokenizer) {
    tokenizer.expect(TokenType.LPAREN)
    List<SfPoint> points = parsePointList(tokenizer)
    tokenizer.expect(TokenType.RPAREN)
    return new SfShape(SfType.LINESTRING, [new SfRing(points)])
  }

  private static SfShape parsePolygonShape(WktTokenizer tokenizer) {
    tokenizer.expect(TokenType.LPAREN)
    List<SfRing> rings = []
    rings << parseRing(tokenizer, false)
    while (tokenizer.match(TokenType.COMMA)) {
      rings << parseRing(tokenizer, true)
    }
    tokenizer.expect(TokenType.RPAREN)
    return new SfShape(SfType.POLYGON, rings)
  }

  private static SfRing parseRing(WktTokenizer tokenizer, boolean hole) {
    tokenizer.expect(TokenType.LPAREN)
    List<SfPoint> points = parsePointList(tokenizer)
    tokenizer.expect(TokenType.RPAREN)
    return new SfRing(points, hole)
  }

  private static List<SfPoint> parsePointList(WktTokenizer tokenizer) {
    List<SfPoint> points = []
    points << parsePoint(tokenizer)
    while (tokenizer.match(TokenType.COMMA)) {
      points << parsePoint(tokenizer)
    }
    return points
  }

  private static SfPoint parsePoint(WktTokenizer tokenizer) {
    BigDecimal x = tokenizer.readNumber('x')
    BigDecimal y = tokenizer.readNumber('y')
    tokenizer.skipExtraNumbers()
    return new SfPoint(x, y)
  }

  private static SfType toType(String typeName) {
    if (typeName == null) {
      throw new IllegalArgumentException('Geometry type is missing')
    }
    switch (typeName.toUpperCase(Locale.ROOT)) {
      case 'POINT': return SfType.POINT
      case 'LINESTRING': return SfType.LINESTRING
      case 'POLYGON': return SfType.POLYGON
      case 'MULTIPOINT': return SfType.MULTIPOINT
      case 'MULTILINESTRING': return SfType.MULTILINESTRING
      case 'MULTIPOLYGON': return SfType.MULTIPOLYGON
      case 'GEOMETRYCOLLECTION': return SfType.GEOMETRYCOLLECTION
      default:
        throw new IllegalArgumentException("Unsupported geometry type: $typeName")
    }
  }

  private enum TokenType {
    WORD,
    NUMBER,
    LPAREN,
    RPAREN,
    COMMA
  }

  private static class Token {
    final TokenType type
    final String text

    Token(TokenType type, String text) {
      this.type = type
      this.text = text
    }
  }

  private static class WktTokenizer {
    private static final Pattern NUMBER_PATTERN =
        Pattern.compile('[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?')
    private static final Pattern WORD_PATTERN = Pattern.compile('[A-Za-z_]+')

    private final String input
    private int index = 0
    private Token lookahead

    WktTokenizer(String input) {
      this.input = input ?: ''
    }

    Token peek() {
      if (lookahead == null) {
        lookahead = nextToken()
      }
      return lookahead
    }

    TokenType peekType() {
      return peek()?.type
    }

    boolean hasNext() {
      return peek() != null
    }

    Token next() {
      Token token = peek()
      lookahead = null
      return token
    }

    boolean match(TokenType type) {
      if (peek()?.type == type) {
        next()
        return true
      }
      return false
    }

    boolean matchWord(String value) {
      Token token = peek()
      if (token?.type == TokenType.WORD && token.text.equalsIgnoreCase(value)) {
        next()
        return true
      }
      return false
    }

    String readWord(String context) {
      Token token = next()
      if (token == null || token.type != TokenType.WORD) {
        throw new IllegalArgumentException("Expected $context, found ${token?.text ?: 'end of input'}")
      }
      return token.text
    }

    BigDecimal readNumber(String context) {
      Token token = next()
      if (token == null || token.type != TokenType.NUMBER) {
        throw new IllegalArgumentException("Expected $context coordinate, found ${token?.text ?: 'end of input'}")
      }
      return new BigDecimal(token.text)
    }

    void expect(TokenType type) {
      Token token = next()
      if (token == null || token.type != type) {
        throw new IllegalArgumentException("Expected ${type.name().toLowerCase()}, found ${token?.text ?: 'end of input'}")
      }
    }

    void skipDimensions() {
      Token token = peek()
      if (token?.type == TokenType.WORD) {
        String dim = token.text.toUpperCase(Locale.ROOT)
        if (dim == 'Z' || dim == 'M' || dim == 'ZM') {
          next()
        }
      }
    }

    void skipExtraNumbers() {
      while (peek()?.type == TokenType.NUMBER) {
        next()
      }
    }

    private Token nextToken() {
      skipWhitespace()
      if (index >= input.length()) {
        return null
      }
      char ch = input.charAt(index)
      switch (ch) {
        case '(':
          index++
          return new Token(TokenType.LPAREN, '(')
        case ')':
          index++
          return new Token(TokenType.RPAREN, ')')
        case ',':
          index++
          return new Token(TokenType.COMMA, ',')
        default:
          break
      }

      if (Character.isLetter(ch)) {
        Matcher matcher = WORD_PATTERN.matcher(input)
        matcher.region(index, input.length())
        if (matcher.lookingAt()) {
          String text = matcher.group()
          index = matcher.end()
          return new Token(TokenType.WORD, text)
        }
      }

      Matcher numberMatcher = NUMBER_PATTERN.matcher(input)
      numberMatcher.region(index, input.length())
      if (numberMatcher.lookingAt()) {
        String text = numberMatcher.group()
        index = numberMatcher.end()
        return new Token(TokenType.NUMBER, text)
      }

      throw new IllegalArgumentException("Unexpected character '${ch}' at position $index")
    }

    private void skipWhitespace() {
      while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
        index++
      }
    }
  }
}
