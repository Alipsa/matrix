package se.alipsa.matrix.charm.util

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

    static String asHexString(Object color) {
        return asHexString(color, 'none')
    }

    static String asHexString(Object color, String defaultIfNull) {
        if (color == null) {
            return defaultIfNull
        }
        if (color instanceof Color) {
            return asHexString(color as Color, defaultIfNull)
        }
        if (color.getClass().name == 'javafx.scene.paint.Color') {
            try {
                double redValue = color.getClass().getMethod('getRed').invoke(color) as double
                double greenValue = color.getClass().getMethod('getGreen').invoke(color) as double
                double blueValue = color.getClass().getMethod('getBlue').invoke(color) as double
                double alphaValue = color.getClass().getMethod('getOpacity').invoke(color) as double
                final String red = pad(Integer.toHexString((redValue * 255).round() as int))
                final String green = pad(Integer.toHexString((greenValue * 255).round() as int))
                final String blue = pad(Integer.toHexString((blueValue * 255).round() as int))
                final String alpha = pad(Integer.toHexString((alphaValue * 255).round() as int))
                return '#' + red + green + blue + alpha
            } catch (ReflectiveOperationException ignored) {
                return defaultIfNull
            }
        }
        return defaultIfNull
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
            pct = 0.max(pct.min(100)) as int
            int value = (255 * (pct / 100.0f)).round() as int
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
