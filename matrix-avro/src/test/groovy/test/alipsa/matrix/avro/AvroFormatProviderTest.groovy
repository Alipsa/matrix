package test.alipsa.matrix.avro

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.avro.AvroFormatProvider
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.core.Matrix

import java.nio.file.Path

class AvroFormatProviderTest {

  @TempDir
  Path tempDir

  @Test
  void testOptionDescriptions() {
    assertTrue(Matrix.listReadOptions('avro').contains('matrixName'))
    assertFalse(Matrix.listReadOptions('avro').contains('lenientTypeConversion'))
    assertTrue(Matrix.listWriteOptions('avro').contains('schemaName'))
    assertTrue(AvroReadOptions.describe().contains('readerSchema'))
    assertFalse(AvroReadOptions.describe().contains('lenientTypeConversion'))
    assertTrue(AvroWriteOptions.describe().contains('compression'))
    assertTrue(AvroWriteOptions.describe().contains('columnSchemas'))
  }

  @Test
  void testSpiWriteAndReadRoundTrip() {
    Matrix source = Matrix.builder('orders')
        .columns(
            id: [1, 2],
            amount: [new BigDecimal('12.34'), new BigDecimal('56.78')]
        )
        .types([Integer, BigDecimal])
        .build()

    File file = tempDir.resolve('orders.avro').toFile()
    source.write([
        inferPrecisionAndScale: true,
        schemaName            : 'Orders',
        namespace             : 'se.alipsa.matrix.spi'
    ], file)

    Matrix matrix = Matrix.read([matrixName: 'loaded-orders'], file)
    assertEquals('loaded-orders', matrix.matrixName)
    assertEquals(source.columnNames(), matrix.columnNames())
    assertEquals(new BigDecimal('12.34'), matrix[0, 'amount'])
  }

  @Test
  void testSpiReadDefaultsToAvroRecordName() {
    Matrix source = Matrix.builder('orders')
        .columns(
            id: [1, 2],
            amount: [new BigDecimal('12.34'), new BigDecimal('56.78')]
        )
        .types([Integer, BigDecimal])
        .build()

    File file = tempDir.resolve('orders-default-name.avro').toFile()
    source.write([
        inferPrecisionAndScale: true,
        schemaName            : 'Orders',
        namespace             : 'se.alipsa.matrix.spi'
    ], file)

    Matrix matrix = Matrix.read(file)
    assertEquals('Orders', matrix.matrixName)
  }

  @Test
  void testProviderMetadata() {
    def provider = new AvroFormatProvider()
    assertEquals(['avro'] as Set, provider.supportedExtensions())
    assertEquals('Avro', provider.formatName())
  }

  @Test
  void testReadAndWriteOptionsIgnoreNullStringValues() {
    AvroReadOptions readOptions = AvroReadOptions.fromMap([matrixName: null])
    AvroWriteOptions writeOptions = AvroWriteOptions.fromMap([
        inferPrecisionAndScale: null,
        namespace             : null,
        schemaName            : null,
        compression           : null,
        compressionLevel      : null,
        syncInterval          : null
    ])

    assertEquals(null, readOptions.matrixName)
    assertEquals(false, writeOptions.inferPrecisionAndScale)
    assertEquals('se.alipsa.matrix.avro', writeOptions.namespace)
    assertEquals(null, writeOptions.schemaName)
    assertEquals(AvroWriteOptions.Compression.NULL, writeOptions.compression)
    assertEquals(-1, writeOptions.compressionLevel)
    assertEquals(0, writeOptions.syncInterval)
  }

  @Test
  void testReadOptionsRoundTripFromMapToMap() {
    String readerSchemaJson = """
    {
      "type": "record",
      "name": "ProjectedOrders",
      "fields": [
        {"name":"id", "type":"int"}
      ]
    }
    """.stripIndent()

    AvroReadOptions options = AvroReadOptions.fromMap([
        matrixName  : 'OrdersView',
        readerSchema: readerSchemaJson
    ])

    Map<String, ?> roundTrip = options.toMap()

    assertEquals('OrdersView', roundTrip.matrixName)
    assertTrue(roundTrip.readerSchema instanceof org.apache.avro.Schema)
    assertEquals('ProjectedOrders', (roundTrip.readerSchema as org.apache.avro.Schema).name)
  }

  @Test
  void testSpiWriteDefaultsSchemaNameFromMatrixName() {
    Matrix source = Matrix.builder('orders')
        .columns(
            id: [1, 2],
            amount: [new BigDecimal('12.34'), new BigDecimal('56.78')]
        )
        .types([Integer, BigDecimal])
        .build()

    File file = tempDir.resolve('orders-schema-default.avro').toFile()
    source.write([inferPrecisionAndScale: true], file)

    def reader = new org.apache.avro.file.DataFileReader<org.apache.avro.generic.GenericRecord>(
        file,
        new org.apache.avro.generic.GenericDatumReader<>()
    )
    try {
      assertEquals('orders', reader.schema.name)
    } finally {
      reader.close()
    }
  }

  @Test
  void testTypedAndSpiWritesUseSameDefaultSchemaName() {
    Matrix source = Matrix.builder('orders')
        .columns(
            id: [1, 2],
            amount: [new BigDecimal('12.34'), new BigDecimal('56.78')]
        )
        .types([Integer, BigDecimal])
        .build()

    File spiFile = tempDir.resolve('orders-spi.avro').toFile()
    File typedFile = tempDir.resolve('orders-typed.avro').toFile()

    source.write([inferPrecisionAndScale: true], spiFile)
    se.alipsa.matrix.avro.MatrixAvroWriter.write(
        source,
        typedFile,
        new AvroWriteOptions().inferPrecisionAndScale(true)
    )

    def spiReader = new org.apache.avro.file.DataFileReader<org.apache.avro.generic.GenericRecord>(
        spiFile,
        new org.apache.avro.generic.GenericDatumReader<>()
    )
    def typedReader = new org.apache.avro.file.DataFileReader<org.apache.avro.generic.GenericRecord>(
        typedFile,
        new org.apache.avro.generic.GenericDatumReader<>()
    )
    try {
      assertEquals('orders', spiReader.schema.name)
      assertEquals('orders', typedReader.schema.name)
    } finally {
      spiReader.close()
      typedReader.close()
    }
  }

  @Test
  void testWriteOptionsRoundTripFromMapToMap() {
    AvroWriteOptions options = AvroWriteOptions.fromMap([
        inferPrecisionAndScale: true,
        namespace             : 'se.alipsa.matrix.spi',
        compression           : 'deflate',
        compressionLevel      : 9,
        syncInterval          : 64000,
        columnSchemas         : [
            amount: [kind: 'decimal', precision: 12, scale: 3],
            props : [kind: 'map', valueType: 'INT'],
            tags  : [kind: 'array', elementType: 'STRING'],
            person: [
                kind  : 'record',
                fields: [
                    name: 'STRING',
                    age : 'INT'
                ]
            ]
        ]
    ])

    Map<String, ?> roundTrip = options.toMap()

    assertEquals(true, roundTrip.inferPrecisionAndScale)
    assertEquals('se.alipsa.matrix.spi', roundTrip.namespace)
    assertEquals('DEFLATE', roundTrip.compression)
    assertEquals(9, roundTrip.compressionLevel)
    assertEquals(64000, roundTrip.syncInterval)
    assertFalse(roundTrip.containsKey('schemaName'))
    assertEquals('decimal', ((roundTrip.columnSchemas as Map).amount as Map).kind)
    assertEquals(12, (((roundTrip.columnSchemas as Map).amount as Map).precision))
    assertEquals('map', ((roundTrip.columnSchemas as Map).props as Map).kind)
    assertEquals('INT', (((roundTrip.columnSchemas as Map).props as Map).valueType as Map).scalarType)
    assertEquals('array', ((roundTrip.columnSchemas as Map).tags as Map).kind)
    assertEquals('STRING', ((((roundTrip.columnSchemas as Map).tags as Map).elementType) as Map).scalarType)
  }

  @Test
  void testTypedAndSpiColumnSchemasProduceSameSchema() {
    Matrix source = Matrix.builder('typedWrite')
        .columns(
            amount: [new BigDecimal('12.34'), new BigDecimal('56.78')],
            props: [[x: 1, y: 2], [y: 3, z: 4]],
            tags: [[1L, 2L], [3L, null]]
        )
        .types([BigDecimal, Map, List])
        .build()

    File directFile = tempDir.resolve('typed-column-schemas.avro').toFile()
    File spiFile = tempDir.resolve('spi-column-schemas.avro').toFile()

    AvroWriteOptions typed = new AvroWriteOptions()
        .columnSchema('amount', AvroSchemaDecl.decimal(12, 2))
        .columnSchema('props', AvroSchemaDecl.map(AvroSchemaDecl.type(Integer)))
        .columnSchema('tags', AvroSchemaDecl.array(AvroSchemaDecl.type(Long)))

    se.alipsa.matrix.avro.MatrixAvroWriter.write(source, directFile, typed)
    source.write([
        columnSchemas: [
            amount: [kind: 'decimal', precision: 12, scale: 2],
            props : [kind: 'map', valueType: 'INT'],
            tags  : [kind: 'array', elementType: 'LONG']
        ]
    ], spiFile)

    def directReader = new org.apache.avro.file.DataFileReader<org.apache.avro.generic.GenericRecord>(
        directFile,
        new org.apache.avro.generic.GenericDatumReader<>()
    )
    def spiReader = new org.apache.avro.file.DataFileReader<org.apache.avro.generic.GenericRecord>(
        spiFile,
        new org.apache.avro.generic.GenericDatumReader<>()
    )
    try {
      assertEquals(directReader.schema.toString(true), spiReader.schema.toString(true))
    } finally {
      directReader.close()
      spiReader.close()
    }
  }

  @Test
  void testInvalidColumnSchemaDeclarationFailsFast() {
    IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException) {
      AvroWriteOptions.fromMap([
          columnSchemas: [
              amount: [kind: 'decimal', precision: 8]
          ]
      ])
    }
    assertTrue(ex.message.contains('decimal.scale'))
  }
}
