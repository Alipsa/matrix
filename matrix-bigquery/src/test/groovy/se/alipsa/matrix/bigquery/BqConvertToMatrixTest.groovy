package se.alipsa.matrix.bigquery

import static org.junit.jupiter.api.Assertions.assertEquals

import groovy.transform.CompileStatic

import com.google.api.gax.paging.Page
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.TableResult
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix

/**
 * Tests BigQuery result conversion to Matrix.
 */
@CompileStatic
class BqConvertToMatrixTest {

  @Test
  void repeatedFieldsBecomeListColumns() {
    Field tagsField = Field.newBuilder('tags', StandardSQLTypeName.STRING)
        .setMode(Field.Mode.REPEATED)
        .build()
    Schema schema = Schema.of(tagsField)
    FieldValue repeatedValue = FieldValue.of(FieldValue.Attribute.REPEATED, FieldValueList.of([
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, 'tag1'),
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, 'tag2')
    ]))
    FieldValueList row = FieldValueList.of([repeatedValue], schema.fields)
    TableResult result = TableResult.newBuilder()
        .setSchema(schema)
        .setTotalRows(1L)
        .setPageNoSchema(pageOf([row]))
        .build()

    Matrix matrix = Bq.convertToMatrix(result)

    assertEquals([List], matrix.types())
    assertEquals([['tag1', 'tag2']], matrix['tags'])
  }

  private static Page<FieldValueList> pageOf(List<FieldValueList> values) {
    new Page<FieldValueList>() {
      final String nextPageToken = null
      final Page<FieldValueList> nextPage = null

      @Override
      boolean hasNextPage() {
        false
      }

      @Override
      Iterable<FieldValueList> iterateAll() {
        values
      }

      @Override
      Iterable<FieldValueList> getValues() {
        values
      }
    }
  }
}
