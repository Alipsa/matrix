package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.render.scale.BrewerPalettes as CharmBrewerPalettes

/**
 * ColorBrewer palette definitions and helpers.
 *
 * @deprecated Use {@link se.alipsa.matrix.charm.render.scale.BrewerPalettes} instead.
 *             This stub delegates all calls to the charm implementation.
 */
@Deprecated
@CompileStatic
class BrewerPalettes {

  /** @see CharmBrewerPalettes#getPalette(String) */
  static List<String> getPalette(String name) {
    CharmBrewerPalettes.getPalette(name)
  }

  /** @see CharmBrewerPalettes#getPaletteNamesForType(String) */
  static List<String> getPaletteNamesForType(String type) {
    CharmBrewerPalettes.getPaletteNamesForType(type)
  }

  /** @see CharmBrewerPalettes#getPaletteNameByIndex(String, int) */
  static String getPaletteNameByIndex(String type, int index) {
    CharmBrewerPalettes.getPaletteNameByIndex(type, index)
  }

  /** @see CharmBrewerPalettes#selectPalette(String, int, int) */
  static List<String> selectPalette(String name, int n, int direction = 1) {
    CharmBrewerPalettes.selectPalette(name, n, direction)
  }
}
