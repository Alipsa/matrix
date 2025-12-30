@Grab('se.alipsa.matrix:matrix-core:3.5.1-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-stats:2.2.1-SNAPSHOT')

import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.groovy.svg.Svg

def mpg = Dataset.mpg()
def chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
    geom_point() +
    geom_smooth(method: 'lm') +
    labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')

Svg svg = chart.render()
write(svg, new File(this.class.name + '.svg'))