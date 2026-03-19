package se.alipsa.matrix.json

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.OptionDescriptor

/**
 * SPI format provider for JSON files.
 */
@CompileStatic
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
    JsonReader.read(file, readOptions.charset)
  }

  @Override
  Matrix read(URL url, Map<String, ?> options) {
    JsonReadOptions readOptions = JsonReadOptions.fromMap(options)
    JsonReader.read(url, readOptions.charset)
  }

  @Override
  Matrix read(InputStream is, Map<String, ?> options) {
    JsonReadOptions readOptions = JsonReadOptions.fromMap(options)
    JsonReader.read(is, readOptions.charset)
  }

  @Override
  void write(Matrix matrix, File file, Map<String, ?> options) {
    JsonWriteOptions writeOptions = JsonWriteOptions.fromMap(options)
    JsonWriter.write(matrix, file, writeOptions.columnFormatters, writeOptions.indent, writeOptions.dateFormat)
  }

  @Override
  List<OptionDescriptor> readOptionDescriptors() {
    JsonReadOptions.descriptors()
  }

  @Override
  List<OptionDescriptor> writeOptionDescriptors() {
    JsonWriteOptions.descriptors()
  }
}
