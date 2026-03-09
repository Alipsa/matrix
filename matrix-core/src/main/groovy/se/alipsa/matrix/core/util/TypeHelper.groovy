package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic

import static se.alipsa.matrix.core.util.ClassUtils.convertPrimitivesToWrapper

/**
 * Internal helper for type management operations.
 */
@CompileStatic
class TypeHelper {

  private static final Logger log = Logger.getLogger(TypeHelper)

  /**
   * Create a list of Object types sized to the supplied template.
   *
   * @param template a collection used only for sizing
   * @return a list of Object classes matching the template size
   */
  static List<Class> createObjectTypes(Collection template) {
    [Object] * template.size() as List<Class>
  }

  /**
   * Sanitize and validate a type list against the header, converting primitives
   * to wrapper types and filling in Object types if none are provided.
   *
   * @param headerList the column names
   * @param dataTypesOpt optional type list
   * @return a validated list of types matching the header size
   */
  static List<Class> sanitizeTypes(Collection<String> headerList, List<Class>... dataTypesOpt) {
    List<Class> types = []
    if (dataTypesOpt.length > 0) {
      types = convertPrimitivesToWrapper(dataTypesOpt[0])
      if (types.isEmpty()) {
        types = createObjectTypes(headerList)
      }
      if (headerList.size() != types.size()) {
        log.warn("Headers (${headerList.size()} elements): $headerList")
        log.warn("Types:  (${types.size()} elements): ${types.collect { it.simpleName }}")
        throw new IllegalArgumentException("Number of columns (${headerList.size()}) differs from number of datatypes provided (${types.size()})")
      }
    }
    if (types.isEmpty()) {
      types = createObjectTypes(headerList)
    }
    types
  }
}
