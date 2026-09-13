package se.alipsa.matrix.avro

import static org.junit.jupiter.api.Assertions.*

import org.apache.avro.Schema
import org.junit.jupiter.api.Test

class AvroSchemaCompatibilityTest {

  @Test
  void multiBranchUnionPrefersTheMatchedContainerFailure() {
    Schema schema = new Schema.Parser().parse('''
      [
        {"type":"array","items":"int"},
        {"type":"map","values":"int"}
      ]
    ''')

    AvroSchemaCompatibility.CompatibilityFailure failure =
        AvroSchemaCompatibility.findIncompatibleValue(schema, [invalid: 'not an integer'], 'value')

    assertNotNull(failure)
    assertEquals("value['invalid']", failure.path)
    assertEquals(Schema.Type.INT, failure.schema.type)
    assertEquals('not an integer', failure.value)
  }
}
