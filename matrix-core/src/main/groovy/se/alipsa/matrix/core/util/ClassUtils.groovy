package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic

@CompileStatic
class ClassUtils {

  static Class primitiveWrapper(Class it) {
    if (it == boolean) {
      return Boolean
    } else if (it == byte) {
      return Byte
    } else if (it == char) {
      return Character
    } else if (it == short) {
      return Short
    } else if (it == int) {
      return Integer
    } else if (it == long) {
      return Long
    } else if (it == float) {
      return Float
    } else if (it == double) {
      return Double
    }
    // not a primitive so return what we got
    return it
  }

  static Class convertPrimitiveToWrapper(Class type) {
    if (type != null && type.isPrimitive()) {
      return primitiveWrapper(type)
    }
    return type
  }

  static List<Class> convertPrimitivesToWrapper(List<Class> types) {
    List<Class> safeTypes = []
    types.each {
      if (it != null && it.isPrimitive()) {
        safeTypes.add(primitiveWrapper(it))
      } else {
        safeTypes.add(it)
      }
    }
    return safeTypes
  }

  static Class findClosestCommonSuper(Class updatedClass, Class columnType) {
    if (updatedClass == null || columnType == null) {
      return Object
    }
    if (updatedClass.isPrimitive()) {
      updatedClass = primitiveWrapper(updatedClass)
    }
    if (columnType.isPrimitive()) {
      columnType = primitiveWrapper(columnType)
    }
    if (columnType.isAssignableFrom(updatedClass)) {
      return columnType
    }
    if (Number.isAssignableFrom(updatedClass) && Number.isAssignableFrom(columnType) ) {
      return findNearestNumberClass(updatedClass as Class<? extends Number>, columnType as Class<? extends Number>)
    }

    def previous = updatedClass
    while (!updatedClass.isAssignableFrom(columnType)) {
      updatedClass = updatedClass.getSuperclass()
      if (updatedClass == null) {
        return previous
      }
      previous = updatedClass
    }

    return updatedClass
  }

  /**
   * Since all primitive wrappers extends Number, the simple solution would be to just
   * return Number but actually there is a kind of hiearchy e.g. a Flloat fits in a Double etc.
   * so this returns the nearest common Number instance that can contain both.
   * Handles Byte, Short, Integer, Long, BigInteger, Float, Double, and BigDecimal
   * @param updated the "new" class to try to fit into the existing one
   * @param columnClass the existing class where the new class is trying to fit
   * @return the nearest common Number instance that can contain instances of both classes
   */
  static Class<? extends Number> findNearestNumberClass(Class<? extends Number> updated, Class<? extends Number> columnClass) {
    Class<? extends Number> aToB = nearest(updated, columnClass)
    if (aToB == Number) {
      aToB = nearest(columnClass, updated)
    }
    return aToB
  }

  private static final Map<String, Class<? extends Number>> NEAREST_TYPE_MAP = [
      'Byte_Short': Short,
      'Byte_Integer': Integer,
      'Byte_Long': Long,
      'Byte_BigInteger': BigInteger,
      'Byte_Float': Float,
      'Byte_Double': Double,
      'Byte_BigDecimal': BigDecimal,
      'Short_Integer': Integer,
      'Short_Long': Long,
      'Short_BigInteger': BigInteger,
      'Short_Float': Float,
      'Short_Double': Double,
      'Short_BigDecimal': BigDecimal,
      'Integer_Long': Long,
      'Integer_BigInteger': BigInteger,
      'Integer_Float': Float,
      'Integer_Double': Double,
      'Integer_BigDecimal': BigDecimal,
      'Long_Float': Double,
      'Long_BigInteger': BigInteger,
      'Long_Double': Double,
      'Long_BigDecimal': BigDecimal,
      'BigInteger_BigDecimal': BigDecimal,
      'Float_Double': Double,
      'Float_BigInteger': BigDecimal,
      'Float_BigDecimal': BigDecimal,
      'Double_BigInteger': BigDecimal,
      'Double_BigDecimal': BigDecimal
  ]

  private static nearest(Class<? extends Number> updated, Class<? extends Number> columnClass) {
    String key = "${updated.simpleName}_${columnClass.simpleName}"
    return NEAREST_TYPE_MAP.get(key, Number)
  }

  static GroovyClassLoader findGroovyClassLoader(Object obj) {

    ClassLoader cl = obj.class.classLoader
    while(cl != null && !(cl instanceof GroovyClassLoader)) {
      cl = cl.parent
    }
    cl as GroovyClassLoader ?: new GroovyClassLoader()
  }
}
