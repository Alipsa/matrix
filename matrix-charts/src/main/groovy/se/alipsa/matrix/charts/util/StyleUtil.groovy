package se.alipsa.matrix.charts.util

import javafx.scene.Node

class StyleUtil {

    static void addStyle(Node node, String style) {
        String existingStyle = node.getStyle().trim()
        String newStyle = style.endsWith(';') ? style : style + ';'
        if(!existingStyle.isEmpty()) {
            newStyle = (existingStyle.endsWith(';') ? existingStyle : existingStyle + ';') + newStyle
        }
        node.setStyle(newStyle)
    }
}
