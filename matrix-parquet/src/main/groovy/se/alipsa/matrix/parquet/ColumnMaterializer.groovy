package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

import org.apache.parquet.column.Dictionary
import org.apache.parquet.io.api.Binary
import org.apache.parquet.io.api.Converter
import org.apache.parquet.io.api.GroupConverter
import org.apache.parquet.io.api.PrimitiveConverter
import org.apache.parquet.io.api.RecordMaterializer
import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.Type

/** Column-oriented materialization; record acceptance and rollback remain in upstream hooks. */
@PackageScope
final class ColumnMaterializer extends RecordMaterializer<Boolean> {

  final List<List> columns
  final Map<String, Object> current = [:]
  private final GroupConverter root
  private final int[] sizes
  private final ParquetPredicate residual

  ColumnMaterializer(MessageType schema, List<Class> types, boolean binaryAsString, ParquetPredicate residual) {
    columns = schema.fields.collect { [] }
    sizes = new int[columns.size()]
    this.residual = residual
    Converter[] children = new Converter[schema.fieldCount]
    schema.fields.eachWithIndex { Type field, int index ->
      Closure logicalSink = residual != null && residual.columns().contains(field.name) ? { Object value -> current[field.name] = value } : null
      children[index] = converter(field, types[index], binaryAsString,
          { Object value -> columns[index].add(value) }, logicalSink)
    }
    root = new GroupConverter() {
      @Override
      Converter getConverter(int index) { children[index] }

      @Override
      void start() {
        current.clear()
        columns.eachWithIndex { List column, int index -> sizes[index] = column.size() }
      }

      @Override
      void end() {
        columns.eachWithIndex { List column, int index ->
          if (column.size() == sizes[index]) { column.add(null) }
        }
      }
    }
  }

  @Override
  GroupConverter getRootConverter() { root }

  @Override
  Boolean getCurrentRecord() {
    residual == null || residual.test(current) ? Boolean.TRUE : null
  }

  @Override
  void skipCurrentRecord() {
    columns.eachWithIndex { List column, int index ->
      if (column.size() > sizes[index]) { column.subList(sizes[index], column.size()).clear() }
    }
  }

  private static Converter converter(Type type, Class expected, boolean binaryAsString, Closure sink, Closure logicalSink = null) {
    if (type.primitive) {
      return new ValueConverter(type.asPrimitiveType(), expected, binaryAsString, sink, logicalSink)
    }
    GroupType group = type.asGroupType()
    Map<String, Object> values = [:]
    Converter[] children = new Converter[group.fieldCount]
    group.fields.eachWithIndex { Type child, int index ->
      children[index] = converter(child, null, binaryAsString) { Object value ->
        if (child.repetition == Type.Repetition.REPEATED) {
          ((List) values.computeIfAbsent(child.name) { [] }).add(value)
        } else {
          values[child.name] = value
        }
      }
    }
    new GroupConverter() {
      @Override
      Converter getConverter(int index) { children[index] }

      @Override
      void start() { values.clear() }

      @Override
      void end() {
        def logical = group.logicalTypeAnnotation
        if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
          Type repeated = group.getType(0)
          List entries = (List) (values[repeated.name] ?: [])
          boolean wrapped = !repeated.primitive && repeated.asGroupType().fieldCount == 1 &&
              !(repeated.name in ['array', "${group.name}_tuple".toString()])
          sink.call(wrapped ? entries.collect { entry -> ((Map) entry)[repeated.asGroupType().getType(0).name] } : entries)
        } else if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
          Map result = [:]
          ((List) (values[group.getType(0).name] ?: [])).each { entry -> Map pair = (Map) entry; result[pair.key] = pair.value }
          sink.call(result)
        } else {
          Map result = [:]
          group.fields.each { Type child -> result[child.name] = values[child.name] }
          sink.call(result)
        }
      }
    }
  }

  private static class ValueConverter extends PrimitiveConverter {
    private final PrimitiveType field
    private final Class expected
    private final boolean binaryAsString
    private final Closure sink
    private final Closure logicalSink
    private Object[] dictionary
    private Object[] logicalDictionary

    ValueConverter(PrimitiveType field, Class expected, boolean binaryAsString, Closure sink, Closure logicalSink = null) {
      this.field = field
      this.expected = expected
      this.binaryAsString = binaryAsString
      this.sink = sink
      this.logicalSink = logicalSink
    }

    private void accept(Object value) {
      sink.call(MatrixParquetReader.decodePrimitive(value, field, expected, binaryAsString))
      if (logicalSink != null) { logicalSink.call(logicalValue(value)) }
    }

    private Object logicalValue(Object value) {
      Class logicalExpected = field.logicalTypeAnnotation instanceof LogicalTypeAnnotation.TimeLogicalTypeAnnotation ? java.time.LocalTime :
          field.logicalTypeAnnotation instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation &&
          ((LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) field.logicalTypeAnnotation).isAdjustedToUTC() ? java.time.Instant :
          field.logicalTypeAnnotation == null && field.primitiveTypeName == PrimitiveType.PrimitiveTypeName.BINARY ? byte[] : null
      MatrixParquetReader.decodePrimitive(value, field, logicalExpected, false)
    }

    @Override
    void addInt(int value) { accept(value) }
    @Override
    void addLong(long value) { accept(value) }
    @Override
    void addFloat(float value) { accept(value) }
    @Override
    void addDouble(double value) { accept(value) }
    @Override
    void addBoolean(boolean value) { accept(value) }
    @Override
    void addBinary(Binary value) { accept(value) }
    @Override
    boolean hasDictionarySupport() {
      MatrixParquetReader.isTextLogicalType(field.logicalTypeAnnotation) || expected == String
    }
    @Override
    void setDictionary(Dictionary source) {
      dictionary = new Object[source.maxId + 1]
      if (logicalSink != null) { logicalDictionary = new Object[source.maxId + 1] }
      for (int id = 0; id <= source.maxId; id++) {
        Binary value = source.decodeToBinary(id)
        dictionary[id] = MatrixParquetReader.decodePrimitive(value, field, expected, binaryAsString)
        if (logicalSink != null) { logicalDictionary[id] = logicalValue(value) }
      }
    }
    @Override
    void addValueFromDictionary(int id) { sink.call(dictionary[id]); logicalSink?.call(logicalDictionary[id]) }
  }
}
