package se.alipsa.matrix.charts

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

import java.math.RoundingMode

class Histogram extends Chart<Histogram> {

  List<? extends Number> originalData = []
  Map<MinMax, Integer> ranges
  Integer numberOfBins = 9

  static Histogram create(Map params) {
    String title = params.title as String
    Matrix data = params.data as Matrix
    String columnName = params.columnName
    Integer bins = params.getOrDefault('bins', 9) as Integer
    int binDecimals = params.getOrDefault('binDecimals', 1) as Integer
    return create(title, data, columnName, bins, binDecimals)
  }

  static Histogram create(String title, Matrix data, String columnName, Integer bins = 9, int binDecimals = 1) {
    Histogram chart = new Histogram()
    chart.title = title
    chart.numberOfBins = bins
    if (Number.isAssignableFrom(data.type(columnName))) {
      chart.originalData = data.column(columnName) as List<? extends Number>
      chart.ranges  = createRanges(chart.originalData, bins, binDecimals)
    } else {
      throw new IllegalArgumentException("Column must be numeric in a histogram (hint: you can Barplot a Frequency)")
    }
    return chart
  }

  static Histogram create(List<? extends Number> column, Integer bins = 9) {
    Histogram chart = new Histogram()
    chart.originalData = column
    chart.numberOfBins = bins
    chart.ranges  = createRanges(column as List<? extends Number>, bins)
    return chart
  }

  private static Map<MinMax, Integer> createRanges(List<? extends Number> column, int bins, int binDecimals = 1) {
    List<MinMax> ranges = []
    def minValue = Stat.min(column) as BigDecimal
    def maxValue = Stat.max(column) as BigDecimal
    def chunk = (maxValue - minValue) / bins
    //println "minValue = $minValue, maxValue = $maxValue, chunk size = $chunk"
    def chunkMin = minValue
    def chunkMax
    for (int i = 0; i < bins; i++) {
      chunkMax = chunkMin + chunk
      ranges.add(new MinMax(chunkMin, chunkMax, binDecimals))
      chunkMin = chunkMax
    }
    Map<MinMax, Integer> dist = new LinkedHashMap<>()
    for (group in ranges) {
      dist.put(group, 0)
    }
    for (Number value in column) {
      for (group in ranges) {
        if (value <= group.maxValue) {
          Integer num = dist[group]
          dist[group] = num + 1
          break
        }
      }
    }
    return dist
  }

  Map<MinMax, Integer> getRanges() {
    return ranges
  }

  List<? extends Number> getOriginalData() {
    return originalData
  }

  Integer getNumberOfBins() {
    return numberOfBins
  }
}

class MinMax {
  BigDecimal minValue
  BigDecimal maxValue

  MinMax(BigDecimal minValue, BigDecimal maxValue, int binDecimals = 1) {
    this.minValue = minValue.setScale(binDecimals, RoundingMode.HALF_EVEN)
    this.maxValue = maxValue.setScale(binDecimals, RoundingMode.HALF_EVEN)
  }

  @Override
  String toString() {
    return minValue + '-' + maxValue
  }
}
