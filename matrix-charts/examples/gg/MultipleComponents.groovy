/*
geom_mean <- function() {
  list(
    stat_summary(fun = "mean", geom = "bar", fill = "grey70"),
    stat_summary(fun.data = "mean_cl_normal", geom = "errorbar", width = 0.4)
  )
}
ggplot(mpg, aes(class, cty)) + geom_mean()
ggplot(mpg, aes(drv, cty)) + geom_mean()
 */

import groovy.transform.Field
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.2.1-SNAPSHOT')

import groovy.transform.SourceURI
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.gg.*
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.groovy.svg.Svg

@SourceURI
@Field
URI sourceUri

@Field
File targetDir = new File(new File(sourceUri).parentFile.parentFile.parentFile, 'build')

def mpg = Dataset.mpg()
def geom_mean() {
  [
      stat_summary(fun: "mean", geom: "bar", fill: "grey70"),
      stat_summary('fun.data': "mean_cl_normal", geom: "errorbar", width: 0.4)
  ]
}
GgChart chart1 = ggplot(mpg, aes('class', 'cty')) + geom_mean()
GgChart chart2 = ggplot(mpg, aes('drv', 'cty')) + geom_mean()

def renderAndSave(GgChart chart, String name) {
  Svg svg = chart.render()
  File targetFile = new File(targetDir, name + '.svg')
  write(svg, targetFile)
  println("Wrote plot to ${targetFile.absolutePath}")
}
renderAndSave(chart1, this.class.name + '1')
renderAndSave(chart2, this.class.name + '2')