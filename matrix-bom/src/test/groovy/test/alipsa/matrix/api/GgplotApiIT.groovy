package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static se.alipsa.matrix.gg.GgPlot.*

/** Exercises the released ggplot facade against the BOM-selected charts runtime. */
@Tag('ggplot')
class GgplotApiIT implements ApiItSupport {

  @Test
  void ggplotGeomsScalesFacetsLabelsAndThemeRender() {
    def chart = ggplot(mtcars(), aes(x: 'wt', y: 'mpg', color: 'cyl')) +
        geom_point() +
        geom_smooth() +
        facet_wrap([facets: 'cyl', ncol: 2]) +
        scale_x_continuous() +
        scale_y_continuous() +
        labs(title: 'gg API', x: 'weight') +
        theme_minimal()
    assertNotNull(chart)
    assertEquals('gg API', chart.labels.title)
    assertRenderedSvg(chart.render())
  }
}
