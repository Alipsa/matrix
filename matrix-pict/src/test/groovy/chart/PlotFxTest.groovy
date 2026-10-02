package chart

import static org.junit.jupiter.api.Assertions.*

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.junit.jupiter.api.Test

/** Verifies that the JavaFX-only API rejects incorrect targets under static compilation. */
class PlotFxTest {

  @Test
  void typedExportsRejectIntegerAssignment() {
    ['PlotFx.jfx(chart)', 'PlotFx.jfx(chart, 400, 300)'].each { String call ->
      try (GroovyClassLoader loader = new GroovyClassLoader(getClass().classLoader)) {
        def error = assertThrows(MultipleCompilationErrorsException) {
          loader.parseClass("""
            import groovy.transform.CompileStatic
            import se.alipsa.matrix.pict.Chart
            import se.alipsa.matrix.pict.PlotFx

            @CompileStatic
            class InvalidJavafxTarget {
              static Integer render(Chart chart) {
                Integer value = ${call}
                value
              }
            }
          """)
        }
        assertTrue(error.message.contains('Cannot assign value of type javafx.scene.Group'))
      }
    }
  }
}
