package se.alipsa.matrix.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.example.GroupWriteSupport
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.MessageType

class GroupWriteSupportWithMetadata extends GroupWriteSupport {

  @Override
  WriteContext init(Configuration configuration) {
    MessageType schema = getSchema(configuration)
    Map<String, String> extraMeta = [:]
    String columnTypes = configuration.get("matrix.columnTypes")
    if (columnTypes != null) {
      extraMeta.put("matrix.columnTypes", columnTypes)
      println "Added column types to metadata: $columnTypes"
    } else {
      println "Warning: No column types specified in configuration. " +
              "This may lead to issues when reading the Parquet file without metadata."
      // If no column types are specified, we can still write the schema without metadata
      extraMeta.put("matrix.columnTypes", schema.fields.collect { it.asPrimitiveType().name }.join(','))
    }
    return new WriteContext(schema, extraMeta)
  }

  @Override
  void prepareForWrite(RecordConsumer recordConsumer) {
    super.prepareForWrite(recordConsumer)
  }
}
