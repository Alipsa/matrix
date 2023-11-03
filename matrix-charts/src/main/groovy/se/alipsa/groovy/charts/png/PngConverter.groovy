package se.alipsa.groovy.charts.png

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Scene
import javafx.scene.image.WritableImage
import se.alipsa.groovy.charts.Chart
import se.alipsa.groovy.charts.Plot

import javax.imageio.ImageIO
import java.util.concurrent.CountDownLatch

class PngConverter {

    /**
     * This implementation uses java fx to render the chart and take a snapshot image
     *
     * @param chart the chart to convert
     * @param out the output stream to save to
     * @param width the width of the png
     * @param height the height of the png
     * @throws IOException if an error occurs during writing
     */
    static void convert(Chart chart, OutputStream out, double width, double height) throws IOException {
        new JFXPanel()
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            var jfxChart = Plot.jfx(chart)
            Scene scene = new Scene(jfxChart, width, height)
            WritableImage snapshot = scene.snapshot(null)
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", out)
            countDownLatch.countDown()
        })
        try {
            countDownLatch.await()
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while saving the chart", e)
        }
    }
}
