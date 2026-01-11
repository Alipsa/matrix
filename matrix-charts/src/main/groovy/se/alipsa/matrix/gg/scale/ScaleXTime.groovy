package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Time-of-day scale for the x-axis.
 * Handles LocalTime values representing times within a 24-hour cycle (00:00:00 to 23:59:59).
 *
 * Usage:
 * - scale_x_time() - basic time x-axis
 * - scale_x_time(time_format: 'h:mm a') - 12-hour format with AM/PM
 * - scale_x_time(time_breaks: '1 hour') - breaks every hour
 */
@CompileStatic
class ScaleXTime extends ScaleContinuous {

  /** Position of the x-axis: 'bottom' (default) or 'top' */
  String position = 'bottom'

  /** Time format pattern for labels (Java DateTimeFormatter pattern) */
  String timeFormat = 'HH:mm'

  /** Break interval specification (e.g., '1 hour', '30 minutes', '15 seconds') */
  String timeBreaks = null

  /** Minimum time in the domain (as seconds since midnight) */
  protected long minSecondsSinceMidnight = 0

  /** Maximum time in the domain (as seconds since midnight) */
  protected long maxSecondsSinceMidnight = 0

  /**
   * Time-specific limits storage.
   * The parent Scale class declares limits as List<BigDecimal>, which causes type coercion
   * failures with @CompileStatic when storing LocalTime or String time values.
   * This separate field avoids the type mismatch while maintaining the same functionality.
   */
  private List timeLimits = null

  ScaleXTime() {
    aesthetic = 'x'
    // Disable default expansion for time
    expand = null
  }

  ScaleXTime(Map params) {
    aesthetic = 'x'
    expand = null
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.name) this.name = params.name as String
    if (params.limits) this.timeLimits = params.limits as List
    if (params.containsKey('expand')) {
      List expandList = params.expand as List
      if (expandList != null && expandList.size() != 2) {
        throw new IllegalArgumentException(
          "expand must have exactly 2 elements [multiplicative, additive], got ${expandList.size()}"
        )
      }
      this.expand = expandList
    }
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.position) this.position = params.position as String
    if (params.time_format) this.timeFormat = params.time_format as String
    if (params.timeFormat) this.timeFormat = params.timeFormat as String
    if (params.date_format) this.timeFormat = params.date_format as String  // Alias for compatibility
    if (params.dateFormat) this.timeFormat = params.dateFormat as String    // Alias for compatibility
    if (params.time_breaks) this.timeBreaks = params.time_breaks as String
    if (params.timeBreaks) this.timeBreaks = params.timeBreaks as String
    if (params.date_breaks) this.timeBreaks = params.date_breaks as String  // Alias for compatibility
    if (params.dateBreaks) this.timeBreaks = params.dateBreaks as String    // Alias for compatibility
    if (params.nBreaks) this.nBreaks = params.nBreaks as int
  }

  @Override
  void train(List data) {
    if (data == null || data.isEmpty()) return

    // Convert all time values to seconds since midnight
    List<Long> secondsValues = data.findResults { toSecondsSinceMidnight(it) } as List<Long>

    if (secondsValues.isEmpty()) return

    // Compute min/max
    long min = secondsValues.min() as long
    long max = secondsValues.max() as long

    // Apply explicit limits if set
    if (timeLimits && timeLimits.size() >= 2) {
      if (timeLimits[0] != null) {
        Long limMin = toSecondsSinceMidnight(timeLimits[0])
        if (limMin != null) min = limMin
      }
      if (timeLimits[1] != null) {
        Long limMax = toSecondsSinceMidnight(timeLimits[1])
        if (limMax != null) max = limMax
      }
    }

    // Apply expansion if set
    if (expand != null && expand.size() >= 2) {
      Number mult = expand[0] != null ? expand[0] : 0
      Number add = expand[1] != null ? expand[1] : 0
      long delta = max - min
      min = (long)(min - delta * mult - add)  // add is in seconds
      max = (long)(max + delta * mult + add)
      // Clamp to valid time range [0, 86399]
      min = Math.max(0, min)
      max = Math.min(86399, max)
    }

    minSecondsSinceMidnight = min
    maxSecondsSinceMidnight = max
    computedDomain = [min as BigDecimal, max as BigDecimal]
    trained = true
  }

  @Override
  Object transform(Object value) {
    Long seconds = toSecondsSinceMidnight(value)
    if (seconds == null) return null

    double v = seconds
    double dMin = minSecondsSinceMidnight
    double dMax = maxSecondsSinceMidnight
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (dMax == dMin) return (rMin + rMax) / 2

    // Linear interpolation
    double normalized = (v - dMin) / (dMax - dMin)
    return rMin + normalized * (rMax - rMin)
  }

  @Override
  Object inverse(Object value) {
    Double numeric = value instanceof Number ? value as double : null
    if (numeric == null) return null

    double v = numeric
    double dMin = minSecondsSinceMidnight
    double dMax = maxSecondsSinceMidnight
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (rMax == rMin) return LocalTime.ofSecondOfDay(((dMin + dMax) / 2) as long)

    // Inverse linear interpolation
    double normalized = (v - rMin) / (rMax - rMin)
    double seconds = dMin + normalized * (dMax - dMin)

    long clampedSeconds = Math.max(0, Math.min(86399, seconds as long))
    return LocalTime.ofSecondOfDay(clampedSeconds)
  }

  @Override
  List getComputedBreaks() {
    if (breaks) return breaks

    // Generate time breaks
    LocalTime minTime = LocalTime.ofSecondOfDay(minSecondsSinceMidnight)
    LocalTime maxTime = LocalTime.ofSecondOfDay(maxSecondsSinceMidnight)

    return generateTimeBreaks(minTime, maxTime)
  }

  @Override
  List<String> getComputedLabels() {
    if (labels) return labels

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat)
    return getComputedBreaks().collect { value ->
      if (value instanceof LocalTime) {
        return (value as LocalTime).format(formatter)
      }
      return value?.toString() ?: ''
    }
  }

  /**
   * Generate nice time breaks.
   */
  private List generateTimeBreaks(LocalTime minTime, LocalTime maxTime) {
    if (timeBreaks) {
      return generateTimeBreaksFromSpec(minTime, maxTime, timeBreaks)
    }

    // Auto-determine appropriate break interval
    long seconds = ChronoUnit.SECONDS.between(minTime, maxTime)

    ChronoUnit unit
    int step
    if (seconds <= 60) {
      // <= 1 minute: 10-second breaks
      unit = ChronoUnit.SECONDS
      step = 10
    } else if (seconds <= 300) {
      // <= 5 minutes: 30-second breaks
      unit = ChronoUnit.SECONDS
      step = 30
    } else if (seconds <= 1800) {
      // <= 30 minutes: 5-minute breaks
      unit = ChronoUnit.MINUTES
      step = 5
    } else if (seconds <= 3600) {
      // <= 1 hour: 10-minute breaks
      unit = ChronoUnit.MINUTES
      step = 10
    } else if (seconds <= 7200) {
      // <= 2 hours: 15-minute breaks
      unit = ChronoUnit.MINUTES
      step = 15
    } else if (seconds <= 21600) {
      // <= 6 hours: 30-minute breaks
      unit = ChronoUnit.MINUTES
      step = 30
    } else if (seconds <= 43200) {
      // <= 12 hours: 1-hour breaks
      unit = ChronoUnit.HOURS
      step = 1
    } else {
      // > 12 hours: 2-hour breaks
      unit = ChronoUnit.HOURS
      step = 2
    }

    return generateBreaksByUnit(minTime, maxTime, unit, step)
  }

  /**
   * Generate breaks from a specification like '1 hour', '30 minutes', '15 seconds', etc.
   */
  private List generateTimeBreaksFromSpec(LocalTime minTime, LocalTime maxTime, String spec) {
    String[] parts = spec.trim().split('\\s+')
    int step = parts.length > 1 ? Integer.parseInt(parts[0]) : 1
    String unitStr = parts.length > 1 ? parts[1] : parts[0]

    ChronoUnit unit = parseUnit(unitStr)
    return generateBreaksByUnit(minTime, maxTime, unit, step)
  }

  private ChronoUnit parseUnit(String unitStr) {
    String u = unitStr.toLowerCase().trim()

    // Handle both singular and plural forms explicitly
    switch (u) {
      case 'second':
      case 'seconds':
        return ChronoUnit.SECONDS
      case 'minute':
      case 'minutes':
        return ChronoUnit.MINUTES
      case 'hour':
      case 'hours':
        return ChronoUnit.HOURS
      default:
        throw new IllegalArgumentException(
          "Invalid time unit '${unitStr}'. Supported units: second/seconds, minute/minutes, hour/hours"
        )
    }
  }

  private List<LocalTime> generateBreaksByUnit(LocalTime minTime, LocalTime maxTime, ChronoUnit unit, int step) {
    List<LocalTime> breaks = []

    // Round min to appropriate boundary
    LocalTime current = roundTimeToUnit(minTime, unit, step)
    boolean seenMidnight = false

    while (!current.isAfter(maxTime)) {
      if (!current.isBefore(minTime)) {
        breaks << current
      }

      LocalTime next = advanceTime(current, unit, step)

      // Detect wrapping past midnight (next is before current) or reaching null (out of bounds)
      if (next == null || next.isBefore(current)) {
        break
      }

      // Prevent infinite loop if we see midnight twice
      if (next == LocalTime.MIDNIGHT) {
        if (seenMidnight) {
          break
        }
        seenMidnight = true
      }

      current = next
    }

    return breaks
  }

  private LocalTime roundTimeToUnit(LocalTime time, ChronoUnit unit, int step) {
    switch (unit) {
      case ChronoUnit.SECONDS:
        // Round down to nearest step boundary
        int totalSeconds = time.toSecondOfDay()
        int roundedSeconds = (totalSeconds.intdiv(step)) * step
        return LocalTime.ofSecondOfDay(roundedSeconds)
      case ChronoUnit.MINUTES:
        // Round down to nearest step boundary
        int totalMinutes = time.hour * 60 + time.minute
        int roundedMinutes = (totalMinutes.intdiv(step)) * step
        int hours = roundedMinutes.intdiv(60)
        int minutes = roundedMinutes % 60
        return LocalTime.of(hours, minutes, 0)
      case ChronoUnit.HOURS:
        // Round down to nearest step boundary
        int roundedHours = (time.hour.intdiv(step)) * step
        return LocalTime.of(roundedHours, 0, 0)
      default:
        return time.withSecond(0).withNano(0)
    }
  }

  private LocalTime advanceTime(LocalTime time, ChronoUnit unit, int step) {
    switch (unit) {
      case ChronoUnit.SECONDS:
        long newSeconds = time.toSecondOfDay() + step
        if (newSeconds >= 86400) return null  // Exceeds valid time range
        return LocalTime.ofSecondOfDay(newSeconds)
      case ChronoUnit.MINUTES:
        long totalSeconds = time.toSecondOfDay() + (step * 60L)
        if (totalSeconds >= 86400) return null  // Would wrap past midnight
        return LocalTime.ofSecondOfDay(totalSeconds)
      case ChronoUnit.HOURS:
        long secondsFromHours = time.toSecondOfDay() + (step * 3600L)
        if (secondsFromHours >= 86400) return null  // Would wrap past midnight
        return LocalTime.ofSecondOfDay(secondsFromHours)
      default:
        throw new IllegalArgumentException("Unsupported ChronoUnit: ${unit}")
    }
  }

  /**
   * Convert various time types to seconds since midnight.
   */
  private static Long toSecondsSinceMidnight(Object value) {
    if (value == null) return null

    if (value instanceof LocalTime) {
      return (value as LocalTime).toSecondOfDay() as long
    }

    if (value instanceof Duration) {
      long seconds = (value as Duration).getSeconds()
      // Clamp to valid 24-hour range
      return Math.max(0, Math.min(86399, seconds))
    }

    if (value instanceof Number) {
      // Assume it's already seconds since midnight
      long seconds = (value as Number).longValue()
      // Clamp to valid 24-hour range [0, 86399]
      return Math.max(0, Math.min(86399, seconds))
    }

    if (value instanceof CharSequence) {
      String s = value.toString().trim()
      if (s.isEmpty()) return null
      try {
        // Try parsing as ISO time (HH:mm:ss or HH:mm)
        LocalTime lt = LocalTime.parse(s)
        return lt.toSecondOfDay() as long
      } catch (Exception ignored) {
        return null
      }
    }

    return null
  }
}
