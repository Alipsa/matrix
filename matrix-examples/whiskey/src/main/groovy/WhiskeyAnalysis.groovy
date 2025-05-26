@Grab('com.github.haifengl:smile-core:4.3.0')
@Grab('com.github.haifengl:smile-plot:4.3.0')
@Grab(group='org.bytedeco', module='openblas', version='0.3.28-1.5.11')
@Grab(group='org.bytedeco', module='javacpp', version='1.5.11')
import se.alipsa.matrix.core.*
import se.alipsa.matrix.csv.*
import se.alipsa.matrix.stats.Correlation
import se.alipsa.matrix.xchart.*
import smile.clustering.KMeans
import smile.feature.extraction.PCA

m = CsvImporter.importCsv('https://www.niss.org/sites/default/files/ScotchWhisky01.txt')
    .dropColumns('RowID')
println m.dimensions()

features = m.columnNames() - 'Distillery'
size = features.size()
features.each(feature -> m.apply(feature) { it.toDouble() / 4 })

selected= m.subset{ it.Fruity > 0.5 && it.Sweetness > 0.5 }

println selected.dimensions()
println selected.head(10)

transparency = 80
aberlour = selected.subset(0..0)
aberlourRc = RadarChart.create(aberlour, 600, 500)
    .setTitle('aberlour')
    .addSeries('Distillery', transparency)
io.display(aberlourRc.exportSwing())
//rc.display()
//rc.exportPng(new File( 'aberlour.png'))
distilleriesRc = RadarChart.create(selected, 680, 500)
    .setTitle("Distilleries")
    .addSeries('Distillery', transparency)
//rc.display()
//rc.exportPng(new File( 'distilleries.png'))
io.display(distilleriesRc.exportSwing())

iterations = 20
data = m.selectColumns(features) as double[][]
model = KMeans.fit(data,3, iterations)
m['Cluster'] = model.group().toList()

result = GQ {
  from w in m
  groupby w.Cluster
  orderby w.Cluster
  select w.Cluster, count(w.Cluster) as Count
}
println result

println Matrix.builder('Cluster allocation').ginqResult(result).build().content()

//assert m.rows().countBy{ it.Cluster } == [0:51, 1:23, 2:12]

pca = PCA.fit(data)
projected = pca.getProjection(2).apply(data)
m['X'] = projected*.getAt(0)
m['Y'] = projected*.getAt(1)

clusters = m['Cluster'].toSet()
sc = ScatterChart.create(m, 700, 500)
sc.title = 'Whisky Flavor Clusters'
for (i in clusters) {
  def series = m.subset('Cluster', i)
  sc.addSeries("Cluster $i", series.column('X'), series.column('Y'))
}
//sc.display()
//sc.exportPng(new File( 'clusters.png'))
io.display(sc.exportSwing())

// Create a correlation heatmap
// TODO use a CorrelationHeatmapChart instead of a homegrown HeatmapChart
corr = [size<..0, 0..<size].combinations().collect { int i, int j ->
  Correlation.cor(data[j] as List<? extends Number>, data[i] as List<? extends Number>) * 100 as int
}

corrMatrix = Matrix.builder().data(X: 0..<corr.size(), Heat: corr)
    .types([Number] * 2)
    .matrixName('Heatmap')
    .build()

hc = HeatmapChart.create(corrMatrix, 820, 500)
    .addSeries('Heat Series', features.reverse(), features,
        corrMatrix.column('Heat').collate(size))

//hc.exportPng(new File('heatmap.png'))
io.display(hc.exportSwing())