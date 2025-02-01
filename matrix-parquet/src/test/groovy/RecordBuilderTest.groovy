import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.parquet.RecordBuilder

class RecordBuilderTest {

  @Test
  void testCreateRecord() {
    // record Point(int x, int y, String color) { }
    RecordBuilder builder = new RecordBuilder(this)
    builder.name = "Point"
    builder.addField('x', int)
    builder.addField('y', int)
    builder.addField('color', String)
    def myClass = builder.createRecord()
    def bluePointAtOrigin = myClass.newInstance([0, 1, 'Blue'] as Object[])
    println bluePointAtOrigin
  }

  @Test
  void testCreateRecordList() {
    def rb = new RecordBuilder(this)
    def mtcars = Dataset.mtcars()
    def records = rb.createRecordRows(mtcars)
    Assertions.assertEquals(32, records.rows.size())
    records.rows.eachWithIndex { rec, idx ->
      Row row = mtcars.row(idx)
      row.eachWithIndex { Object entry, int col ->
        String colName = row.columnName(col)
        def recVal = rec[colName]
        //println "checking if $entry equals $recVal row $idx, col $col"
        Assertions.assertEquals(entry, recVal, "Error on row $idx, col $col")
      }

    }
  }
}
