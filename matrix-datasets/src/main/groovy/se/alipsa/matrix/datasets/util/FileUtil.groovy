package se.alipsa.matrix.datasets.util

import groovy.transform.CompileStatic

import java.nio.file.FileSystemNotFoundException
import java.nio.file.Paths
import java.util.function.Function

/**
 * Common file utilities
 */
@CompileStatic
class FileUtil {

  private FileUtil() {
    // prevent instantiation
  }

  private static final String FILE_PROTOCOL = 'file'
  private static final String PLUS = '+'
  private static final String ENCODED_PLUS = '%2B'
  private static final String DEFAULT_ENCODING = 'UTF-8'

  /**
   * Verify that the filePath exists on the file system and return it as a File.
   * Resources that are only available inside a jar are reported as not found, because they have no
   * file-system representation; read those through {@link #getResourceUrl(String)} instead.
   *
   * @param filePath the path + file name of the resource to find (classpath resource or absolute file path)
   * @return the File if found
   * @throws FileNotFoundException if the filePath cannot be found, or is not a file on the file system
   */
  static File checkFilePath(String filePath) throws FileNotFoundException {
    URL url = getResourceUrl(filePath)
    if (url == null) {
      throw new FileNotFoundException("$filePath does not exist")
    }
    resolveFile(filePath, url, "$filePath cannot be resolved to a file",
        { File f -> "$filePath does not exist" } as Function<File, String>)
  }

  /**
   * Resolves a file-protocol resource url to an existing File, sharing the protocol check, uri conversion
   * and existence check used by {@link #checkFilePath(String)} and {@link #getResourceFile(String, String...)}.
   *
   * @param name the resource name, used in protocol-check error messages
   * @param url the resolved url, must use the file protocol
   * @param unresolvableMsg message prefix when the url cannot be converted to a File
   * @param missingMsg produces the message when the resolved File does not exist
   * @return the resolved File
   * @throws FileNotFoundException if the url is not a file-system url, cannot be resolved, or does not exist
   */
  private static File resolveFile(String name, URL url, String unresolvableMsg,
      Function<File, String> missingMsg) throws FileNotFoundException {
    requireFileProtocol(name, url)
    File file
    try {
      file = Paths.get(url.toURI()).toFile()
    } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException | UnsupportedOperationException e) {
      throw new FileNotFoundException("$unresolvableMsg: ${e.message}").initCause(e)
    }
    if (!file.exists()) {
      throw new FileNotFoundException(missingMsg.apply(file))
    }
    file
  }

  /** Throws if the url does not point into the file system (e.g. a jar: url). */
  private static void requireFileProtocol(String name, URL url) throws FileNotFoundException {
    if (url.protocol != FILE_PROTOCOL) {
      throw new FileNotFoundException(
          "$name was found but is not a file on the file system ($url); read it via getResourceUrl() instead")
    }
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
   * Gets a reference to a file or folder in the classpath or on the file system. Useful for getting test resources and
   * other similar artifacts.
   *
   * @param name the name of the resource, use / to separate path entities.
   *                    Do NOT lead with a "/" unless you know what you are doing.
   * @param encodingOpt retained for source compatibility; file urls are decoded by java.net.URI, so it is not used.
   * @return The resource as a file.
   * @throws FileNotFoundException if the resource cannot be found, or is not a file on the file system (e.g. inside a jar)
   */
  @SuppressWarnings('UnusedMethodParameter')
  static File getResourceFile(String name, String... encodingOpt) throws FileNotFoundException {
    URL url = getResourceUrl(name)
    if (url == null) {
      throw new FileNotFoundException("Resource not found: $name")
    }
    resolveFile(name, url, "Resource $name cannot be resolved to a file",
        { File f -> "Resource $name does not exist ($f)" } as Function<File, String>)
  }

  /**
   * Returns the decoded path part of the resource url. For a resource on the file system this is the absolute path.
   * For a resource inside a jar it is the jar url's file part, e.g. {@code file:/lib/x.jar!/data/x.csv}, which is
   * <b>not</b> a file-system path; use {@link #getResourceFile(String, String...)} if a File is required.
   *
   * @param name the name of the resource, use / to separate path entities.
   * @param encodingOpt optional encoding used to decode percent-escapes, default UTF-8.
   * @return the decoded path of the resource url
   * @throws FileNotFoundException if the resource cannot be found
   * @throws UnsupportedEncodingException if the encoding is not supported
   */
  static String getResourcePath(String name, String... encodingOpt) throws UnsupportedEncodingException {
    URL url = getResourceUrl(name)
    if (url == null) {
      throw new FileNotFoundException("Resource not found: $name")
    }
    decodePath(url, encodingOpt)
  }

  /** Percent-decodes the file part of the url while preserving literal plus signs. */
  private static String decodePath(URL url, String... encodingOpt) throws UnsupportedEncodingException {
    String encoding = encodingOpt.length > 0 ? encodingOpt[0] : DEFAULT_ENCODING
    URLDecoder.decode(url.file.replace(PLUS, ENCODED_PLUS), encoding)
  }

}
