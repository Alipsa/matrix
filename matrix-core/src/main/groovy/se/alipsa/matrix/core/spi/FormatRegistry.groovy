package se.alipsa.matrix.core.spi

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.util.Logger

/**
 * Singleton registry that discovers and caches {@link MatrixFormatProvider}
 * implementations via {@link ServiceLoader}.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Look up a provider by extension
 * MatrixFormatProvider csv = FormatRegistry.instance.getProvider('csv')
 *
 * // List all registered extensions
 * Set<String> exts = FormatRegistry.instance.supportedExtensions()
 *
 * // Human-readable summary
 * println FormatRegistry.instance.describe()
 *
 * // List read/write options for a specific extension
 * println FormatRegistry.instance.listReadOptions('csv')
 * println FormatRegistry.instance.listWriteOptions('csv')
 * }</pre>
 *
 * @see MatrixFormatProvider
 */
@CompileStatic
class FormatRegistry {

  private static final Logger log = Logger.getLogger(FormatRegistry)

  @SuppressWarnings('unused')
  private static final FormatRegistry INSTANCE = new FormatRegistry()

  private final Map<String, MatrixFormatProvider> providers = [:]
  private volatile boolean loaded = false
  private final Object lock = new Object()

  private FormatRegistry() {
    // Singleton
  }

  /**
   * Returns the singleton instance.
   *
   * @return the FormatRegistry instance
   */
  static FormatRegistry getInstance() {
    INSTANCE
  }

  /**
   * Returns the provider for the given file extension.
   *
   * @param extension the file extension (lowercase, no dot)
   * @return the provider, or null if no provider handles this extension
   */
  MatrixFormatProvider getProvider(String extension) {
    ensureLoaded()
    providers[extension?.toLowerCase()]
  }

  /**
   * Returns all registered file extensions.
   *
   * @return set of supported extensions
   */
  Set<String> supportedExtensions() {
    ensureLoaded()
    Collections.unmodifiableSet(providers.keySet())
  }

  /**
   * Returns a human-readable summary of all registered providers.
   *
   * @return formatted description of all providers and their options
   */
  String describe() {
    ensureLoaded()
    if (providers.isEmpty()) {
      return 'No format providers registered. Add a matrix-* format module to the classpath.'
    }

    // Collect unique providers (multiple extensions can map to the same provider)
    Map<MatrixFormatProvider, Set<String>> providerExtensions = [:]
    providers.each { String ext, MatrixFormatProvider p ->
      providerExtensions.computeIfAbsent(p) { new LinkedHashSet<String>() }.add(ext)
    }

    StringBuilder sb = new StringBuilder()
    sb.append('Registered Format Providers:\n')
    sb.append('=' * 60).append('\n')

    providerExtensions.each { MatrixFormatProvider p, Set<String> exts ->
      sb.append("\n${p.formatName()}\n")
      sb.append("  Extensions: ${exts.join(', ')}\n")
      sb.append("  Read: ${p.canRead() ? 'yes' : 'no'}")
      sb.append("  Write: ${p.canWrite() ? 'yes' : 'no'}\n")

      if (p.canRead()) {
        List<OptionDescriptor> readOpts = p.readOptionDescriptors()
        if (readOpts) {
          sb.append("\n  Read Options:\n")
          sb.append(indent(OptionDescriptor.describe(readOpts), '  ')).append('\n')
        }
      }
      if (p.canWrite()) {
        List<OptionDescriptor> writeOpts = p.writeOptionDescriptors()
        if (writeOpts) {
          sb.append("\n  Write Options:\n")
          sb.append(indent(OptionDescriptor.describe(writeOpts), '  ')).append('\n')
        }
      }
    }
    sb.toString()
  }

  /**
   * Returns a human-readable description of the read options
   * for the format associated with the given file extension.
   *
   * @param fileExtension the file extension (e.g. {@code 'csv'}, {@code 'json'})
   * @return formatted option table, or a message if the extension is unknown
   */
  String listReadOptions(String fileExtension) {
    MatrixFormatProvider provider = resolveProvider(fileExtension)
    if (provider == null) {
      return noProviderMessage(fileExtension)
    }
    if (!provider.canRead()) {
      return "${provider.formatName()} (*.${fileExtension?.toLowerCase()}) does not support reading."
    }
    List<OptionDescriptor> descriptors = provider.readOptionDescriptors()
    "${provider.formatName()} (*.${fileExtension?.toLowerCase()}) Read Options\n" +
        ('=' * 60) + '\n' +
        (descriptors ? OptionDescriptor.describe(descriptors) : 'No options available.')
  }

  /**
   * Returns a human-readable description of the write options
   * for the format associated with the given file extension.
   *
   * @param fileExtension the file extension (e.g. {@code 'csv'}, {@code 'json'})
   * @return formatted option table, or a message if the extension is unknown
   */
  String listWriteOptions(String fileExtension) {
    MatrixFormatProvider provider = resolveProvider(fileExtension)
    if (provider == null) {
      return noProviderMessage(fileExtension)
    }
    if (!provider.canWrite()) {
      return "${provider.formatName()} (*.${fileExtension?.toLowerCase()}) does not support writing."
    }
    List<OptionDescriptor> descriptors = provider.writeOptionDescriptors()
    "${provider.formatName()} (*.${fileExtension?.toLowerCase()}) Write Options\n" +
        ('=' * 60) + '\n' +
        (descriptors ? OptionDescriptor.describe(descriptors) : 'No options available.')
  }

  /**
   * Reloads all providers from the ServiceLoader.
   * Useful for testing.
   */
  void reload() {
    synchronized (lock) {
      providers.clear()
      loaded = false
      ensureLoaded()
    }
  }

  /**
   * Extracts the file extension (lowercase, no dot) from a filename.
   *
   * @param fileName the filename to extract from
   * @return the extension, or empty string if none
   */
  static String extractExtension(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return ''
    }
    int lastSeparator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'))
    String baseName = lastSeparator >= 0 ? fileName.substring(lastSeparator + 1) : fileName
    if (baseName.isEmpty()) {
      return ''
    }
    int lastDot = baseName.lastIndexOf('.')
    if (lastDot < 0 || lastDot == baseName.length() - 1) {
      return ''
    }
    baseName.substring(lastDot + 1).toLowerCase()
  }

  private MatrixFormatProvider resolveProvider(String fileExtension) {
    ensureLoaded()
    String ext = fileExtension?.toLowerCase()
    providers[ext]
  }

  private String noProviderMessage(String fileExtension) {
    String ext = fileExtension?.toLowerCase()
    "No provider found for extension '${ext}'. Available: ${providers.keySet().join(', ')}"
  }

  private void ensureLoaded() {
    if (loaded) {
      return
    }
    synchronized (lock) {
      if (loaded) {
        return
      }
      ServiceLoader<MatrixFormatProvider> loader = ServiceLoader.load(MatrixFormatProvider)
      for (MatrixFormatProvider provider : loader) {
        for (String ext : provider.supportedExtensions()) {
          String lowerExt = ext.toLowerCase()
          if (providers.containsKey(lowerExt)) {
            log.warn("Extension '${lowerExt}' already registered by ${providers[lowerExt].formatName()}, " +
                "ignoring ${provider.formatName()}")
          } else {
            providers[lowerExt] = provider
          }
        }
        log.debug("Registered format provider: ${provider.formatName()} [${provider.supportedExtensions().join(', ')}]")
      }
      loaded = true
    }
  }

  private static String indent(String text, String prefix) {
    text.readLines().collect { prefix + it }.join('\n')
  }
}
