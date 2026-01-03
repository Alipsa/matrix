/*
The equivalent R code is :
library(ggplot2)
bestfit <- geom_smooth(
    method = "lm",
    se = FALSE,
    colour = alpha("steelblue", 0.5),
    linewidth = 2
)
ggplot(mpg, aes(cty, hwy)) +
    geom_point() +
    bestfit
ggplot(mpg, aes(displ, hwy)) +
    geom_point() +
    bestfit

geom_lm <- function(formula = y ~ x, colour = alpha("steelblue", 0.5),
                    linewidth = 2, ...)  {
  geom_smooth(formula = formula, se = FALSE, method = "lm", colour = colour,
    linewidth = linewidth, ...)
}
ggplot(mpg, aes(displ, 1 / hwy)) +
  geom_point() +
  geom_lm()
ggplot(mpg, aes(displ, 1 / hwy)) +
  geom_point() +
  geom_lm(y ~ poly(x, 2), linewidth = 1, colour = "red")


 */
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.3.0-SNAPSHOT')

import groovy.transform.SourceURI
import se.alipsa.matrix.gg.*
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.groovy.svg.Svg

@SourceURI
URI sourceUri

File subProjectDir = new File(sourceUri).parentFile.parentFile.parentFile
File targetDir = new File(subProjectDir, 'build/examples/gg')
targetDir.mkdirs()

def mpg = Dataset.mpg()

// Using geom_lm (built-in convenience wrapper for linear model)
def bestfit = geom_lm()

GgChart chart1 = ggplot(mpg, aes('cty', 'hwy')) +
    geom_point() +
    bestfit +
    labs(title: 'City vs Highway MPG')

GgChart chart2 = ggplot(mpg, aes('displ', 'hwy')) +
    geom_point() +
    bestfit +
    labs(title: 'Displacement vs Highway MPG')

// Using closure expressions in aes for data transformations
// The expr() function wraps a closure that receives a Row object

GgChart chart3 = ggplot(mpg, aes(x: 'displ', y: expr { 1.0 / it.hwy })) +
    geom_point() +
    geom_lm() +
    labs(title: 'Simple Linear Model', y: '1 / Highway MPG')

// Polynomial model with expression in y
GgChart chart4 = ggplot(mpg, aes(x: 'displ', y: expr { 1.0 / it.hwy })) +
    geom_point() +
    geom_lm(formula: 'y ~ poly(x, 2)', linewidth: 1, colour: "red") +
    labs(title: 'Polynomial Model (degree 2)', y: '1 / Highway MPG')

// Render and save charts
targetDir.mkdirs()

[
    'reuse_cty_hwy': chart1,
    'reuse_displ_hwy': chart2,
    'reuse_expr_linear': chart3,
    'reuse_expr_poly': chart4
].each { name, chart  ->
  Svg svg = chart.render()
  write(svg, new File(targetDir, "${name}.svg"))
  println("Wrote ${name}.svg")
}
