/*
The equivalent R code is :
library(ggplot2)

p <- ggplot(mpg, aes(cty, hwy, colour = class)) +
  geom_point() +
  scale_colour_viridis_d()

ggsave("scales.svg", plot = p)
 */
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
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
  scale_colour_viridis_d()

Svg svg = chart.render()
File targetFile = new File(targetDir, this.class.name + '.svg')
write(svg, targetFile)
println("Wrote scatter plot with viridis colors to ${targetFile.absolutePath}")