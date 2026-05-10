package se.alipsa.matrix.datasets.util

import groovy.transform.CompileStatic

import java.nio.file.Paths

/**
 * Common file utilities
 */
@CompileStatic
class FileUtil {

  private FileUtil() {
    // prevent instantiation
  }

  /**
   * Verify that the filePath exists and is reachable
   * @param filePath the path + file name of the resource to find
   * @return a File of found
   * @throws FileNotFoundException if the filePath cannot be found
   */
  static File checkFilePath(String filePath) throws FileNotFoundException {
    File excelFile
    URL url = getResourceUrl(filePath)
    if (url == null) {
      throw new FileNotFoundException("$filePath does not exist")
    }
    try {
      excelFile = Paths.get(url.toURI()).toFile()
    } catch (URISyntaxException | IllegalArgumentException | UnsupportedOperationException e) {
      throw new FileNotFoundException("$filePath does not exist").initCause(e)
    }
    if (!excelFile.exists()) {
      throw new FileNotFoundException("$filePath does not exist")
    }
    return excelFile
  }

  /**
   * Find a resource using available class loaders.
   * It will also load resources/files from the
   * absolute path of the file system (not only the classpath's).
   * @param resource the resource to search for
   * @return an url to the resource or null if not found
   */
  static URL getResourceUrl(String resource) {
    final List<ClassLoader> classLoaders = []
    classLoaders.add(Thread.currentThread().getContextClassLoader())
    classLoaders.add(FileUtil.getClassLoader())

    URL url = FileUtil.getResource(resource)
    if (url != null) {
      return url
    }
    for (ClassLoader classLoader : classLoaders) {
      url = getResourceWith(classLoader, resource)
      if (url != null) {
        return url
      }
    }

    final URL systemResource = ClassLoader.getSystemResource(resource)
    if (systemResource != null) {
      return systemResource
    }
    File file = new File(resource)
    if (file.exists()) {
      try {
        return file.toURI().toURL()
      } catch (MalformedURLException ignored) {
        // fall through to return null
      }
    }
    return null
  }

  private static URL getResourceWith(ClassLoader classLoader, String resource) {
    if (classLoader != null) {
      return classLoader.getResource(resource)
    }
    return null
  }

  /**
   * Gets a reference to a file or folder in the classpath. Useful for getting test resources and
   * other similar artifacts.
   *
   * @param name the name of the resource, use / to separate path entities.
   *                    Do NOT lead with a "/" unless you know what you are doing.
   * @param encodingOpt optional encoding if something other than UTF-8 is needed.
   * @return The resource as a file.
   * @throws FileNotFoundException if the resource cannot be found.
   */
  static File getResourceFile(String name, String... encodingOpt) throws FileNotFoundException {
    File file
    try {
      String path = getResourcePath(name, encodingOpt)
      file = new File(path)
    } catch (UnsupportedEncodingException e) {
      throw new FileNotFoundException("Failed to find resource $name")
    }
    return file
  }

  static String getResourcePath(String name, String... encodingOpt) throws UnsupportedEncodingException {
    String encoding = encodingOpt.length > 0 ? encodingOpt[0] : 'UTF-8'
    URL url = getResourceUrl(name)
    if (url == null) {
      throw new FileNotFoundException("Resource not found: $name")
    }
    return URLDecoder.decode(url.getFile(), encoding)
  }

}
