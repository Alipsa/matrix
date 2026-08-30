package se.alipsa.matrix.jupyter.render

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Structure
import se.alipsa.matrix.core.Summary
import se.alipsa.matrix.jupyter.MimeBundle
import se.alipsa.matrix.jupyter.RenderOptions

/** Tests for core table MIME rendering. */
class CoreRendererTest {
  @Test
  void keepsPlainFallbackAndEscapesCellValues() {
    Matrix matrix = Matrix.builder().columns(value: ['<script>alert(1)</script>']).build()

    MimeBundle bundle = new CoreRenderer().render(matrix, new RenderOptions())

    assertEquals(['text/html', 'text/plain'], bundle.keySet().toList())
    assertTrue(bundle['text/html'].contains('&lt;script&gt;alert(1)&lt;/script&gt;'))
    assertFalse(bundle['text/html'].contains('<script>'))
  }

  @Test
  void truncatesRowsAndColumnsWithAnEscapedCaption() {
    Matrix matrix = Matrix.builder().columns(a: [1, 2, 3], b: [4, 5, 6], c: [7, 8, 9]).build()

    MimeBundle bundle = new CoreRenderer().render(matrix, new RenderOptions(2, 2, true, [caption: '<mine>']))

    assertTrue(bundle['text/html'].contains('<caption>&lt;mine&gt; — showing 2 of 3 rows, 2 of 3 columns</caption>'))
    assertFalse(bundle['text/html'].contains('<th class=\'c'))
  }

  @Test
  void supportsUnlimitedAndEmptyMatrices() {
    Matrix matrix = Matrix.builder().columns(a: [1, 2], b: [3, 4]).build()
    MimeBundle unlimited = new CoreRenderer().render(matrix, new RenderOptions(null, null))
    MimeBundle empty = new CoreRenderer().render(Matrix.builder().build(), new RenderOptions())

    assertFalse(unlimited['text/html'].contains('showing '))
    assertTrue(unlimited['text/html'].contains('>4</td>'))
    assertTrue(empty['text/html'].contains('<table'))
  }

  @Test
  void wrapsGridRowsAndColumnsAsTables() {
    Matrix matrix = Matrix.builder().columns(name: ['Ada'], score: [42]).build()
    Grid grid = new Grid([[1, 2], [3, 4]])
    CoreRenderer renderer = new CoreRenderer()

    MimeBundle row = renderer.render(matrix.row(0), new RenderOptions())
    MimeBundle column = renderer.render(matrix.column('score'), new RenderOptions())
    MimeBundle gridBundle = renderer.render(grid, new RenderOptions())

    assertTrue(row['text/html'].contains('>name</th>'))
    assertTrue(row['text/html'].contains('>Ada</td>'))
    assertTrue(column['text/html'].contains('>score</th>'))
    assertTrue(column['text/html'].contains('>42</td>'))
    assertTrue(gridBundle['text/html'].contains('>4</td>'))
  }

  @Test
  void rendersSparseSummaryAndRaggedStructureData() {
    Summary summary = new Summary()
    summary['first'] = [mean: 3, min: 1]
    summary['second'] = [mean: 7, max: 9]
    Structure structure = new Structure()
    structure['first'] = ['Integer']
    structure['second'] = ['String', 'example']
    CoreRenderer renderer = new CoreRenderer()

    MimeBundle summaryBundle = renderer.render(summary, new RenderOptions())
    MimeBundle structureBundle = renderer.render(structure, new RenderOptions())

    assertTrue(summaryBundle['text/html'].contains('>variable</th>'))
    assertTrue(summaryBundle['text/html'].contains('>max</th>'))
    assertTrue(summaryBundle['text/html'].contains('>9</td>'))
    assertTrue(structureBundle['text/html'].contains('String, example'))
  }
}
