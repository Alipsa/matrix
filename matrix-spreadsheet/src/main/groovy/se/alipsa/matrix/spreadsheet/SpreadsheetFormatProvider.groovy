package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.OptionDescriptor

import java.nio.file.Files

/**
 * SPI format provider for spreadsheet files.
 */
@CompileStatic
class SpreadsheetFormatProvider extends AbstractFormatProvider {

  private static final Set<String> EXTENSIONS = ['xlsx', 'ods'] as Set<String>

  @Override
  Set<String> supportedExtensions() {
    EXTENSIONS
  }

  @Override
  String formatName() {
    'Spreadsheet'
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
    SpreadsheetReadOptions readOptions = resolveReadOptions(file, options)
    Map<String, Object> params = importerParams(file, readOptions)
    SpreadsheetImporter.importSpreadsheet(params)
  }

  @Override
  Matrix read(URL url, Map<String, ?> options) {
    String extension = extractExtension(url.path)
    if (!extension) {
      throw new IllegalArgumentException("Cannot auto-detect spreadsheet format for URL '$url': no file extension was found")
    }
    java.nio.file.Path tempFile = Files.createTempFile('matrix-spreadsheet-', ".${extension}")
    try {
      url.openStream().withCloseable { InputStream is ->
        Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      }
      read(tempFile.toFile(), options)
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }

  @Override
  void write(Matrix matrix, File file, Map<String, ?> options) {
    SpreadsheetWriteOptions writeOptions = SpreadsheetWriteOptions.fromMap(options)
    if (writeOptions.sheetName == null && writeOptions.startPosition == 'A1') {
      SpreadsheetWriter.write(matrix, file)
      return
    }
    String sheetName = writeOptions.sheetName ?: (matrix.matrixName ?: 'Sheet1')
    SpreadsheetWriter.write(matrix, file, sheetName, writeOptions.startPosition)
  }

  @Override
  List<OptionDescriptor> readOptionDescriptors() {
    SpreadsheetReadOptions.descriptors()
  }

  @Override
  List<OptionDescriptor> writeOptionDescriptors() {
    SpreadsheetWriteOptions.descriptors()
  }

  private static SpreadsheetReadOptions resolveReadOptions(File file, Map<String, ?> options) {
    SpreadsheetReadOptions readOptions = SpreadsheetReadOptions.fromMap(options)
    if (readOptions.endRow != null && readOptions.hasEndColumn()) {
      return readOptions
    }
    SpreadsheetReader.Factory.create(file).withCloseable { SpreadsheetReader reader ->
      if (readOptions.endRow == null) {
        readOptions.endRow(resolveLastRow(reader, readOptions))
      }
      if (!readOptions.hasEndColumn()) {
        readOptions.endColumn(resolveLastColumn(reader, readOptions))
      }
    }
    readOptions
  }

  private static Map<String, Object> importerParams(File file, SpreadsheetReadOptions readOptions) {
    Map<String, Object> params = [:]
    params.putAll(readOptions.toMap())
    params.file = file
    params.startCol = params.remove('startColumn')
    if (params.containsKey('endColumn')) {
      params.endCol = params.remove('endColumn')
    }
    params
  }

  private static int resolveLastRow(SpreadsheetReader reader, SpreadsheetReadOptions options) {
    options.hasSheetName()
        ? reader.findLastRow(options.sheetName)
        : reader.findLastRow(options.sheetNumber ?: 1)
  }

  private static int resolveLastColumn(SpreadsheetReader reader, SpreadsheetReadOptions options) {
    int detectedLastColumn = options.hasSheetName()
        ? reader.findLastCol(options.sheetName)
        : reader.findLastCol(options.sheetNumber ?: 1)
    detectedLastColumn
  }
}
