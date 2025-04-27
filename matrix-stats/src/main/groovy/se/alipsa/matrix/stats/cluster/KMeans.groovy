package se.alipsa.matrix.stats.cluster

import groovy.transform.CompileStatic

/**
 * KMeans clustering algorithm.
 *
 * The K-means algorithm is a method of vector quantization, originally from signal processing,
 * that is popular for cluster analysis in data mining.
 *
 * The algorithm aims to partition n observations into k clusters in which each observation belongs to the cluster with the nearest mean,
 * serving as a prototype of the cluster.
 * Example usage:
 * <pre><code>
 *   import se.alipsa.matrix.stats.cluster.KMeans
 *   import se.alipsa.matrix.core.Matrix
 *
 *   Matrix m = Matrix.builder('Whiskey data').data('https://www.niss.org/sites/default/files/ScotchWhisky01.txt').build())
 *   KMeans kmeans = new KMeans(m)
 kmeans.fit(features, 3, 20)
 */
@CompileStatic
class KMeans {

}
