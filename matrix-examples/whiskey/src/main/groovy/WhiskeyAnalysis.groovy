@Grab('se.alipsa.matrix:matrix-core:3.8.0')
@Grab('se.alipsa.matrix:matrix-csv:2.4.0')
// TODO: pin to the released matrix-stats once 2.5.3 is published; the -SNAPSHOT coordinate
//       only resolves after a local publishToMavenLocal (needed here for Pca)
@Grab('se.alipsa.matrix:matrix-stats:2.5.3-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-xchart:0.3.2')
@groovy.lang.GrabConfig(systemClassLoader=true)

import se.alipsa.matrix.core.*
import se.alipsa.matrix.csv.*
import se.alipsa.matrix.stats.dimred.Pca
import se.alipsa.matrix.xchart.*

m = CsvImporter.importCsv('https://www.niss.org/sites/default/files/ScotchWhisky01.txt')
    .drop('RowID')
println m.dimensions()

features = m.columnNames() - 'Distillery'
features.each(feature -> m.apply(feature) { it.toDouble() / 4 })

selected= m.subset{ it.Fruity > 0.5 && it.Sweetness > 0.5 }

println selected.dimensions()
println selected.head(10)

transparency = 80
aberlour = selected.subset(0..0)
aberlourRc = RadarChart.create(aberlour, 600, 500)
    .setTitle('aberlour')
    .addSeries('Distillery', transparency)
//io.display(aberlourRc.exportSwing())
aberlourRc.display()
//rc.exportPng(new File( 'aberlour.png'))
distilleriesRc = RadarChart.create(selected, 680, 500)
    .setTitle("Distilleries")
    .addSeries('Distillery', transparency)
distilleriesRc.display()
//rc.exportPng(new File( 'distilleries.png'))
//io.display(distilleriesRc.exportSwing())


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
m['X'] = pca.scores(0)
m['Y'] = pca.scores(1)
println "Variance explained by PC1 + PC2: ${(pca.cumulativeExplainedVariance()[1] * 100).round(1)}%"

clusters = m['Cluster'].toSet()
sc = ScatterChart.create(m, 700, 500)
sc.title = 'Whisky Flavor Clusters'
for (i in clusters) {
  def series = m.subset('Cluster', i)
  sc.addSeries("Cluster $i", series.column('X'), series.column('Y'))
}
sc.display()
//sc.exportPng(new File( 'clusters.png'))
//io.display(sc.exportSwing())

// Create a correlation heatmap
CorrelationHeatmapChart.create(m, 820, 500)
  .addSeries('Heat Series', features)
  .display()

//hc.exportPng(new File('heatmap.png'))
//io.display(hc.exportSwing())
