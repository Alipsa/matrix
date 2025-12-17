package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic
import jdk.internal.reflect.Reflection

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
   * @return the the nearest common Number instance that can contain instances of both classes
   */
  static Class<? extends Number> findNearestNumberClass(Class<? extends Number> updated, Class<? extends Number> columnClass) {
    Class<? extends Number> aToB = nearest(updated, columnClass)
    if (aToB == Number) {
      aToB = nearest(columnClass, updated)
    }
    return aToB
  }

  private static nearest(Class<? extends Number> updated, Class<? extends Number> columnClass) {
    if (updated == Byte && columnClass == Short) {
      return Short
    }
    if (updated == Byte && columnClass == Integer) {
      return Integer
    }
    if (updated == Byte && columnClass == Long) {
      return Long
    }
    if (updated == Byte && columnClass == BigInteger) {
      return BigInteger
    }
    if (updated == Byte && columnClass == Float) {
      return Float
    }
    if (updated == Byte && columnClass == Double) {
      return Double
    }
    if (updated == Byte && columnClass == BigDecimal) {
      return BigDecimal
    }

    if (updated == Short && columnClass == Integer) {
      return Integer
    }
    if (updated == Short && columnClass == Long) {
      return Long
    }
    if (updated == Short && columnClass == BigInteger) {
      return BigInteger
    }
    if (updated == Short && columnClass == Float) {
      return Float
    }
    if (updated == Short && columnClass == Double) {
      return Double
    }
    if (updated == Short && columnClass == BigDecimal) {
      return BigDecimal
    }

    if (updated == Integer && columnClass == Long) {
      return Long
    }
    if (updated == Integer && columnClass == BigInteger) {
      return BigInteger
    }
    if (updated == Integer && columnClass == Float) {
      return Float
    }
    if (updated == Integer && columnClass == Double) {
      return Double
    }
    if (updated == Integer && columnClass == BigDecimal) {
      return BigDecimal
    }

    if (updated == Long && columnClass == Float) {
      return Double
    }
    if (updated == Long && columnClass == BigInteger) {
      return BigInteger
    }
    if (updated == Long && columnClass == Double) {
      return Double
    }
    if (updated == Long && columnClass == BigDecimal) {
      return BigDecimal
    }
    if (updated == BigInteger && columnClass == BigDecimal) {
      return BigDecimal
    }

    if (updated == Float && columnClass == Double) {
      return Double
    }
    if (updated == Float && columnClass == BigInteger) {
      return BigDecimal
    }
    if (updated == Float && columnClass == BigDecimal) {
      return BigDecimal
    }

    if (updated == Double && columnClass == BigInteger) {
      return BigDecimal
    }
    if (updated == Double && columnClass == BigDecimal) {
      return BigDecimal
    }

    return Number
  }

  static GroovyClassLoader findGroovyClassLoader(Object obj) {

    ClassLoader cl = obj.class.classLoader
    while(cl != null && !(cl instanceof GroovyClassLoader)) {
      cl = cl.parent
    }
    cl as GroovyClassLoader ?: new GroovyClassLoader()
  }
}
