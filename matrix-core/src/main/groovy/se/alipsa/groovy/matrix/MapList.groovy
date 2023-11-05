package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic

@CompileStatic
class MapList<M,L> {

  Map<M, List<L>> data = new HashMap<>()

  void add(M key, L value) {
    getOrCreate(key).add(value)
  }

  L getAt(M key, int index) {
    List<L> list = data.get(key)
    return list == null ? null : list[index]
  }

  private List<L> getOrCreate(M key) {
    data.computeIfAbsent(key, k -> new ArrayList<L>())
  }
}
