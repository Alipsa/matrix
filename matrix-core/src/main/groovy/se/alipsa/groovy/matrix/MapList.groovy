package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic

@CompileStatic
class MapList<M,L> {

  Map<M, List<L>> data = new LinkedHashMap<>()

  void add(M key, L value) {
    getOrCreate(key).add(value)
  }

  /**
   * Fetches the entire list associated with the key
   * <code>
   *   MapList<Integer, String> ml = new MapList<>()
   *   ml[1] = 'Foo'
   *   ml[1] = 'Bar'
   *   ml[2] = 'Baz'
   *   List<String> first = ml.get(1)
   * </code>
   * @param key the key parameter
   * @return the List corresponding to the key
   */
  // Note: naming this getAt breaks the other getAt method
  List<L> get(M key) {
    return data.get(key)
  }

  /**
   * Allows for short square bracket notation to get a value, e.g:
   * <code>
   *   MapList<Integer, String> ml = new MapList<>()
   *   ml[1] = 'Foo'
   *   ml[1] = 'Bar'
   *   ml[2] = 'Baz'
   *   assert 'Foo' == m[1, 0]
   * </code>
   * @param key the key parameter
   * @param index the index to the list value to get
   * @return the value corresponding to the List index belonging to the key
   */
  L getAt(M key, Number index) {
    List<L> list = data.get(key)
    return list == null ? null : list[index.intValue()]
  }

  /**
   * Allows for shorthand square bracket notation to add values , e.g:
   * <code>
   *   MapList<Integer, String> ml = new MapList<>()
   *   ml[1] = 'Foo' // adds 'Foo' to the list associated with the 1 key
   * @param key
   * @param value
   */
  void putAt(M key, L value) {
    add(key, value)
  }

  L remove(M key, Number index) {
    List<L> list = data.get(key)
    if (list != null) {
      return list.remove(index.intValue())
    }
    return null
  }

  private List<L> getOrCreate(M key) {
    data.computeIfAbsent(key, k -> new ArrayList<L>())
  }

  @Override
  String toString() {
    return String.valueOf(data)
  }
}
