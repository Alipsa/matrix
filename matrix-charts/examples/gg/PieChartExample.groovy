/*
piechart <- function(data, mapping) {
  ggplot(data, mapping) +
    geom_bar(width = 1) +
    coord_polar(theta = "y") +
    xlab(NULL) +
    ylab(NULL)
}
piechart(mpg, aes(factor(1), fill = class))
 */
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.2.1-SNAPSHOT')

import groovy.transform.SourceURI
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.groovy.svg.Svg

@SourceURI
URI sourceUri

File targetDir = new File(new File(sourceUri).parentFile.parentFile.parentFile, 'build')

def piechart(data, mapping) {
  ggplot(data, mapping) +
      geom_bar(width: 1) +
      coord_polar(theta: "y") +
      xlab(NULL) +
      ylab(NULL)
}
def chart = piechart(Dataset.mpg(), aes(factor(1), fill: 'class'))

Svg svg = chart.render()
File targetFile = new File(targetDir, this.class.name + '.svg')
write(svg, targetFile)
println("Wrote pie plot to ${targetFile.absolutePath}")
