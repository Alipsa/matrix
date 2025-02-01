package se.alipsa.matrix.parquet

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.util.ClassUtils

class RecordBuilder {
  private GroovyClassLoader loader
  private String name
  private Class cls
  private def imports
  private def fields
  private def methods

  RecordBuilder(GroovyClassLoader loader) {
    this.loader = loader
    imports = []
    fields = [:]
    methods = [:]
  }

  RecordBuilder(Object caller) {
    this(ClassUtils.findGroovyClassLoader(caller))
  }

  def setName(String name) {
    String n = name.substring(0, 1).toUpperCase() + name.substring(1)
    this.name = n
        .replace(' ', '')
        .replace('.', '_')
  }

  def addImport(Class importClass) {
    imports << "${importClass.getPackage().getName()}" +
        ".${importClass.getSimpleName()}"
  }

  def addField(String name, Class type) {
    fields[name] = type.simpleName
  }

  def addMethod(String name, Closure closure) {
    methods[name] = closure
  }

  Class<? extends Record> createRecord() {
    cls = loader.parseClass(createRecordDefinition())
    methods.each {
      cls.metaClass."$it.key" = it.value
    }
    return cls
  }

  String createRecordDefinition() {
    StringBuilder sb = new StringBuilder()
    imports.each {
      sb.append("import $it\n")
    }
    List<String> params = []
    fields.each {
      params << "$it.value $it.key".toString()
    }
    sb.append("record $name(${String.join(', ', params)}) {}")
    //println("record class def is: ${sb.toString()}")
    sb.toString()
  }

  Map createRecordRows(Matrix matrix, Map config = [:]) {
    if (matrix.rowCount() < 1) {
      return [:]
    }
    Row row = matrix.row(0)
    setName('Row_' + (matrix.matrixName ?: ''))
    int precision = config.getOrDefault('precision', 5) as int
    int scale = config.getOrDefault('scale', 0) as int
    row.eachWithIndex { v, idx ->
      addField(row.columnName(idx), v.class)
      if (v instanceof BigDecimal) {
        precision = Math.max(precision, v.precision())
        scale = Math.max(scale, v.scale())
      }
    }
    def rec = createRecord()
    List<? extends Record> recordRows = []
    matrix.each { Row r ->
      recordRows << rec.newInstance(r.toArray())
    }
    [rows: recordRows, precision: precision, scale:scale, recordClass: rec]
  }
}
