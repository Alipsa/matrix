package se.alipsa.matrix.gg.coord

import groovy.transform.CompileStatic

import java.util.Locale

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
      case 'mercator':
        this.xTrans = Transformations.fromClosures({ Number x ->
          Math.toRadians(x as double)
        }, { Number x ->
          Math.toDegrees(x as double)
        })
        this.yTrans = Transformations.fromClosures({ Number y ->
          double lat = y as double
          double rad = Math.toRadians(lat)
          Math.log(Math.tan(Math.PI / 4d + rad / 2d))
        }, { Number y ->
          double v = y as double
          Math.toDegrees(2d * Math.atan(Math.exp(v)) - Math.PI / 2d)
        })
        break
      case 'equirectangular':
      case 'identity':
        this.xTrans = new Transformations.IdentityTrans()
        this.yTrans = new Transformations.IdentityTrans()
        break
      default:
        throw new IllegalArgumentException("Unsupported projection: $projection")
    }
  }
}
