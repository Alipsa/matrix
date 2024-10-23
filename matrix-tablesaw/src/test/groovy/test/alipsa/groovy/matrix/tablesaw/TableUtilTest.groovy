package test.alipsa.groovy.matrix.tablesaw

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.tablesaw.TableUtil
import tech.tablesaw.api.BigDecimalColumn
import tech.tablesaw.api.ColumnType
import tech.tablesaw.api.Table
import tech.tablesaw.column.numbers.BigDecimalColumnType
import tech.tablesaw.io.csv.CsvReadOptions

import static org.junit.jupiter.api.Assertions.assertEquals
import static tech.tablesaw.api.ColumnType.*

class TableUtilTest {

  @Test
  void testFrequency() {
    def csv = getClass().getResource("/glaciers.csv")
    CsvReadOptions.Builder builder = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes([INTEGER, DOUBLE, INTEGER] as ColumnType[])

    def glaciers = Table.read().usingOptions(builder.build())
    def freq = TableUtil.frequency(glaciers, "Number of observations")
    Assertions.assertEquals(20, freq.size())
    Assertions.assertEquals(31, freq.get(0, 1))
  }

  @Test
  void testRound() {
    def csv = getClass().getResource("/glaciers.csv")
    CsvReadOptions.Builder builder = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes([INTEGER, BigDecimalColumnType.instance(), INTEGER] as ColumnType[])

    def glaciers = Table.read().usingOptions(builder.build())
    BigDecimalColumn col = glaciers.column(1) as BigDecimalColumn
    TableUtil.round(col, 2);
    col.forEach(v -> assertEquals(2, v.scale()))
  }

  @Test
  void testColumnTypeForClass() {
    assertEquals(STRING, TableUtil.columnTypeForClass(String.class))
    assertEquals(BOOLEAN, TableUtil.columnTypeForClass(Boolean.class))
    assertEquals(BigDecimalColumnType.instance(), TableUtil.columnTypeForClass(BigDecimal.class))
  }

  @Test
  void testConvertMatrixToTablesaw() {
    Matrix glaciers = Matrix.builder().data(getClass().getResource("/glaciers.csv")).build()
    Table table = TableUtil.toTablesaw(glaciers)
    assertEquals("glaciers", table.name())
    assertEquals(glaciers.columnCount(), table.columnCount(), "number of columns")
    assertEquals(glaciers.rowCount(), table.rowCount(), "number of rows")
    assertEquals(glaciers.get(1,0), table.get(1, 0))
    assertEquals(glaciers.get(2,1), table.get(2, 1))
    assertEquals(glaciers.get(3,2), table.get(3, 2))
  }

  @Test
  void testConvertTablesawToMatrix() throws IOException {
    var csv = getClass().getResource("/tornadoes_1950-2014.csv")
    CsvReadOptions.Builder builder = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes(new ColumnType[]{LOCAL_DATE, LOCAL_TIME, STRING, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE, DOUBLE} );

    var table = Table.read().usingOptions(builder.build());
    Matrix matrix = TableUtil.fromTablesaw(table);
    assertEquals(table.name(), matrix.getMatrixName());
    assertEquals(table.columnCount(), matrix.columnCount(), "number of columns");
    assertEquals(table.rowCount(), matrix.rowCount(), "number of rows");
    assertEquals(table.get(0,1), matrix.get(0, 1));
    assertEquals(table.get(2,3), matrix.get(2, 3));
  }
}
