package se.alipsa.matrix.parquet

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.OptionDescriptor

/**
 * SPI format provider for Parquet files.
 */
@CompileStatic
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
    ParquetReadOptions readOptions = ParquetReadOptions.fromMap(options)
    if (readOptions.matrixName != null && readOptions.zoneId != null) {
      return MatrixParquetReader.read(file, readOptions.matrixName, readOptions.zoneId)
    }
    if (readOptions.matrixName != null) {
      return MatrixParquetReader.read(file, readOptions.matrixName)
    }
    if (readOptions.zoneId != null) {
      return MatrixParquetReader.read(file, readOptions.zoneId)
    }
    MatrixParquetReader.read(file)
  }

  @Override
  Matrix read(URL url, Map<String, ?> options) {
    ParquetReadOptions readOptions = ParquetReadOptions.fromMap(options)
    if (readOptions.matrixName != null && readOptions.zoneId != null) {
      return MatrixParquetReader.read(url, readOptions.matrixName, readOptions.zoneId)
    }
    if (readOptions.matrixName != null) {
      url.openStream().withCloseable { InputStream is ->
        return MatrixParquetReader.read(is, readOptions.matrixName)
      }
    }
    if (readOptions.zoneId != null) {
      return MatrixParquetReader.read(url, readOptions.zoneId)
    }
    MatrixParquetReader.read(url)
  }

  @Override
  Matrix read(InputStream is, Map<String, ?> options) {
    ParquetReadOptions readOptions = ParquetReadOptions.fromMap(options)
    if (readOptions.matrixName != null && readOptions.zoneId != null) {
      return MatrixParquetReader.read(is, readOptions.matrixName, readOptions.zoneId)
    }
    if (readOptions.matrixName != null) {
      return MatrixParquetReader.read(is, readOptions.matrixName)
    }
    if (readOptions.zoneId != null) {
      return MatrixParquetReader.read(is, readOptions.zoneId)
    }
    MatrixParquetReader.read(is)
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
