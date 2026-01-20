/*
The equivalent R code is :
library(ggplot2)

p <- ggplot(mpg, aes(cty, hwy, colour = class)) +
  geom_point() +
  theme_minimal() +
  theme(
    legend.position = "top",
    axis.line = element_line(linewidth = 0.75),
    axis.line.x.bottom = element_line(colour = "blue")
  )

ggsave("themes.svg", plot = p)
 */
@Grab('se.alipsa.matrix:matrix-core:3.6.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-groovy-ext:0.1.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.3.0-SNAPSHOT')

import groovy.transform.SourceURI
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.groovy.svg.Svg

@SourceURI
URI sourceUri

File subProjectDir = new File(sourceUri).parentFile.parentFile.parentFile
File targetDir = new File(subProjectDir, 'build/examples/gg')
targetDir.mkdirs()

def mpg = Dataset.mpg()
def chart = ggplot(mpg, aes('cty', 'hwy', colour: 'class')) +
  geom_point() +
  theme_minimal() +
  theme(
    'legend.position': "top",
    'axis.line': element_line(linewidth: 0.75),
    'axis.line.x.bottom': element_line(colour: "blue")
  )

Svg svg = chart.render()
File targetFile = new File(targetDir, this.class.name + '.svg')
write(svg, targetFile)
println("Wrote themes plot to ${targetFile.absolutePath}")