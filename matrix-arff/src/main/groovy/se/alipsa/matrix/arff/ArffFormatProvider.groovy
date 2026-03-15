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
    if (readOptions.matrixName == null) {
      return MatrixArffReader.read(file)
    }
    new FileInputStream(file).withCloseable { InputStream is ->
      MatrixArffReader.read(is, readOptions.matrixName)
    }
  }

  @Override
  Matrix read(URL url, Map<String, ?> options) {
    ArffReadOptions readOptions = ArffReadOptions.fromMap(options)
    if (readOptions.matrixName == null) {
      return MatrixArffReader.read(url)
    }
    url.openStream().withCloseable { InputStream is ->
      MatrixArffReader.read(is, readOptions.matrixName)
    }
  }

  @Override
  Matrix read(InputStream is, Map<String, ?> options) {
    ArffReadOptions readOptions = ArffReadOptions.fromMap(options)
    MatrixArffReader.read(is, readOptions.matrixName ?: 'ArffMatrix')
  }

  @Override
  void write(Matrix matrix, File file, Map<String, ?> options) {
    ArffWriteOptions writeOptions = ArffWriteOptions.fromMap(options)
    if (writeOptions.nominalMappings.isEmpty()) {
      MatrixArffWriter.write(matrix, file)
    } else {
      MatrixArffWriter.write(matrix, file, writeOptions.nominalMappings)
    }
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
