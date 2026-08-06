package test.alipsa.matrix.api

import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset

import java.nio.file.Files
import java.nio.file.Path as NioPath

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

/** Shared fixtures and assertions for the BOM API integration tests. */
trait ApiItSupport {

  /** @return the documented mtcars fixture. */
  static Matrix mtcars() {
    Dataset.mtcars()
  }

  /** @return the documented airquality fixture. */
  static Matrix airquality() {
    Dataset.airquality()
  }

  /** Creates a unique file below the Maven target directory. */
  static NioPath tempFile(String suffix) {
    NioPath directory = NioPath.of('target', 'api-it-files')
    Files.createDirectories(directory)
    Files.createTempFile(directory, 'matrix-api-', suffix)
  }

  /** Asserts that a rendered SVG contains native path geometry. */
  static void assertRenderedSvg(Svg svg) {
    assertNotNull(svg, 'rendered SVG')
    assertTrue(svg.descendants().findAll { it instanceof Path || it.class.simpleName in ['Circle', 'Rect'] }.size() > 0,
        'rendered SVG should contain chart geometry')
    assertTrue(SvgWriter.toXml(svg).contains('<svg'), 'serialized SVG should have an svg root')
  }
}
