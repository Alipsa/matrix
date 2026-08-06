package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.alipsa.matrix.tablesaw.TableUtil
import se.alipsa.matrix.tablesaw.gtable.Gtable

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers Matrix/Tablesaw conversions, frequency/rounding, and Gtable readers. */
@Tag('tablesaw')
class TablesawApiIT implements ApiItSupport {

  @Test
  void convertsTablesAndReadsGtableCsv(@TempDir Path directory) {
    def matrix = mtcars().select(['cyl', 'mpg'])
    def table = TableUtil.toTablesaw(matrix)
    assertEquals(matrix.rowCount(), table.rowCount())
    assertEquals(matrix.columnCount(), table.columnCount())
    assertEquals(matrix.rowCount(), TableUtil.fromTablesaw(table).rowCount())
    assertTrue(TableUtil.frequency(table, 'cyl').rowCount() > 0)
    assertEquals(1.23d, TableUtil.round(1.234d, 2), 0.0001d)
    File csv = directory.resolve('table.csv').toFile()
    csv.text = 'id,value\n1.5,a\n2.5,b\n'
    Gtable read = Gtable.read().csv(csv)
    assertEquals(2, read.rowCount())
    assertEquals(2, read.columnCount())
    assertEquals(2, read.normalizeMinMax('id', 'normalized', 2).rowCount())
  }
}
