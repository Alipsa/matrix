package gg

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.aes
import static se.alipsa.matrix.gg.GgPlot.geom_point
import static se.alipsa.matrix.gg.GgPlot.ggplot

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.TextAnnotationSpec
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.bridge.GgCharmCompilation
import se.alipsa.matrix.gg.bridge.GgCharmCompiler
import se.alipsa.matrix.gg.geom.GeomText
import se.alipsa.matrix.gg.layer.Layer

class TooltipBridgeTest {

  @Test
  void testTooltipAestheticIsPropagatedToCharmAndRendered() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'tip')
        .rows([
            [1, 2, 'first'],
            [2, 4, 'second']
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', tooltip: 'tip')) + geom_point()

    GgCharmCompilation adaptation = new GgCharmCompiler().adapt(chart)
    assertTrue(adaptation.delegated, adaptation.reasons.join('; '))
    assertEquals('tip', adaptation.charmChart.mapping.tooltip.columnName())

    String xml = SvgWriter.toXml(chart.render())
    assertTrue(xml.contains('<title>first</title>'))
    assertTrue(xml.contains('<title>second</title>'))
  }

  @Test
  void testTooltipColumnInLayerParamsIsPropagatedToCharm() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'tip')
        .rows([
            [1, 2, 'layer-first'],
            [2, 4, 'layer-second']
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) + geom_point(tooltip: 'tip')

    GgCharmCompilation adaptation = new GgCharmCompiler().adapt(chart)
    assertTrue(adaptation.delegated, adaptation.reasons.join('; '))
    assertEquals('tip', adaptation.charmChart.layers.first().mapping.tooltip.columnName())

    String xml = SvgWriter.toXml(chart.render())
    assertTrue(xml.contains('<title>layer-first</title>'))
    assertTrue(xml.contains('<title>layer-second</title>'))
  }

  @Test
  void testAnnotationColumnRefsAcceptGStringAesthetics() {
    String suffix = '1'
    Matrix data = Matrix.builder()
        .columnNames(["x_${suffix}".toString(), "y_${suffix}".toString(), "label_${suffix}".toString()])
        .rows([[1, 2, 'note']])
        .build()
    Layer annotation = new Layer(
        geom: new GeomText(),
        data: data,
        aes: new Aes(x: "x_${suffix}", y: "y_${suffix}", label: "label_${suffix}"),
        inheritAes: false
    )
    def chart = ggplot(data, aes()) + annotation

    GgCharmCompilation adaptation = new GgCharmCompiler().adapt(chart)

    assertTrue(adaptation.delegated, adaptation.reasons.join('; '))
    TextAnnotationSpec annotationSpec = adaptation.charmChart.getAnnotations().first() as TextAnnotationSpec
    assertEquals('note', annotationSpec.label)
  }
}
