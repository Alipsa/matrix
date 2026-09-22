/*
The equivalent R code is :
library(ggplot2)
p <- ggplot(mpg, aes(cty, hwy)) +
  geom_point() +
  geom_smooth(formula = y ~ x, method = "lm")
ggsave("layers.svg", plot = p)
 */
@Grab('se.alipsa.matrix:matrix-core:3.9.0')
@Grab('se.alipsa.matrix:matrix-charts:0.6.0')
@Grab('se.alipsa.matrix:matrix-ggplot:0.6.0')
@Grab('se.alipsa.matrix:matrix-datasets:2.2.1')
@Grab('se.alipsa.matrix:matrix-stats:2.5.3')

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
def chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
    geom_point() +
    geom_smooth(method: 'lm') +
    labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')

Svg svg = chart.render()
File targetFile = new File(targetDir, this.class.name + '.svg')
write(svg, targetFile)
println("Wrote scatter plot with regression line to ${targetFile.absolutePath}")
