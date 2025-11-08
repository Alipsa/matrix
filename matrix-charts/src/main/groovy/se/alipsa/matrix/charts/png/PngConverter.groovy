package se.alipsa.matrix.charts.png

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Scene
import javafx.scene.chart.Chart as FxChart
import javafx.scene.image.WritableImage
import se.alipsa.matrix.charts.Plot
import se.alipsa.matrix.charts.Chart

import javax.imageio.ImageIO
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class PngConverter {

  /**
   * This implementation uses JavaFX to render the chart and take a snapshot image.
   *
   * @param chart the chart to convert
   * @param out the output stream to save to
   * @param width the width of the png
   * @param height the height of the png
   * @throws IOException if an error occurs during writing
   */
  static void convert(Chart chart, OutputStream out, double width, double height) throws IOException {
    new JFXPanel()
    CountDownLatch countDownLatch = new CountDownLatch(1)
    AtomicReference<Throwable> errorReference = new AtomicReference<>()
    Platform.runLater {
      try {
        FxChart jfxChart = Plot.jfx(chart)
        Scene scene = new Scene(jfxChart, width, height)
        WritableImage snapshot = scene.snapshot(null)
        ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", out)
      } catch (Throwable e) {
        errorReference.set(e)
      } finally {
        countDownLatch.countDown()
      }
    }
    try {
      countDownLatch.await()
      Throwable error = errorReference.get()
      if (error != null) {
        throw new IOException("Error while saving the chart", error)
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt()
      throw new IOException("Interrupted while saving the chart", e)
    }
  }
}
