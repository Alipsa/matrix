package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.Type

/** Logical paths omit LIST and MAP physical wrappers. */
@PackageScope
final class NestedPaths {
  private NestedPaths() {}

  /** Escapes '.' and '\\' so dotted or backslash-containing names cannot collide with path separators. */
  static String escape(String name) { name.replace('\\', '\\\\').replace('.', '\\.') }

  /** Logical path of a top-level field. */
  static String root(String name) { escape(name) }

  static String child(String path, String name) { path.empty ? escape(name) : "${path}.${escape(name)}".toString() }

  static boolean isMap(GroupType group, boolean parentIsMap = false) {
    group.logicalTypeAnnotation instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation ||
        (!parentIsMap && group.logicalTypeAnnotation instanceof LogicalTypeAnnotation.MapKeyValueTypeAnnotation &&
        group.fieldCount == 1 && !group.getType(0).primitive && group.getType(0).repetition == Type.Repetition.REPEATED &&
        group.getType(0).asGroupType().fieldCount in [1, 2])
  }

  static boolean wrappedList(GroupType group) {
    Type repeated = group.getType(0)
    !repeated.primitive && repeated.asGroupType().fieldCount == 1 &&
        !(repeated.name in ['array', "${group.name}_tuple".toString()])
  }

  static Map<String, Type> leaves(Type type, String path, boolean parentIsMap = false) {
    if (type.primitive) { return [(path): type] }
    GroupType group = type.asGroupType()
    if (group.logicalTypeAnnotation instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
      Type repeated = group.getType(0)
      return leaves(wrappedList(group) ? repeated.asGroupType().getType(0) : repeated, child(path, 'element'))
    }
    if (isMap(group, parentIsMap)) {
      Map<String, Type> result = [:]
      group.getType(0).asGroupType().fields.eachWithIndex { Type field, int index ->
        result.putAll(leaves(field, child(path, index == 0 ? 'key' : 'value')))
      }
      return result
    }
    Map<String, Type> result = [:]
    group.fields.each { Type field -> result.putAll(leaves(field, child(path, field.name))) }
    result
  }
}
