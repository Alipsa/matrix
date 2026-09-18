package tech.tablesaw.io;

import java.util.Locale;
import java.util.Set;

/** Collision-safe column naming shared by the table readers. */
public final class ColumnNames {

  private ColumnNames() {}

  /**
   * Returns {@code candidate}, or {@code candidate + "-" + k} (k = 2, 3, ...) when the lowercased
   * candidate is already in {@code taken}; the returned name (lowercased) is added to {@code taken}.
   *
   * @param candidate the desired column name
   * @param taken lowercased names that are no longer available; updated in place
   * @return a name that does not collide, case-insensitively, with any name in {@code taken}
   */
  public static String unique(String candidate, Set<String> taken) {
    String result = candidate;
    String normalized = result.toLowerCase(Locale.ROOT);
    int suffix = 2;
    while (taken.contains(normalized)) {
      result = candidate + "-" + suffix++;
      normalized = result.toLowerCase(Locale.ROOT);
    }
    taken.add(normalized);
    return result;
  }
}
