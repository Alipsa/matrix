package se.alipsa.matrix.arff

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.OptionDescriptor

/**
 * SPI format provider for ARFF files.
 */
@CompileStatic
class ArffFormatProvider extends AbstractFormatProvider {

  private static final Set<String> EXTENSIONS = ['arff'] as Set<String>

  @Override
  Set<String> supportedExtensions() {
    EXTENSIONS
  }

  @Override
  String formatName() {
    'ARFF'
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
    ArffReadOptions readOptions = ArffReadOptions.fromMap(options)
    MatrixArffReader.read(file, readOptions)
  }

  @Override
  Matrix read(URL url, Map<String, ?> options) {
    ArffReadOptions readOptions = ArffReadOptions.fromMap(options)
    MatrixArffReader.read(url, readOptions)
  }

  @Override
  Matrix read(InputStream is, Map<String, ?> options) {
    ArffReadOptions readOptions = ArffReadOptions.fromMap(options)
    MatrixArffReader.read(is, readOptions)
  }

  @Override
  void write(Matrix matrix, File file, Map<String, ?> options) {
    ArffWriteOptions writeOptions = ArffWriteOptions.fromMap(options)
    MatrixArffWriter.write(matrix, file, writeOptions)
  }

  @Override
  List<OptionDescriptor> readOptionDescriptors() {
    ArffReadOptions.descriptors()
  }

  @Override
  List<OptionDescriptor> writeOptionDescriptors() {
    ArffWriteOptions.descriptors()
  }
}
