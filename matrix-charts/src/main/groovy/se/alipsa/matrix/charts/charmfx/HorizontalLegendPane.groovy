package se.alipsa.matrix.charts.charmfx

import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.layout.HBox

class HorizontalLegendPane extends HBox implements LegendPane {

    HorizontalLegendPane(){
        setSpacing(2)
        setPadding(new Insets(2, 3, 2, 3))
    }

    @Override
    ObservableList<Node> getLegendChildren() {
        getChildren()
    }
}
