package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

/**
 * 2D rectangular binned summary statistics.
 * Bins x and y into a rectangular grid and computes summary statistics for z in each bin.
 *
 * This is similar to stat_bin_2d() but computes custom summary statistics (mean, median, etc.)
 * instead of just counting observations.
 *
 * Parameters:
 * <ul>
 *   <li><b>bins:</b> number of bins in each direction (default: 30)</li>
 *   <li><b>binwidth:</b> width of bins in data units (overrides bins if specified)</li>
 *   <li><b>fun:</b> summary function name - 'mean', 'median', 'sum', 'min', 'max' (default: 'mean')</li>
 *   <li><b>fun.data:</b> custom summary closure taking List&lt;Number&gt; returning [y: value] or [value: value]</li>
 *   <li><b>drop:</b> if true, remove bins with no observations (default: true)</li>
 * </ul>
 *
 * Usage:
 * <pre>
 * // Basic usage - compute mean of z values in each 2D bin
 * ggplot(data, aes('x', 'y', fill='z')) +
 *   geom_tile() +
 *   stat_summary_2d()
 *
 * // Custom number of bins
 * stat_summary_2d(bins: 10)
 *
 * // Custom summary function
 * stat_summary_2d(fun: 'median')
 *
 * // Custom binwidth
 * stat_summary_2d(binwidth: 0.5)
 *
 * // Custom summary closure
 * stat_summary_2d('fun.data': { values -> [y: values.sum() / values.size()] })
 * </pre>
 */
@CompileStatic
class StatsSummary2d extends Stats {

  /**
   * Create a stat_summary_2d specification.
   *
   * @param params stat parameters: bins, binwidth, fun, fun.data
   */
  StatsSummary2d(Map params = [:]) {
    super(StatType.SUMMARY_2D, params)
  }
}
