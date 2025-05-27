package se.alipsa.matrix.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.hadoop.example.GroupWriteSupport
import org.apache.parquet.io.api.RecordConsumer
import org.apache.parquet.schema.MessageType

class GroupWriteSupportWithMetadata extends GroupWriteSupport {
  @Override
  WriteContext init(Configuration configuration) {
    MessageType schema = getSchema(configuration)
    Map<String, String> extraMeta = [:]
    extraMeta.put("matrix.columnTypes", configuration.get("matrix.columnTypes"))
    return new WriteContext(schema, extraMeta)
  }

  @Override
  void prepareForWrite(RecordConsumer recordConsumer) {
    super.prepareForWrite(recordConsumer)
  }
}
