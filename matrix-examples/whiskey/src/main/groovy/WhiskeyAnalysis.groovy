@Grab('se.alipsa.matrix:matrix-core:3.9.0')
@Grab('se.alipsa.matrix:matrix-csv:2.5.0')
@Grab('se.alipsa.matrix:matrix-stats:2.5.3')
@Grab('se.alipsa.matrix:matrix-pict:0.6.0')
@GrabConfig(systemClassLoader=true)

import se.alipsa.matrix.core.*
import se.alipsa.matrix.csv.*
import se.alipsa.matrix.stats.dimred.Pca
import se.alipsa.matrix.pict.*

m = CsvImporter.importCsv('https://www.niss.org/sites/default/files/ScotchWhisky01.txt')
    .drop('RowID')
println m.dimensions()

features = m.columnNames() - 'Distillery'
features.each(feature -> m.apply(feature) { it.toDouble() / 4 })

selected= m.subset{ it.Fruity > 0.5 && it.Sweetness > 0.5 }

println selected.dimensions()
println selected.head(10)

transparency = 0.8
aberlour = selected.subset(0..0)
aberlourRc = RadarChart.builder(aberlour)
    .title('Aberlour')
    .label('Distillery')
    .values(features)
    .fillAlpha(transparency)
    .build()
Plot.png(aberlourRc, new File('aberlour.png'), 600, 500)

distilleriesRc = RadarChart.builder(selected)
    .title('Distilleries')
    .label('Distillery')
    .values(features)
    .fillAlpha(transparency)
    .build()
Plot.png(distilleriesRc, new File('distilleries.png'), 680, 500)


iterations = 20
def km = new se.alipsa.matrix.stats.cluster.KMeans(m)
mCluster = km.fit(features, 3, iterations, 'Cluster', true).withMatrixName('mCluster')
println mCluster.content()
println Stat.countBy(mCluster, 'Cluster')

result = GQ {
  from w in mCluster
  groupby w.Cluster
  orderby w.Cluster
  select w.Cluster, count(w.Cluster) as Count
}
println result

println Matrix.builder('Cluster allocation').ginqResult(result).build().content()

//assert m.rows().countBy{ it.Cluster } == [0:51, 1:23, 2:12]

// PCA projection onto the two first principal components
pca = Pca.fit(m, features)
mCluster['X'] = pca.scores(0)
mCluster['Y'] = pca.scores(1)
println "Variance explained by PC1 + PC2: ${(pca.cumulativeExplainedVariance()[1] * 100).round(1)}%"

mCluster.addColumn('markerSize', Integer, [1] * mCluster.rowCount())
sc = BubbleChart.builder(mCluster)
    .title('Whisky Flavor Clusters')
    .x('X')
    .y('Y')
    .size('markerSize')
    .group('Cluster')
    .build()
Plot.png(sc, new File('clusters.png'), 700, 500)

hc = CorrelationHeatmapChart.builder(m)
    .title('Whisky Feature Correlations')
    .columns(features)
    .build()
Plot.png(hc, new File('heatmap.png'), 820, 500)
