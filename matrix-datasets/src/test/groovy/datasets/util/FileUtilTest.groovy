package datasets.util

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.datasets.util.FileUtil

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileUtilTest {

  private static final String JAR_RESOURCE = 'jarres/inside.csv'

  @Test
  void testGetResourceUrlFindsClasspathResource() {
    URL url = FileUtil.getResourceUrl('/data/iris.csv')
    assertNotNull(url, 'bundled resource should be found')
    assertTrue(url.toString().endsWith('/data/iris.csv'))
  }

  @Test
  void testGetResourceUrlReturnsNullForMissingResource() {
    assertNull(FileUtil.getResourceUrl('/data/does_not_exist.csv'))
  }

  @Test
  void testMissingResourceThrowsFileNotFound() {
    assertThrows(FileNotFoundException) { FileUtil.checkFilePath('/data/does_not_exist.csv') }
    assertThrows(FileNotFoundException) { FileUtil.getResourceFile('/data/does_not_exist.csv') }
    assertThrows(FileNotFoundException) { FileUtil.getResourcePath('/data/does_not_exist.csv') }
  }

  @Test
  void testFileSystemResource(@TempDir Path tmp) {
    Path file = Files.writeString(tmp.resolve('local.csv'), 'a,b\n1,2\n')
    String abs = file.toAbsolutePath()
    File expected = file.toFile().canonicalFile
    assertEquals(file.toUri().toURL(), FileUtil.getResourceUrl(abs))
    assertEquals(file.toUri().path, FileUtil.getResourcePath(abs))
    assertEquals(expected, FileUtil.getResourceFile(abs).canonicalFile)
    assertEquals(expected, FileUtil.checkFilePath(abs).canonicalFile)
  }

  @Test
  void testPlusSignInPathIsPreserved(@TempDir Path tmp) {
    Path dir = Files.createDirectory(tmp.resolve('c++'))
    Path file = Files.writeString(dir.resolve('a+b.csv'), 'x\n1\n')
    String abs = file.toAbsolutePath()
    File expected = file.toFile().canonicalFile
    String path = FileUtil.getResourcePath(abs)
    assertEquals(file.toUri().path, path)
    assertTrue(path.endsWith('/c++/a+b.csv'), "literal + must survive decoding: $path")
    assertFalse(path.endsWith('/c  /a b.csv'), "+ must not be decoded as a space: $path")
    assertEquals(expected, FileUtil.getResourceFile(abs).canonicalFile)
    assertEquals(expected, FileUtil.checkFilePath(abs).canonicalFile)
  }

  @Test
  void testResourceInsideJar(@TempDir Path tmp) {
    Path jar = tmp.resolve('res.jar')
    new ZipOutputStream(Files.newOutputStream(jar)).withCloseable { ZipOutputStream zip ->
      zip.putNextEntry(new ZipEntry(JAR_RESOURCE))
      zip.write('a,b\n1,2\n'.getBytes('UTF-8'))
      zip.closeEntry()
    }
    ClassLoader previous = Thread.currentThread().contextClassLoader
    URLClassLoader loader = new URLClassLoader([jar.toUri().toURL()] as URL[], null)
    Thread.currentThread().contextClassLoader = loader
    try {
      URL url = FileUtil.getResourceUrl(JAR_RESOURCE)
      assertNotNull(url, 'resource inside the jar should be found')
      assertEquals('jar', url.protocol)

      String path = FileUtil.getResourcePath(JAR_RESOURCE)
      assertTrue(path.endsWith("res.jar!/$JAR_RESOURCE"), path)

      def checkEx = assertThrows(FileNotFoundException) { FileUtil.checkFilePath(JAR_RESOURCE) }
      assertTrue(checkEx.message.contains('not a file on the file system'), checkEx.message)
      assertTrue(checkEx.message.contains('res.jar'), checkEx.message)

      def fileEx = assertThrows(FileNotFoundException) { FileUtil.getResourceFile(JAR_RESOURCE) }
      assertTrue(fileEx.message.contains('not a file on the file system'), fileEx.message)
    } finally {
      Thread.currentThread().contextClassLoader = previous
      loader.close()
    }
  }
}
