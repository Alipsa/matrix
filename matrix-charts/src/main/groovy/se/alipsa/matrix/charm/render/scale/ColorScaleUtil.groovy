package se.alipsa.matrix.charm.render.scale

import java.math.RoundingMode
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Utilities for color scale interpolation.
 */
@SuppressWarnings('DuplicateListLiteral')
@SuppressWarnings('DuplicateNumberLiteral')
@SuppressWarnings('IfStatementBraces')
class ColorScaleUtil {

  private static final int NEUTRAL_COMPONENT = 128
  private static final BigDecimal OPAQUE = 1.0
  private static final BigDecimal BYTE_MAX = 255
  private static final String HASH = '#'
  private static final String PERCENT = '%'
  private static final int[] NO_COLOR = [] as int[]
  private static final Pattern RGB_FUNCTION = Pattern.compile(
      '(?i)^\\s*(rgb|rgba)\\(\\s*([^,]+)\\s*,\\s*([^,]+)\\s*,\\s*([^,]+)(?:\\s*,\\s*([^,]+))?\\s*\\)\\s*$'
  )

  /**
   * The default discrete colours Charm assigns to scale levels when no palette is
   * configured. Level {@code i} uses {@code DEFAULT_COLORS[i % size]}.
   */
  static final List<String> DEFAULT_COLORS = [
      '#1f77b4', '#d62728', '#2ca02c', '#ff7f0e', '#9467bd',
      '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf'
  ].asImmutable()

  /**
   * Returns Charm's default discrete palette for the requested number of levels.
   *
   * @param n number of levels; zero or less returns an empty list
   * @return colours in level order, wrapping around the default palette
   */
  static List<String> defaultPalette(int n) {
    n <= 0 ? [] : (0..<n).collect { int i -> DEFAULT_COLORS[i % DEFAULT_COLORS.size()] }
  }

  /**
   * Returns black or white text that contrasts with a colour's perceived luminance.
   *
   * <p>Transparent colours are composited over white. Use
   * {@link #contrastTextColor(String, String)} to select a different background.</p>
   *
   * @param color background colour
   * @return {@code #000000} for light backgrounds, otherwise {@code #ffffff}
   */
  static String contrastTextColor(String color) {
    contrastTextColor(color, '#ffffff')
  }

  /**
   * Returns black or white text that contrasts with a colour composited over a background.
   *
   * <p>Supports {@code #RGB}, {@code #RGBA}, {@code #RRGGBB}, {@code #RRGGBBAA},
   * {@code rgb(...)}, {@code rgba(...)}, and the named colours supported by
   * {@link #parseColor(String)}. Unsupported values are treated as neutral gray.</p>
   *
   * @param color foreground/background fill colour
   * @param background opaque colour behind transparent {@code color}
   * @return {@code #000000} for light backgrounds, otherwise {@code #ffffff}
   */
  static String contrastTextColor(String color, String background) {
    ParsedColor foreground = parseColorValue(color)
    ParsedColor backdrop = parseColorValue(background)
    int red = composite(foreground.red, backdrop.red, foreground.alpha)
    int green = composite(foreground.green, backdrop.green, foreground.alpha)
    int blue = composite(foreground.blue, backdrop.blue, foreground.alpha)
    int luminance = (red * 299 + green * 587 + blue * 114).intdiv(1000)
    luminance >= 128 ? '#000000' : '#ffffff'
  }

  /**
   * Interpolate between two colors.
   *
   * @param color1 start color (hex string or named)
   * @param color2 end color (hex string or named)
   * @param t interpolation factor (0-1)
   * @return interpolated color as hex string
   */
  static String interpolateColor(String color1, String color2, BigDecimal t) {
    int[] rgb1 = parseColor(color1)
    int[] rgb2 = parseColor(color2)

    int r = (rgb1[0] + t * (rgb2[0] - rgb1[0])).round() as int
    int g = (rgb1[1] + t * (rgb2[1] - rgb1[1])).round() as int
    int b = (rgb1[2] + t * (rgb2[2] - rgb1[2])).round() as int

    r = 0.max(r.min(255)) as int
    g = 0.max(g.min(255)) as int
    b = 0.max(b.min(255)) as int

    String.format('#%02X%02X%02X', r, g, b)
  }

  /**
   * Parse a color string to RGB values.
   * Supports {@code #RGB}, {@code #RGBA}, {@code #RRGGBB}, {@code #RRGGBBAA},
   * {@code rgb(...)}, {@code rgba(...)}, and named colors. Alpha is not included in the returned
   * RGB values. Unsupported values return neutral gray.
   *
   * @param color color string
   * @return RGB array
   */
  static int[] parseColor(String color) {
    int[] opaqueHex = parseOpaqueHexColor(color)
    if (opaqueHex.length != 0) {
      return opaqueHex
    }
    ParsedColor parsed = parseColorValue(color)
    [parsed.red, parsed.green, parsed.blue] as int[]
  }

  private static int[] parseOpaqueHexColor(String color) {
    String value = color?.trim()
    if (value == null || value.length() != 7 || !value.startsWith(HASH)) {
      return NO_COLOR
    }
    try {
      [
          Integer.parseInt(value.substring(1, 3), 16),
          Integer.parseInt(value.substring(3, 5), 16),
          Integer.parseInt(value.substring(5, 7), 16)
      ] as int[]
    } catch (NumberFormatException ignored) {
      NO_COLOR
    }
  }

  private static ParsedColor parseColorValue(String color) {
    String value = color?.trim()
    if (value == null || value.isEmpty()) {
      return neutral()
    }
    if (value.startsWith(HASH)) {
      ParsedColor parsed = parseHexColor(value.substring(1))
      if (parsed != null) {
        return parsed
      }
    }
    ParsedColor functional = parseRgbFunction(value)
    if (functional != null) {
      return functional
    }
    int[] named = NAMED_COLORS[value.toLowerCase()]
    named == null ? neutral() : new ParsedColor(named[0], named[1], named[2], OPAQUE)
  }

  private static ParsedColor parseHexColor(String hex) {
    try {
      switch (hex.length()) {
        case 3 -> new ParsedColor(expandHexDigit(hex, 0), expandHexDigit(hex, 1), expandHexDigit(hex, 2), OPAQUE)
        case 4 -> new ParsedColor(expandHexDigit(hex, 0), expandHexDigit(hex, 1), expandHexDigit(hex, 2), alphaFromByte(expandHexDigit(hex, 3)))
        case 6 -> new ParsedColor(hexByte(hex, 0), hexByte(hex, 2), hexByte(hex, 4), OPAQUE)
        case 8 -> new ParsedColor(hexByte(hex, 0), hexByte(hex, 2), hexByte(hex, 4), alphaFromByte(hexByte(hex, 6)))
        default -> null
      }
    } catch (NumberFormatException ignored) {
      null
    }
  }

  private static ParsedColor parseRgbFunction(String value) {
    Matcher matcher = RGB_FUNCTION.matcher(value)
    if (!matcher.matches()) {
      return null
    }
    Integer red = parseRgbComponent(matcher.group(2))
    Integer green = parseRgbComponent(matcher.group(3))
    Integer blue = parseRgbComponent(matcher.group(4))
    BigDecimal alpha = matcher.group(5) == null ? OPAQUE : parseAlpha(matcher.group(5))
    if (red == null || green == null || blue == null || alpha == null) {
      return null
    }
    new ParsedColor(red, green, blue, alpha)
  }

  private static Integer parseRgbComponent(String value) {
    try {
      String component = value.trim()
      BigDecimal decimal = component.endsWith(PERCENT)
          ? new BigDecimal(component.substring(0, component.length() - 1)) * BYTE_MAX / 100
          : new BigDecimal(component)
      int rounded = decimal.setScale(0, RoundingMode.HALF_UP).intValue()
      0.max(rounded.min(BYTE_MAX.intValue())) as int
    } catch (NumberFormatException ignored) {
      null
    }
  }

  private static BigDecimal parseAlpha(String value) {
    try {
      String component = value.trim()
      BigDecimal alpha = component.endsWith(PERCENT)
          ? new BigDecimal(component.substring(0, component.length() - 1)) / 100
          : new BigDecimal(component)
      alpha.min(OPAQUE).max(BigDecimal.ZERO)
    } catch (NumberFormatException ignored) {
      null
    }
  }

  private static int expandHexDigit(String hex, int index) {
    Integer.parseInt("${hex[index]}${hex[index]}", 16)
  }

  private static int hexByte(String hex, int start) {
    Integer.parseInt(hex.substring(start, start + 2), 16)
  }

  private static BigDecimal alphaFromByte(int alpha) {
    new BigDecimal(alpha).divide(BYTE_MAX, 8, RoundingMode.HALF_UP)
  }

  private static int composite(int foreground, int background, BigDecimal alpha) {
    BigDecimal composited = foreground * alpha + background * (OPAQUE - alpha)
    composited.setScale(0, RoundingMode.HALF_UP).intValue()
  }

  private static ParsedColor neutral() {
    new ParsedColor(NEUTRAL_COMPONENT, NEUTRAL_COMPONENT, NEUTRAL_COMPONENT, OPAQUE)
  }

  private static final Map<String, int[]> NAMED_COLORS = [
      'white': [255, 255, 255] as int[],
      'black': [0, 0, 0] as int[],
      'red': [255, 0, 0] as int[],
      'green': [0, 128, 0] as int[],
      'blue': [0, 0, 255] as int[],
      'yellow': [255, 255, 0] as int[],
      'cyan': [0, 255, 255] as int[],
      'magenta': [255, 0, 255] as int[],
      'orange': [255, 165, 0] as int[],
      'purple': [128, 0, 128] as int[],
      'pink': [255, 192, 203] as int[],
      'grey': [128, 128, 128] as int[],
      'gray': [128, 128, 128] as int[],
      'grey50': [128, 128, 128] as int[],
      'gray50': [128, 128, 128] as int[],
      'darkblue': [0, 0, 139] as int[],
      'lightblue': [173, 216, 230] as int[],
      'darkgreen': [0, 100, 0] as int[],
      'lightgreen': [144, 238, 144] as int[],
      'darkred': [139, 0, 0] as int[],
      'steelblue': [70, 130, 180] as int[],
      'navy': [0, 0, 128] as int[],
      'maroon': [128, 0, 0] as int[],
      'olive': [128, 128, 0] as int[],
      'teal': [0, 128, 128] as int[]
  ]

  private static class ParsedColor {
    final int red
    final int green
    final int blue
    final BigDecimal alpha

    ParsedColor(int red, int green, int blue, BigDecimal alpha) {
      this.red = red
      this.green = green
      this.blue = blue
      this.alpha = alpha
    }
  }

}
