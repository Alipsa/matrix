package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*
import static org.junit.jupiter.api.Assumptions.assumeTrue

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

class ParquetReaderIoTest {
  @TempDir File directory

  @Test
  void temporaryReadFilesAreOwnerOnly() {
    def path = MatrixParquetReader.createReadTempFile()
    try {
      assumeTrue(Files.getFileStore(path).supportsFileAttributeView('posix'))
      assertEquals([PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE] as Set, Files.getPosixFilePermissions(path))
    } finally { Files.deleteIfExists(path) }
  }

  @Test
  void datasetNamesAreConsistentForBatchesListsDirectoriesAndGlobs() {
    File file = new File(directory, 'part-0.parquet')
    MatrixParquetWriter.write(Matrix.builder().columns(id: [1]).build(), file)
    assertEquals(directory.name, MatrixParquetReader.read(directory).matrixName)
    assertEquals('part-0', MatrixParquetReader.read([file]).matrixName)
    assertEquals('part-0', MatrixParquetReader.readGlob(directory.path + '/*.parquet').matrixName)
    [directory, [file]].each { source ->
      assertEquals('chosen', MatrixParquetReader.builder().matrixName('chosen').read(source).matrixName)
      MatrixParquetReader.builder().matrixName('chosen').batches(source).withCloseable { cursor ->
        assertEquals('chosen', cursor.emptyMatrix().matrixName)
        assertEquals('chosen', cursor.next().matrixName)
      }
      MatrixParquetReader.builder().batches(source).withCloseable { cursor ->
        assertEquals(source instanceof File ? directory.name : 'part-0', cursor.next().matrixName)
      }
    }
  }

  @Test
  void globParsingSupportsWindowsSeparatorsAndFixedDepth() {
    [['*.parquet', '/', '.', '*.parquet', 1], ['{a,b}/*.parquet', '/', '.', '{a,b}/*.parquet', 2],
     ['/*.parquet', '/', '/', '*.parquet', 1], ['C:\\data\\*.parquet', '\\', 'C:/data', '*.parquet', 1],
     ['C:\\data\\sub\\*.parquet', '\\', 'C:/data/sub', '*.parquet', 1],
     ['C:\\data\\{a,b}\\*.parquet', '\\', 'C:/data', '{a,b}/*.parquet', 2],
     ['C:\\data\\**\\*.parquet', '\\', 'C:/data', '**/*.parquet', Integer.MAX_VALUE],
     ['C:\\*.parquet', '\\', 'C:/', '*.parquet', 1]].each { source, separator, root, pattern, depth ->
      def spec = MatrixParquetReader.parseGlob(source, separator)
      assertEquals(root, spec.root); assertEquals(pattern, spec.pattern); assertEquals(depth, spec.depth)
    }
    assertThrows(IllegalArgumentException) { MatrixParquetReader.parseGlob('C:*.parquet', '\\') }
    assertEquals(Integer.MAX_VALUE, MatrixParquetReader.parseGlob('root/{a,b/c}/*.parquet', '/').depth)
  }

  @Test
  void globsRespectRootsEscapesDepthAndErrors() {
    byte[] bytes = MatrixParquetWriter.writeBytes(Matrix.builder().columns(id: [1]).build())
    ['one.parquet', 'a/two.parquet', 'b/c/three.parquet', 'a{b}/four.parquet'].each { name ->
      File file = new File(directory, name); file.parentFile.mkdirs(); file.bytes = bytes
    }
    assertEquals(1, MatrixParquetReader.readGlob(directory.path + '/*.parquet').rowCount())
    assertEquals(3, MatrixParquetReader.readGlob(directory.path + '/**/*.parquet').rowCount())
    assertEquals(2, MatrixParquetReader.readGlob(directory.path + '/{a,b/c}/*.parquet').rowCount())
    assertEquals(1, MatrixParquetReader.parseGlob(directory.path + '/*.parquet', File.separator).depth)
    assertTrue(assertThrows(IllegalArgumentException) { MatrixParquetReader.readGlob(directory.path + '/missing/*.parquet') }.message.contains('No such glob root'))
    assertTrue(assertThrows(IllegalArgumentException) { MatrixParquetReader.readGlob(directory.path + '/*.missing') }.message.contains('No files match glob'))
    assumeTrue(File.separator == '/')
    new File(directory, 'x*.parquet').bytes = bytes
    new File(directory, 'xy.parquet').bytes = bytes
    assertEquals(1, MatrixParquetReader.readGlob(directory.path + '/x\\*.parquet').rowCount())
    assertEquals(1, MatrixParquetReader.readGlob(directory.path + '/a\\{b\\}/*.parquet').rowCount())
  }
}
