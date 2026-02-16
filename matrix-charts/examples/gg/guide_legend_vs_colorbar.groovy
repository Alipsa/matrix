/*
R equivalent:
library(ggplot2)
df <- data.frame(
  x = 1:5,
  y = c(1, 4, 2, 5, 3),
  value = c(0, 2.5, 5, 7.5, 10)
)

legend_plot <- ggplot(df, aes(x, y, colour = value)) +
  geom_point(size = 3) +
  scale_colour_gradient(
    low = "#132B43",
    high = "#F6C141",
    breaks = c(0, 5, 10),
    labels = c("low", "mid", "high")
  ) +
  guides(colour = guide_legend()) +
  labs(title = "Guide legend")

colorbar_plot <- ggplot(df, aes(x, y, colour = value)) +
  geom_point(size = 3) +
  scale_colour_gradient(
    low = "#132B43",
    high = "#F6C141",
    breaks = c(0, 5, 10),
    labels = c("low", "mid", "high")
  ) +
  guides(colour = guide_colourbar()) +
  labs(title = "Guide colorbar")
*/
@Grab('se.alipsa.matrix:matrix-core:3.7.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.5.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.3.0-SNAPSHOT')

import groovy.transform.SourceURI
import se.alipsa.matrix.core.Matrix
import se.alipsa.groovy.svg.Svg

import static se.alipsa.matrix.gg.GgPlot.*

@SourceURI
URI sourceUri

File projectDir = new File(sourceUri).parentFile
File subProjectDir = projectDir.parentFile.parentFile
File targetDir = new File(subProjectDir, 'build/examples/gg')
targetDir.mkdirs()

def data = Matrix.builder().data(
    x: [1, 2, 3, 4, 5],
    y: [1, 4, 2, 5, 3],
    value: [0, 2.5, 5, 7.5, 10]
).types(Integer, Integer, Double).build()

def legendChart = ggplot(data, aes('x', 'y', color: 'value')) +
    geom_point(size: 3) +
    scale_color_gradient(
        low: '#132B43',
        high: '#F6C141',
        breaks: [0, 5, 10],
        labels: ['low', 'mid', 'high']
    ) +
    guides(color: guide_legend()) +
    labs(title: 'Guide legend')

def colorbarChart = ggplot(data, aes('x', 'y', color: 'value')) +
    geom_point(size: 3) +
    scale_color_gradient(
        low: '#132B43',
        high: '#F6C141',
        breaks: [0, 5, 10],
        labels: ['low', 'mid', 'high']
    ) +
    guides(color: guide_colorbar()) +
    labs(title: 'Guide colorbar')

Svg legendSvg = legendChart.render()
Svg colorbarSvg = colorbarChart.render()

File legendFile = new File(targetDir, 'guide_legend.svg')
File colorbarFile = new File(targetDir, 'guide_colorbar.svg')

write(legendSvg, legendFile)
write(colorbarSvg, colorbarFile)

println("Wrote guide legend example to ${legendFile.absolutePath}")
println("Wrote guide colorbar example to ${colorbarFile.absolutePath}")
