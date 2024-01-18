import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.paint.Color
import javafx.stage.Modality
import se.alipsa.groovy.charts.charm.*
import javafx.scene.shape.Rectangle


private static void show(Node node, String title) {

    Platform.runLater {
        Alert alert = new Alert(Alert.AlertType.INFORMATION)
        alert.setHeaderText(null)
        alert.setContentText(null)
        alert.setTitle(title)
        alert.getDialogPane().setContent(node)
        alert.initModality(Modality.NONE)
        alert.setResizable(true)
        alert.showAndWait()
    }
}

new JFXPanel()
CharmChartFx chart = new CharmChartFx()
chart.addTitle("Hello world top", Position.TOP_CENTER)
chart.addTitle("Hello world bottom", Position.BOTTOM_CENTER)
chart.addLegend('Orange': Color.ORANGE, 'Blue': Color.BLUE, Position.RIGHT_CENTER)
        .setBackground(Color.LIGHTBLUE)
        .setBorder(Color.RED)
        //.setPrefWrapLength(50)

chart.addLegend('Red': Color.RED, 'Green': Color.GREEN, 'Yellow': Color.YELLOW, Position.BOTTOM_LEFT)
        .setBackground(Color.LIGHTGRAY)
        .setBorder(Color.BROWN)
        //.setPrefWrapLength(100)

PlotPane plotPane = new PlotPane(210,210)
Rectangle rect = new Rectangle(5, 5, 200, 200);
rect.setFill(Color.BLUE);
def gc = plotPane.getGraphicsContext2D()
gc.setFill(rect.getFill())
gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight())

chart.add(plotPane)
show(chart, "Charm chart")