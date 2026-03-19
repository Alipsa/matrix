package se.alipsa.matrix.avro

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.OptionDescriptor

/**
 * SPI format provider for Avro files.
 */
@CompileStatic
class AvroFormatProvider extends AbstractFormatProvider {

  private static final Set<String> EXTENSIONS = ['avro'] as Set<String>

  @Override
  Set<String> supportedExtensions() {
    EXTENSIONS
  }

  @Override
  String formatName() {
    'Avro'
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
    MatrixAvroReader.read(file, AvroReadOptions.fromMap(options))
  }

  @Override
  Matrix read(URL url, Map<String, ?> options) {
    MatrixAvroReader.read(url, AvroReadOptions.fromMap(options))
  }

  @Override
  Matrix read(InputStream is, Map<String, ?> options) {
    MatrixAvroReader.read(is, AvroReadOptions.fromMap(options))
  }

  @Override
  void write(Matrix matrix, File file, Map<String, ?> options) {
    MatrixAvroWriter.write(matrix, file, AvroWriteOptions.fromMap(options))
  }

  @Override
  List<OptionDescriptor> readOptionDescriptors() {
    AvroReadOptions.descriptors()
  }

  @Override
  List<OptionDescriptor> writeOptionDescriptors() {
    AvroWriteOptions.descriptors()
  }
}
