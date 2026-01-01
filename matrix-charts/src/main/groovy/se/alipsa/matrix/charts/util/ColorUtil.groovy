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

    static String normalizeColor(String color) {
        if (color == null) {
            return null
        }
        String trimmed = color.trim()
        if (trimmed.isEmpty()) {
            return color
        }
        def matcher = trimmed =~ /(?i)gr[ae]y(\d{1,3})/
        if (matcher.matches()) {
            int pct = Integer.parseInt(matcher.group(1))
            pct = Math.max(0, Math.min(100, pct))
            int value = Math.round(255 * (pct / 100.0f))
            String hex = pad(Integer.toHexString(value))
            return "#${hex}${hex}${hex}"
        }
        if (trimmed.equalsIgnoreCase('grey') || trimmed.equalsIgnoreCase('gray')) {
            return '#808080'
        }
        return color
    }

    private static String pad(final String hex) {
        return (hex.length() == 1) ? "0" + hex : hex;
    }
}
