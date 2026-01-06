package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

/**
 * Date scale for the x-axis.
 * Handles Date, LocalDate, LocalDateTime, and other temporal types.
 *
 * Usage:
 * - scale_x_date() - basic date x-axis
 * - scale_x_date(date_labels: '%Y-%m') - custom date format
 * - scale_x_date(date_breaks: '1 month') - breaks every month
 */
@CompileStatic
class ScaleXDate extends ScaleContinuous {

  /** Position of the x-axis: 'bottom' (default) or 'top' */
  String position = 'bottom'

  /** Date format pattern for labels (Java DateTimeFormatter pattern) */
  String dateFormat = 'yyyy-MM-dd'

  /** Break interval specification (e.g., '1 month', '1 week', '1 year') */
  String dateBreaks = null

  /** Minimum date in the domain (as epoch millis) */
  protected long minEpochMillis = 0

  /** Maximum date in the domain (as epoch millis) */
  protected long maxEpochMillis = 0

  ScaleXDate() {
    aesthetic = 'x'
    // Disable default expansion for dates
    expand = null
  }

  ScaleXDate(Map params) {
    aesthetic = 'x'
    expand = null
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.expand) this.expand = params.expand as List<Number>
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.position) this.position = params.position as String
    if (params.date_format) this.dateFormat = params.date_format as String
    if (params.dateFormat) this.dateFormat = params.dateFormat as String
    if (params.date_breaks) this.dateBreaks = params.date_breaks as String
    if (params.dateBreaks) this.dateBreaks = params.dateBreaks as String
    if (params.date_labels) this.dateFormat = params.date_labels as String
    if (params.nBreaks) this.nBreaks = params.nBreaks as int
  }

  @Override
  void train(List data) {
    if (data == null || data.isEmpty()) return

    // Convert all date values to epoch milliseconds
    List<Long> epochValues = data.findResults { toEpochMillis(it) } as List<Long>

    if (epochValues.isEmpty()) return

    // Compute min/max
    long min = epochValues.min() as long
    long max = epochValues.max() as long

    // Apply explicit limits if set
    if (limits && limits.size() >= 2) {
      if (limits[0] != null) {
        Long limMin = toEpochMillis(limits[0])
        if (limMin != null) min = limMin
      }
      if (limits[1] != null) {
        Long limMax = toEpochMillis(limits[1])
        if (limMax != null) max = limMax
      }
    }

    // Apply expansion if set
    if (expand != null && expand.size() >= 2) {
      Number mult = expand[0] != null ? expand[0] : 0
      Number add = expand[1] != null ? expand[1] : 0
      long delta = max - min
      min = (long)(min - delta * mult - add * 86400000)  // add is in days
      max = (long)(max + delta * mult + add * 86400000)
    }

    minEpochMillis = min
    maxEpochMillis = max
    computedDomain = [min as BigDecimal, max as BigDecimal]
    trained = true
  }

  @Override
  Object transform(Object value) {
    Long epochMillis = toEpochMillis(value)
    if (epochMillis == null) return null

    double v = epochMillis
    double dMin = minEpochMillis
    double dMax = maxEpochMillis
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (dMax == dMin) return (rMin + rMax) / 2

    // Linear interpolation
    double normalized = (v - dMin) / (dMax - dMin)
    return rMin + normalized * (rMax - rMin)
  }

  @Override
  Object inverse(Object value) {
    Double numeric = value instanceof Number ? (value as Number).doubleValue() : null
    if (numeric == null) return null

    double v = numeric
    double dMin = minEpochMillis
    double dMax = maxEpochMillis
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (rMax == rMin) return LocalDate.ofEpochDay(((dMin + dMax) / 2 / 86400000) as long)

    // Inverse linear interpolation
    double normalized = (v - rMin) / (rMax - rMin)
    double epochMillis = dMin + normalized * (dMax - dMin)

    return LocalDate.ofEpochDay((epochMillis / 86400000) as long)
  }

  @Override
  List getComputedBreaks() {
    if (breaks) return breaks

    // Generate date breaks
    LocalDate minDate = LocalDate.ofEpochDay((minEpochMillis / 86400000) as long)
    LocalDate maxDate = LocalDate.ofEpochDay((maxEpochMillis / 86400000) as long)

    return generateDateBreaks(minDate, maxDate)
  }

  @Override
  List<String> getComputedLabels() {
    if (labels) return labels

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat)
    return getComputedBreaks().collect { value ->
      if (value instanceof LocalDate) {
        return (value as LocalDate).format(formatter)
      } else if (value instanceof LocalDateTime) {
        return (value as LocalDateTime).format(formatter)
      } else if (value instanceof Date) {
        LocalDate ld = (value as Date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        return ld.format(formatter)
      }
      return value?.toString() ?: ''
    }
  }

  /**
   * Generate nice date breaks.
   */
  private List generateDateBreaks(LocalDate minDate, LocalDate maxDate) {
    if (dateBreaks) {
      return generateDateBreaksFromSpec(minDate, maxDate, dateBreaks)
    }

    // Auto-determine appropriate break interval
    long days = ChronoUnit.DAYS.between(minDate, maxDate)

    ChronoUnit unit
    int step
    if (days <= 14) {
      unit = ChronoUnit.DAYS
      step = 1
    } else if (days <= 60) {
      unit = ChronoUnit.WEEKS
      step = 1
    } else if (days <= 365) {
      unit = ChronoUnit.MONTHS
      step = 1
    } else if (days <= 365 * 5) {
      unit = ChronoUnit.MONTHS
      step = 3
    } else {
      unit = ChronoUnit.YEARS
      step = 1
    }

    return generateBreaksByUnit(minDate, maxDate, unit, step)
  }

  /**
   * Generate breaks from a specification like '1 month', '2 weeks', etc.
   */
  private List generateDateBreaksFromSpec(LocalDate minDate, LocalDate maxDate, String spec) {
    String[] parts = spec.trim().split('\\s+')
    int step = parts.length > 1 ? Integer.parseInt(parts[0]) : 1
    String unitStr = parts.length > 1 ? parts[1] : parts[0]

    ChronoUnit unit = parseUnit(unitStr)
    return generateBreaksByUnit(minDate, maxDate, unit, step)
  }

  private ChronoUnit parseUnit(String unitStr) {
    String u = unitStr.toLowerCase().replaceAll('s$', '')  // Remove plural 's'
    switch (u) {
      case 'day': return ChronoUnit.DAYS
      case 'week': return ChronoUnit.WEEKS
      case 'month': return ChronoUnit.MONTHS
      case 'year': return ChronoUnit.YEARS
      case 'quarter': return ChronoUnit.MONTHS  // Will use step=3
      default: return ChronoUnit.DAYS
    }
  }

  private List<LocalDate> generateBreaksByUnit(LocalDate minDate, LocalDate maxDate, ChronoUnit unit, int step) {
    List<LocalDate> breaks = []

    // Round min to appropriate boundary
    LocalDate current = roundDateToUnit(minDate, unit)

    while (!current.isAfter(maxDate)) {
      if (!current.isBefore(minDate)) {
        breaks << current
      }
      current = advanceDate(current, unit, step)
    }

    return breaks
  }

  private LocalDate roundDateToUnit(LocalDate date, ChronoUnit unit) {
    switch (unit) {
      case ChronoUnit.DAYS:
        return date
      case ChronoUnit.WEEKS:
        return date.minusDays(date.getDayOfWeek().getValue() - 1)
      case ChronoUnit.MONTHS:
        return date.withDayOfMonth(1)
      case ChronoUnit.YEARS:
        return date.withDayOfYear(1)
      default:
        return date
    }
  }

  private LocalDate advanceDate(LocalDate date, ChronoUnit unit, int step) {
    switch (unit) {
      case ChronoUnit.DAYS:
        return date.plusDays(step)
      case ChronoUnit.WEEKS:
        return date.plusWeeks(step)
      case ChronoUnit.MONTHS:
        return date.plusMonths(step)
      case ChronoUnit.YEARS:
        return date.plusYears(step)
      default:
        return date.plusDays(step)
    }
  }

  /**
   * Convert various date types to epoch milliseconds.
   */
  private static Long toEpochMillis(Object value) {
    if (value == null) return null

    if (value instanceof Number) {
      // Assume it's already epoch millis or epoch days
      long v = (value as Number).longValue()
      // If value is small, assume epoch days
      if (v < 100000) {
        return v * 86400000L
      }
      return v
    }

    if (value instanceof Date) {
      return (value as Date).getTime()
    }

    if (value instanceof LocalDate) {
      return (value as LocalDate).toEpochDay() * 86400000L
    }

    if (value instanceof LocalDateTime) {
      return (value as LocalDateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    if (value instanceof Temporal) {
      // Try to convert other temporal types
      try {
        LocalDate ld = LocalDate.from(value as Temporal)
        return ld.toEpochDay() * 86400000L
      } catch (Exception ignored) {
        return null
      }
    }

    if (value instanceof CharSequence) {
      String s = value.toString().trim()
      if (s.isEmpty()) return null
      try {
        // Try parsing as ISO date
        LocalDate ld = LocalDate.parse(s)
        return ld.toEpochDay() * 86400000L
      } catch (Exception ignored) {
        try {
          // Try parsing as ISO datetime
          LocalDateTime ldt = LocalDateTime.parse(s)
          return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (Exception ignored2) {
          return null
        }
      }
    }

    return null
  }
}
