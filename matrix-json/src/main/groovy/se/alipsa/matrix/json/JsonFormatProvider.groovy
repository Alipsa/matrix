package se.alipsa.matrix.json

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.OptionDescriptor

/**
 * SPI format provider for JSON files.
 */
class JsonFormatProvider extends AbstractFormatProvider {

  private static final Set<String> EXTENSIONS = ['json'] as Set<String>

  @Override
  Set<String> supportedExtensions() {
    EXTENSIONS
  }

  @Override
  String formatName() {
    'JSON'
  }

  @Override
  boolean canRead() {
    true
  }

  @Override
  boolean canWrite() {
    true
  }

  @Override
  Matrix read(File file, Map<String, ?> options) {
    JsonReadOptions readOptions = JsonReadOptions.fromMap(options)
    Matrix result = JsonReader.read(file, readOptions.charset)
    applyReadOptions(result, readOptions)
  }

  @Override
  Matrix read(URL url, Map<String, ?> options) {
    JsonReadOptions readOptions = JsonReadOptions.fromMap(options)
    Matrix result = JsonReader.read(url, readOptions.charset)
    applyReadOptions(result, readOptions)
  }

  @Override
  Matrix read(InputStream is, Map<String, ?> options) {
    JsonReadOptions readOptions = JsonReadOptions.fromMap(options)
    Matrix result = JsonReader.read(is, readOptions.charset)
    applyReadOptions(result, readOptions)
  }

  @Override
  void write(Matrix matrix, File file, Map<String, ?> options) {
    JsonWriteOptions writeOptions = JsonWriteOptions.fromMap(options)
    JsonWriter.WriteBuilder builder = JsonWriter.write(matrix)
        .indent(writeOptions.indent)
        .dateFormat(writeOptions.dateFormat)
        .columnFormatters(writeOptions.columnFormatters)
    if (writeOptions.dateTimeFormat != null) {
      builder.dateTimeFormat(writeOptions.dateTimeFormat)
    }
    builder.to(file)
  }

  @Override
  List<OptionDescriptor> readOptionDescriptors() {
    JsonReadOptions.descriptors()
  }

  @Override
  List<OptionDescriptor> writeOptionDescriptors() {
    JsonWriteOptions.descriptors()
  }

  private static Matrix applyReadOptions(Matrix result, JsonReadOptions readOptions) {
    if (readOptions.matrixName != null) {
      result.matrixName = readOptions.matrixName
    }
    if (readOptions.types != null) {
      result = result.convert(readOptions.types, readOptions.dateTimeFormat)
    }
    result
  }
}
