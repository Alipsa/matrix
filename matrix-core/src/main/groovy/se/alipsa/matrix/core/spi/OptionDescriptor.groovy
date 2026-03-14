package se.alipsa.matrix.core.spi

import groovy.transform.CompileStatic

/**
 * Describes a single configuration option for a format provider.
 *
 * <p>Used by {@link MatrixFormatProvider#readOptionDescriptors()} and
 * {@link MatrixFormatProvider#writeOptionDescriptors()} to advertise which
 * options a provider supports, along with their types, defaults, and
 * human-readable descriptions.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * def desc = new OptionDescriptor('delimiter', Character, ',', 'The character used to separate values', false)
 * println OptionDescriptor.describe([desc])
 * }</pre>
 *
 * @see MatrixFormatProvider
 */
@CompileStatic
class OptionDescriptor {

  /** The option key name, e.g. {@code 'delimiter'} */
  String name

  /** The expected value type, e.g. {@code Character} */
  Class type

  /** Human-readable default value, e.g. {@code ","} */
  String defaultValue

  /** Human-readable description of what this option does */
  String description

  /** Whether this option is required */
  boolean required

  /**
   * Creates a new OptionDescriptor.
   *
   * @param name the option key name
   * @param type the expected value type
   * @param defaultValue human-readable default value (null if none)
   * @param description what this option does
   * @param required whether this option must be provided
   */
  OptionDescriptor(String name, Class type, String defaultValue, String description, boolean required = false) {
    this.name = name
    this.type = type
    this.defaultValue = defaultValue
    this.description = description
    this.required = required
  }

  /**
   * Formats a list of option descriptors as a human-readable table.
   *
   * @param descriptors the option descriptors to format
   * @return a formatted table string
   */
  static String describe(List<OptionDescriptor> descriptors) {
    if (descriptors == null || descriptors.isEmpty()) {
      return 'No options available.'
    }

    // Calculate column widths
    int nameWidth = 'Option'.length()
    int typeWidth = 'Type'.length()
    int defaultWidth = 'Default'.length()
    int requiredWidth = 'Required'.length()
    int descWidth = 'Description'.length()

    for (OptionDescriptor d : descriptors) {
      nameWidth = Math.max(nameWidth, (d.name ?: '').length())
      typeWidth = Math.max(typeWidth, (d.type?.simpleName ?: '').length())
      defaultWidth = Math.max(defaultWidth, (d.defaultValue ?: '').length())
      descWidth = Math.max(descWidth, (d.description ?: '').length())
    }

    String fmt = "| %-${nameWidth}s | %-${typeWidth}s | %-${defaultWidth}s | %-${requiredWidth}s | %-${descWidth}s |"
    String sep = "+-${'-' * nameWidth}-+-${'-' * typeWidth}-+-${'-' * defaultWidth}-+-${'-' * requiredWidth}-+-${'-' * descWidth}-+"

    StringBuilder sb = new StringBuilder()
    sb.append(sep).append('\n')
    sb.append(String.format(fmt, 'Option', 'Type', 'Default', 'Required', 'Description')).append('\n')
    sb.append(sep).append('\n')
    for (OptionDescriptor d : descriptors) {
      sb.append(String.format(fmt,
          d.name ?: '',
          d.type?.simpleName ?: '',
          d.defaultValue ?: '',
          d.required ? 'yes' : 'no',
          d.description ?: ''
      )).append('\n')
    }
    sb.append(sep)
    sb.toString()
  }
}
