package se.alipsa.matrix.gg.coord

import groovy.transform.CompileStatic

import java.util.Locale

import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Map projection coordinate system for lon/lat data.
 * Provides basic projections without external dependencies.
 */
@CompileStatic
class CoordMap extends CoordTrans {

  /** Projection name (mercator, equirectangular, identity) */
  String projection = 'mercator'

  CoordMap() {
    super()
    applyProjection(projection)
  }

  CoordMap(Map params) {
    super()
    if (params.projection) this.projection = params.projection as String
    if (params.xlim) this.xlim = params.xlim as List<Number>
    if (params.ylim) this.ylim = params.ylim as List<Number>
    applyProjection(this.projection)
  }

  private void applyProjection(String projection) {
    String proj = projection?.toLowerCase(Locale.ROOT) ?: 'mercator'
    switch (proj) {
      case 'mercator' -> {
        this.xTrans = Transformations.fromClosures({ Number x ->
          (x as BigDecimal).toRadians()
        }, { Number x ->
          (x as BigDecimal).toDegrees()
        })
        this.yTrans = Transformations.fromClosures({ Number y ->
          BigDecimal lat = y as BigDecimal
          // Clamp extreme latitudes to prevent tan() approaching infinity near poles
          if (lat.abs() > 85.05) {
            lat = lat.signum() * 85.05
          }
          BigDecimal rad = lat.toRadians()
          (PI / 4 + rad / 2).tan().log()
        }, { Number y ->
          BigDecimal v = y as BigDecimal
          (2 * v.exp().atan() - PI / 2).toDegrees()
        })
      }
      case 'equirectangular', 'identity' -> {
        this.xTrans = new Transformations.IdentityTrans()
        this.yTrans = new Transformations.IdentityTrans()
      }
      default -> throw new IllegalArgumentException("Unsupported projection: $projection")
    }
  }
}
