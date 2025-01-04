package se.alipsa.matrix.charts.charm

import javafx.geometry.Insets
import javafx.scene.layout.*

class HorizontalLegendPane extends HBox implements LegendPane {

    HorizontalLegendPane(){
        setSpacing(2)
        setPadding(new Insets(2, 3, 2, 3))
    }
}
