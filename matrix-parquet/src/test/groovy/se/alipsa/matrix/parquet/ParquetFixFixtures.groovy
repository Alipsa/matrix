package se.alipsa.matrix.parquet

import org.apache.parquet.conf.PlainParquetConfiguration
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.io.LocalOutputFile
import org.apache.parquet.schema.MessageType

/** External-file fixtures without Matrix writer metadata or conversion. */
class ParquetFixFixtures {
  static File write(File directory, String name, MessageType schema, Map<String, String> metadata = [:], Closure rows) {
    File file = new File(directory, name + '.parquet')
    ExampleParquetWriter.builder(new LocalOutputFile(file.toPath())).withConf(new PlainParquetConfiguration())
        .withType(schema).withExtraMetaData(metadata).build().withCloseable { writer ->
          rows.call(writer, new SimpleGroupFactory(schema))
        }
    file
  }

  static Map footer(byte[] bytes) {
    ParquetFileReader.open(new InMemoryInputFile(bytes), org.apache.parquet.ParquetReadOptions.builder(new PlainParquetConfiguration()).build()).withCloseable { reader ->
      [schema: reader.fileMetaData.schema, metadata: reader.fileMetaData.keyValueMetaData]
    }
  }
}
