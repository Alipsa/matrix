/*
library(Require)
Require(c("ggplot2", "svglite", "Hmisc"))

geom_mean <- function() {
  list(
    stat_summary(fun = "mean", geom = "bar", fill = "grey70"),
    stat_summary(fun.data = "mean_cl_normal", geom = "errorbar", width = 0.4)
  )
}
geom_median <- function() {
  list(
    stat_summary(fun = "median", geom = "bar"),
    stat_summary(fun.data = "median_hilow", geom = "errorbar", width = 0.4)
  )
}
chart1 <- ggplot(mpg, aes(class, cty)) + geom_mean()
chart2 <- ggplot(mpg, aes(x = drv, y = cty, fill = drv)) +
  geom_mean() +
  ggtitle('Median cty by drv') +
  theme(plotTitle = element_text(hjust = 0.5))

ggsave("MultipleComponents1.svg", plot = chart1)
ggsave("MultipleComponents2.svg", plot = chart2)
 */

import groovy.transform.Field
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-groovy-ext:0.1.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.3.0-SNAPSHOT')

import groovy.transform.SourceURI
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.gg.*
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.groovy.svg.Svg

@SourceURI
@Field
URI sourceUri

@Field
File targetDir = new File(new File(sourceUri).parentFile.parentFile.parentFile, 'build/examples/gg')
targetDir.mkdirs()

def mpg = Dataset.mpg()
def geom_mean() {
  [
      stat_summary(fun: "mean", geom: "bar", fill: "grey70"),
      stat_summary('fun.data': "mean_cl_normal", geom: "errorbar", width: 0.4)
  ]
}
def geom_median() {
  [
      stat_summary(fun: "median", geom: "bar"),
      stat_summary('fun.data': "median_hilow", geom: "errorbar", width: 0.4)
  ]
}
GgChart chart1 = ggplot(mpg, aes('class', 'cty')) + geom_mean()
GgChart chart2 = ggplot(mpg, aes(x:'drv', y:'cty', fill: 'drv')) +
    geom_median() +
    ggtitle('Median cty by drv') +
    theme('plot.title': element_text(hjust: 0.5))

def renderAndSave(GgChart chart, String name) {
  Svg svg = chart.render()
  File targetFile = new File(targetDir, name + '.svg')
  write(svg, targetFile)
  println("Wrote plot to ${targetFile.absolutePath}")
}
renderAndSave(chart1, this.class.name + '1')
renderAndSave(chart2, this.class.name + '2')