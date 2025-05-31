package se.alipsa.matrix.stats.cluster

import groovy.transform.CompileStatic

@CompileStatic
class ClusteredPoint {
  final int clusterId
  final double[] point

  ClusteredPoint(int clusterId, double[] point) {
    this.clusterId = clusterId
    this.point = point
  }

  int getClusterId() { clusterId }
  double[] getPoint() { point }
}
