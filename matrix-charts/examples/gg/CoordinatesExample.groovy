/* This is the equivalent R code:
library(ggplot2)

p <- ggplot(mpg, aes(cty, hwy)) +
  geom_point() +
  coord_fixed()

ggsave("my_plot.svg", plot = p)

 */
@Grab('se.alipsa.matrix:matrix-core:3.7.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.5.0-SNAPSHOT')
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
def chart = ggplot(mpg, aes('cty', 'hwy')) +
    geom_point() +
    coord_fixed()

Svg svg = chart.render()
File targetFile = new File(targetDir, this.class.name + '.svg')
write(svg, targetFile)
println("Wrote coordinates plot to ${targetFile.absolutePath}")