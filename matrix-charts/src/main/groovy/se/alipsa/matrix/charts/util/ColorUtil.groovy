package se.alipsa.matrix.charts.util

import java.awt.Color

class ColorUtil {

  static String asHexString(Color color, String defaultIfNull='none') {
    if (color == null) {
      return defaultIfNull;
    }
    final String red = pad(Integer.toHexString(color.getRed()))
    final String green = pad(Integer.toHexString(color.getGreen()))
    final String blue = pad(Integer.toHexString(color.getBlue()))
    final String alpha = pad(Integer.toHexString(color.getAlpha()))
    return '#' + red + green + blue + alpha
  }

  static String asHexString(javafx.scene.paint.Color color) {
    final String red = pad(Integer.toHexString((int)Math.round(color.getRed()*255)))
    final String green = pad(Integer.toHexString((int)Math.round(color.getGreen()*255)))
    final String blue = pad(Integer.toHexString((int)Math.round(color.getBlue()*255)))
    final String alpha = pad(Integer.toHexString((int)Math.round(color.getOpacity()*255)))
    return '#' + red + green + blue + alpha
  }

  private static String pad(final String hex) {
    return (hex.length() == 1) ? "0" + hex : hex;
  }
}
