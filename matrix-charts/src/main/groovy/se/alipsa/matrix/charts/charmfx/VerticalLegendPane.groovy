package se.alipsa.matrix.charts.charmfx

import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.layout.VBox

class VerticalLegendPane extends VBox implements LegendPane {

    VerticalLegendPane(){
        setSpacing(2)
        setPadding(new Insets(2, 3, 2, 3))
    }

    @Override
    ObservableList<Node> getLegendChildren() {
        getChildren()
    }

}
