package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.core.util.Logger

import java.util.Locale
import java.util.Random

/**
 * Sample stat for large datasets.
 *
 * <p>Supported params:</p>
 * <ul>
 *   <li>{@code n}: sample size (default 10000)</li>
 *   <li>{@code seed}: optional random seed</li>
 *   <li>{@code method}: {@code random} (default) or {@code systematic}</li>
 * </ul>
 */
@CompileStatic
class SampleStat {

  private static final Logger log = Logger.getLogger(SampleStat)
  private static final int DEFAULT_SAMPLE_SIZE = 10_000

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    int n = resolveSampleSize(params['n'])
    if (n >= data.size()) {
      return data.collect { LayerData datum -> LayerDataUtil.copyDatum(datum) }
    }

    String method = (params['method']?.toString()?.trim()?.toLowerCase(Locale.ROOT) ?: 'random')
    Long seed = resolveSeed(params['seed'])
    List<Integer> indexes = switch (method) {
      case 'random' -> randomIndexes(data.size(), n, seed)
      case 'systematic' -> systematicIndexes(data.size(), n, seed)
      default -> {
        log.warn("Unsupported sample method '${method}', falling back to random")
        yield randomIndexes(data.size(), n, seed)
      }
    }

    indexes.collect { Integer idx -> LayerDataUtil.copyDatum(data[idx]) }
  }

  private static int resolveSampleSize(Object raw) {
    BigDecimal parsed = ValueConverter.asBigDecimal(raw)
    int n = parsed == null ? DEFAULT_SAMPLE_SIZE : parsed.intValue()
    if (n < 1) {
      log.warn("Sample size n must be >= 1, got ${raw}; using default ${DEFAULT_SAMPLE_SIZE}")
      return DEFAULT_SAMPLE_SIZE
    }
    n
  }

  private static Long resolveSeed(Object raw) {
    BigDecimal parsed = ValueConverter.asBigDecimal(raw)
    parsed == null ? null : parsed.longValue()
  }

  private static List<Integer> randomIndexes(int total, int n, Long seed) {
    Random random = seed == null ? new Random() : new Random(seed)
    List<Integer> sampled = new ArrayList<>(n)
    for (int i = 0; i < n; i++) {
      sampled << i
    }
    for (int i = n; i < total; i++) {
      int j = random.nextInt(i + 1)
      if (j < n) {
        sampled[j] = i
      }
    }
    sampled.sort()
    sampled
  }

  private static List<Integer> systematicIndexes(int total, int n, Long seed) {
    double offset = seed == null ? 0d : new Random(seed).nextDouble()
    List<Integer> sampled = []
    for (int i = 0; i < n; i++) {
      int idx = Math.floor(((i + offset) * total) / n) as int
      sampled << idx
    }
    sampled
  }
}
