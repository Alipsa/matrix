package se.alipsa.matrix.stats.cluster

/**
 * Definition of a mean that contains a centroid and points on its cluster.
 */
class ClusterMean {
  double[] mCentroid
  final List<double[]> mClosestItems = []

  ClusterMean(int dimension) {
    mCentroid = new float[dimension]
  }

  ClusterMean(double... centroid) {
    mCentroid = centroid
  }

  double[] getCentroid() {
    return mCentroid
  }
  List<double[]> getItems() {
    return mClosestItems
  }

  @Override
  String toString() {
    return "Mean(centroid: $mCentroid, size: ${mClosestItems.size()})"
  }
}
