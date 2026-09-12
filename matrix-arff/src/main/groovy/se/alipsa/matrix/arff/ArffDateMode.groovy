package se.alipsa.matrix.arff

/**
 * How DATE attribute values are parsed and formatted.
 */
enum ArffDateMode {

  /**
   * The default: UTC, {@code Locale.ROOT}, and the whole value must match the pattern. Values do not depend on the
   * machine that reads or writes them.
   */
  UTC,

  /**
   * What {@code weka.core.Attribute} does: the JVM's default time zone and locale, and text after the date is
   * ignored ({@code SimpleDateFormat.parse(String)}). Produces the same {@code Date} instants as Weka on the same
   * machine.
   */
  WEKA

}
