/*
The equivalent R code is :
library(ggplot2)

p <- ggplot(mpg, aes(cty, hwy)) +
  geom_point() +
  facet_grid(year ~ drv)

ggsave("facets.svg", plot = p)
 */
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
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
File targetDir = new File(subProjectDir, 'build')

def mpg = Dataset.mpg()
def chart = ggplot(mpg, aes('cty', 'hwy')) +
    geom_point() +
    facet_grid('year ~ drv')

Svg svg = chart.render()
File targetFile = new File(targetDir, this.class.name + '.svg')
write(svg, targetFile)
println("Wrote facets plot to ${targetFile.absolutePath}")