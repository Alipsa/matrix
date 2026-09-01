package se.alipsa.matrix.gg.coord

import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Map projection coordinate system for lon/lat data.
 * Provides basic projections without external dependencies.
 */
class CoordMap extends CoordTrans {

  private static final String DEFAULT_PROJECTION = 'mercator'

  /** Clamp bound for latitudes near the poles, where tan() approaches infinity */
  private static final BigDecimal MAX_MERCATOR_LATITUDE = 85.05

  /** The factor of 2 in the Mercator/Gudermannian formula (both forward and inverse) */
  private static final BigDecimal TWO = 2 as BigDecimal

  /** Projection name (mercator, equirectangular, identity) */
  String projection = DEFAULT_PROJECTION

  CoordMap() {
    super()
    applyProjection(projection)
  }

  CoordMap(Map params) {
    super()
    if (params.projection) {
      this.projection = params.projection as String
    }
    if (params.xlim) {
      this.xlim = params.xlim as List<Number>
    }
    if (params.ylim) {
      this.ylim = params.ylim as List<Number>
    }
    applyProjection(this.projection)
  }

  private void applyProjection(String projection) {
    String proj = projection?.toLowerCase(Locale.ROOT) ?: DEFAULT_PROJECTION
    switch (proj) {
      case 'mercator' -> {
        this.xTrans = Transformations.fromClosures({ Number x ->
          (x as BigDecimal).toRadians()
        }) { Number x ->
          (x as BigDecimal).toDegrees()
        }
        this.yTrans = Transformations.fromClosures({ Number y ->
          BigDecimal lat = y as BigDecimal
          // Clamp extreme latitudes to prevent tan() approaching infinity near poles
          if (lat.abs() > MAX_MERCATOR_LATITUDE) {
            lat = lat.signum() * MAX_MERCATOR_LATITUDE
          }
          BigDecimal rad = lat.toRadians()
          (PI / 4 + rad / TWO).tan().log()
        }) { Number y ->
          BigDecimal v = y as BigDecimal
          (TWO * v.exp().atan() - PI / TWO).toDegrees()
        }
      }
      case 'equirectangular', 'identity' -> {
        this.xTrans = new Transformations.IdentityTrans()
        this.yTrans = new Transformations.IdentityTrans()
      }
      default -> throw new IllegalArgumentException("Unsupported projection: $projection")
    }
  }
}
