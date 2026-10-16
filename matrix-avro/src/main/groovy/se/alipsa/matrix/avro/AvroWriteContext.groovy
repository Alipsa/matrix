package se.alipsa.matrix.avro

import groovy.transform.PackageScope

import se.alipsa.matrix.avro.exceptions.AvroSchemaException

import java.util.concurrent.ConcurrentHashMap

/** Per-cell key validation shared by compatibility checking and conversion. */
@PackageScope
final class AvroWriteContext {

  static final String ROOT_PATH = '<root>'

  private static final Set<Class<?>> STRING_KEY_MAP_CLASSES = [
      HashMap, LinkedHashMap, ConcurrentHashMap,
      Map.of().getClass(), Map.of('a', 1).getClass(), Map.of('b', 1, 'c', 2).getClass()
  ] as Set<Class<?>>
  private Map firstMap
  private MapKeys firstKeys
  private Map<Map, MapKeys> maps
  private final String columnName

  AvroWriteContext(String columnName = null) {
    this.columnName = columnName
  }

  MapKeys keys(Map input, String path) {
    if (input.is(firstMap)) {
      return firstKeys
    }
    MapKeys cached = maps == null ? null : maps.get(input)
    if (cached != null) {
      return cached
    }
    boolean ordinary = STRING_KEY_MAP_CLASSES.contains(input.getClass()) ||
        (input.getClass() == TreeMap && ((TreeMap) input).comparator() == null)
    boolean stringsOnly = ordinary
    Map<String, Object> originals = ordinary ? null : [:]
    for (Map.Entry entry : input.entrySet()) {
      Object key = entry.key
      if (key == null) {
        throw new AvroSchemaException("Null map key at ${path ?: ROOT_PATH}", columnName, null, null)
      }
      if (originals == null && !(key instanceof String)) {
        stringsOnly = false
        originals = [:]
        // Previous String keys are unique in an allowlisted map. Seed them once.
        for (Map.Entry preceding : input.entrySet()) {
          if (preceding.key.is(key)) {
            break
          }
          originals.put((String) preceding.key, preceding.key)
        }
      }
      if (originals != null) {
        String name = String.valueOf(key)
        if (originals.containsKey(name)) {
          Object previous = originals.get(name)
          throw new AvroSchemaException(
              "Clashing map keys '${previous}' (${previous.getClass().simpleName}) and " +
                  "'${key}' (${key.getClass().simpleName}) normalize to '$name' at ${path ?: ROOT_PATH}",
              columnName, null, null)
        }
        originals.put(name, key)
      }
    }
    MapKeys result = new MapKeys(input, stringsOnly ? input : null, originals)
    if (firstMap == null) {
      firstMap = input
      firstKeys = result
    } else {
      if (maps == null) {
        maps = new IdentityHashMap<>()
      }
      maps.put(input, result)
    }
    result
  }

  /** Validated lookup; ordinary String maps reuse the caller's map. */
  static final class MapKeys {

    private final Map input
    private Map<String, Object> normalized
    private final Map<String, Object> originals

    MapKeys(Map input, Map<String, Object> normalized, Map<String, Object> originals) {
      this.input = input
      this.normalized = normalized
      this.originals = originals
    }

    Map<String, Object> getNormalized() {
      if (normalized == null) {
        normalized = [:]
        originals.each { String name, Object key -> normalized.put(name, input.get(key)) }
      }
      normalized
    }

    Object value(String name) {
      try {
        Object direct = input.get(name)
        if (input.is(normalized) || direct != null || input.containsKey(name)) {
          return direct
        }
      } catch (ClassCastException ignored) {
        // Sorted maps may reject a String probe when their keys are enums or other types.
      }
      getNormalized().get(name)
    }
  }
}
