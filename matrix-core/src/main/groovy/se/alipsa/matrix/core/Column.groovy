package se.alipsa.matrix.core

/**
 * A column is a list with some arithmetic operations changed compared to how lists normally behaves in Groovy.
 * the multiply, div, plus, minus, and power applies to each element in the list instead of on the list itself. E.g.
 * new Column([1,2,3]) * 2 == [2,4,6] instead of [2,4,6,2,4,6] which the default result would on a list i Groovy.
 */
class Column extends ArrayList {

  String name
  Class type

  Column(int initialCapacity) {
    this(initialCapacity, Object)
  }

  Column() {
    this(Object)
  }

  Column(Collection<?> c) {
    this(c, Object)
  }

  Column(int initialCapacity, Class type) {
    super(initialCapacity)
    this.type = type
  }

  Column(Class type) {
    this.type = type
  }

  Column(Collection c, Class type) {
    super(c)
    this.type = type
  }

  Column(String name, Collection c) {
    super(c)
    this.name = name
  }

  Column(String name, Collection c, Class type) {
    super(c)
    this.type = type
    this.name = name
  }

  Column(String name, Class type) {
    this.name = name
    this.type = type
  }

  List plus(Object val) {
    List result = new Column()
    this.each {
      if (val == null || it == null) {
        result.add(null)
      } else {
        result.add(it + val)
      }
    }
    result
  }

  List plus(List list) {
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it + (val as Number))
      } else if (it instanceof Character) {
        result.add(it + (val as Character))
      } else if (it instanceof String) {
        result.add(it + (val as Character))
      } else {
        result.add(it + val)
      }

    }
    result
  }

  List minus(Object val) {
    List result = new Column()
    this.each {
      if (val == null || it == null) {
        result.add(null)
      } else {
        result.add(it - val)
      }
    }
    result
  }

  List minus(List list) {
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it - (val as Number))
      } else if (it instanceof Character) {
        result.add(it - (val as Character))
      } else if (it instanceof String) {
        result.add(it - (val as Character))
      } else {
        result.add(it - val)
      }
    }
    result
  }

  List multiply(Number val) {
    List result = new Column()
    this.each {
      if (val == null || it == null) {
        result.add(null)
      } else {
        result.add(it * val)
      }
    }
    result
  }

  List multiply(List list) {
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it * (val as Number))
      } else {
        result.add(it * val)
      }
    }
    result
  }

  List div(Number val) {
    List result = new Column()
    this.each {
      if (val == null || it == null) {
        result.add(null)
      } else {
        result.add(it / val)
      }
    }
    result
  }

  List div(List list) {
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it / (val as Number))
      } else {
        result.add(it / val)
      }
    }
    result
  }

  List power(Number val) {
    List result = new Column()
    this.each {
      if (val == null || it == null) {
        result.add(null)
      } else {
        result.add(it ** val)
      }
    }
    result
  }

  List power(List list) {
    List result = new Column()
    def that = fill(list)
    this.eachWithIndex {it, idx ->
      def val = that[idx]
      if (it == null || val == null) {
        result.add(null)
      } else if (it instanceof Number) {
        result.add(it ** (val as Number))
      } else {
        result.add(it ** val)
      }
    }
    result
  }

  private Column fill(List list) {
    def that = new Column(list)
    int listSize = list.size()
    int size = this.size()
    if (listSize < size) {
      that.addAll([null]*(size-listSize))
    }
    that
  }

  List subList(IntRange range) {
    this.subList(range.min(), range.max() +1)
  }
}
