package se.alipsa.matrix.charts.charm

import javafx.geometry.Insets
import javafx.scene.layout.VBox

class VerticalLegendPane extends VBox implements LegendPane {

    VerticalLegendPane(){
        setSpacing(2)
        setPadding(new Insets(2, 3, 2, 3))
    }


}
