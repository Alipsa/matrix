/*
The equivalent R code is :
library(Require)
Require(c("ggplot2", "svglite", "palmerpenguins"))

# Chart
p <- ggplot(
       data = penguins,
       mapping = aes(x = flipper_length_mm, y = body_mass_g)
     ) +
       geom_point(aes(color = species, shape = species)) +
       geom_smooth(method = "lm") +
       labs(
         title = "Body mass and flipper length",
         subtitle = "Dimensions for Adelie, Chinstrap, and Gentoo Penguins",
         x = "Flipper length (mm)", y = "Body mass (g)",
         color = "Species", shape = "Species"
       )

ggsave("facets.svg", plot = p)
 */
@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.3.0-SNAPSHOT')

import static se.alipsa.matrix.gg.GgPlot.*

import groovy.transform.SourceURI
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.core.*
import se.alipsa.groovy.svg.Svg

@SourceURI
URI sourceUri

File projectDir = new File(sourceUri).parentFile
File subProjectDir = projectDir.parentFile.parentFile
File targetDir = new File(subProjectDir, 'build/examples/gg')
targetDir.mkdirs()

Matrix penguins = Matrix.builder()
    .data(new File(projectDir, 'penguins.csv'))
    .build()
    .replaceAll('NA', null)
    .convert(
        'species': String,
        'island': String,
        'bill_length_mm': Double,
        'bill_depth_mm': Double,
        'flipper_length_mm': Double,
        'body_mass_g': Integer,
        'sex': String,
        'year': Integer
    )

def p = ggplot(
        data: penguins,
        mapping: aes(x: 'flipper_length_mm', y: 'body_mass_g')
    ) +
    geom_point(aes(color: 'species', shape: 'species')) +
    geom_smooth(method: "lm") +
    labs(
        title: "Body mass and flipper length",
        subtitle: "Dimensions for Adelie, Chinstrap, and Gentoo Penguins",
        x: "Flipper length (mm)", y: "Body mass (g)",
        color: "Species", shape: "Species"
    )

Svg svg = p.render()
File targetFile = new File(targetDir, this.class.name + '.svg')
write(svg, targetFile)
println("Wrote labels plot to ${targetFile.absolutePath}")