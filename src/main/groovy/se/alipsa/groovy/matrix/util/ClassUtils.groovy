package se.alipsa.groovy.matrix.util

class ClassUtils {

  static Class<?> primitiveWrapper(Class<?> it) {
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

  static List<Class<?>> convertPrimitivesToWrapper(List<Class<?>> types) {
    List<Class<?>> safeTypes = []
    types.each {
      if (it.isPrimitive()) {
        safeTypes.add(primitiveWrapper(it))
      } else {
        safeTypes.add(it)
      }
    }
    return safeTypes
  }

  static Class<?> findClosestCommonSuper(Class<?> updatedClass, Class<?> columnType) {
    if (updatedClass == null || columnType == null) {
      return Object
    }
    if (updatedClass.isPrimitive()) {
      Class<?> wrapper = primitiveWrapper(updatedClass)
      if (wrapper == columnType) {
        return columnType
      }
      if (wrapper instanceof Number && columnType instanceof Number) {
        return Number
      }
      return Object
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
}
