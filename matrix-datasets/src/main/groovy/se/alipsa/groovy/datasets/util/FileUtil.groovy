package se.alipsa.groovy.datasets.util

import java.nio.file.Paths

/**
 * Common file utilities
 */
class FileUtil {

   private FileUtil() {
      // prevent instantiation
   }

   /**
    * Verify that the filePath exists and is reachable
    * @param filePath the path + file name of the resource to find
    * @return a File of found
    * @throws Exception if the filePath cannot be found
    */
   static File checkFilePath(String filePath) throws Exception {
      File excelFile
      URL url = getResourceUrl(filePath)
      if (url == null) {
         throw new Exception(filePath + " does not exist")
      }
      try {
         excelFile = Paths.get(url.toURI()).toFile()
      } catch (URISyntaxException | RuntimeException e) {
         throw new Exception(filePath + " does not exist", e)
      }
      if (!excelFile.exists()) {
         throw new Exception(filePath + " does not exist")
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
      final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>()
      classLoaders.add(Thread.currentThread().getContextClassLoader())
      classLoaders.add(FileUtil.class.getClassLoader())

      for (ClassLoader classLoader : classLoaders) {
         final URL url = getResourceWith(classLoader, resource)
         if (url != null) {
            return url
         }
      }

      final URL systemResource = ClassLoader.getSystemResource(resource)
      if (systemResource != null) {
         return systemResource
      } else {
         try {
            return new File(resource).toURI().toURL()
         } catch (MalformedURLException ignored) {
            return null
         }
      }
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
    * @param name        the name of the resource, use / to separate path entities.
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
         throw new FileNotFoundException("Failed to find resource " + name);
      }
      return file
   }

   static String getResourcePath(String name, String... encodingOpt) throws UnsupportedEncodingException {
      String encoding = encodingOpt.length > 0 ? encodingOpt[0] : "UTF-8"
      URL url = getResourceUrl(name)
      return URLDecoder.decode(url.getFile(), encoding)
   }
}
