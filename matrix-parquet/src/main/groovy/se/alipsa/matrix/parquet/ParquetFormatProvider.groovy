package se.alipsa.matrix.parquet

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.OptionDescriptor

/**
 * SPI format provider for Parquet files.
 */
class ParquetFormatProvider extends AbstractFormatProvider {

  private static final Set<String> EXTENSIONS = ['parquet'] as Set<String>

  @Override
  Set<String> supportedExtensions() {
    EXTENSIONS
  }

  @Override
  String formatName() {
    'Parquet'
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
    MatrixParquetReader.builder(ParquetReadOptions.fromMap(options)).read(file)
  }

  @Override
  Matrix read(URL url, Map<String, ?> options) {
    MatrixParquetReader.builder(ParquetReadOptions.fromMap(options)).read(url)
  }

  @Override
  Matrix read(InputStream stream, Map<String, ?> options) {
    MatrixParquetReader.builder(ParquetReadOptions.fromMap(options)).read(stream)
  }

  @Override
  void write(Matrix matrix, File file, Map<String, ?> options) {
    MatrixParquetWriter.write(matrix, file, ParquetWriteOptions.fromMap(options))
  }

  @Override
  List<OptionDescriptor> readOptionDescriptors() {
    ParquetReadOptions.descriptors()
  }

  @Override
  List<OptionDescriptor> writeOptionDescriptors() {
    ParquetWriteOptions.descriptors()
  }
}
