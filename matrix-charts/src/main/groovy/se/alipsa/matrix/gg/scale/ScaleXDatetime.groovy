package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

/**
 * DateTime scale for the x-axis.
 * Similar to ScaleXDate but includes time components and uses appropriate formatting.
 *
 * Usage:
 * - scale_x_datetime() - basic datetime x-axis
 * - scale_x_datetime(date_labels: 'yyyy-MM-dd HH:mm') - custom datetime format
 * - scale_x_datetime(date_breaks: '1 hour') - breaks every hour
 */
@CompileStatic
class ScaleXDatetime extends ScaleContinuous {

  /** Position of the x-axis: 'bottom' (default) or 'top' */
  String position = 'bottom'

  /** DateTime format pattern for labels */
  String dateFormat = 'yyyy-MM-dd HH:mm'

  /** Break interval specification (e.g., '1 hour', '30 minutes', '1 day') */
  String dateBreaks = null

  /** Minimum datetime in the domain (as epoch millis) */
  protected long minEpochMillis = 0

  /** Maximum datetime in the domain (as epoch millis) */
  protected long maxEpochMillis = 0

  ScaleXDatetime() {
    aesthetic = 'x'
    expand = null
  }

  ScaleXDatetime(Map params) {
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

    // Convert all datetime values to epoch milliseconds
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
      min = (long)(min - delta * mult - add * 3600000)  // add is in hours
      max = (long)(max + delta * mult + add * 3600000)
    }

    minEpochMillis = min
    maxEpochMillis = max
    computedDomain = [min as Number, max as Number]
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

    if (rMax == rMin) {
      long millis = ((dMin + dMax) / 2) as long
      return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    }

    // Inverse linear interpolation
    double normalized = (v - rMin) / (rMax - rMin)
    long epochMillis = (dMin + normalized * (dMax - dMin)) as long

    return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
  }

  @Override
  List getComputedBreaks() {
    if (breaks) return breaks

    // Generate datetime breaks
    LocalDateTime minDt = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(minEpochMillis), ZoneId.systemDefault())
    LocalDateTime maxDt = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(maxEpochMillis), ZoneId.systemDefault())

    return generateDatetimeBreaks(minDt, maxDt)
  }

  @Override
  List<String> getComputedLabels() {
    if (labels) return labels

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat)
    return getComputedBreaks().collect { value ->
      if (value instanceof LocalDateTime) {
        return (value as LocalDateTime).format(formatter)
      } else if (value instanceof LocalDate) {
        return (value as LocalDate).atStartOfDay().format(formatter)
      } else if (value instanceof Date) {
        LocalDateTime ldt = (value as Date).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        return ldt.format(formatter)
      }
      return value?.toString() ?: ''
    }
  }

  /**
   * Generate nice datetime breaks.
   */
  private List generateDatetimeBreaks(LocalDateTime minDt, LocalDateTime maxDt) {
    if (dateBreaks) {
      return generateDatetimeBreaksFromSpec(minDt, maxDt, dateBreaks)
    }

    // Auto-determine appropriate break interval
    long minutes = ChronoUnit.MINUTES.between(minDt, maxDt)

    ChronoUnit unit
    int step
    if (minutes <= 60) {
      unit = ChronoUnit.MINUTES
      step = 10
    } else if (minutes <= 60 * 6) {
      unit = ChronoUnit.HOURS
      step = 1
    } else if (minutes <= 60 * 24) {
      unit = ChronoUnit.HOURS
      step = 4
    } else if (minutes <= 60 * 24 * 7) {
      unit = ChronoUnit.DAYS
      step = 1
    } else if (minutes <= 60 * 24 * 60) {
      unit = ChronoUnit.WEEKS
      step = 1
    } else {
      unit = ChronoUnit.MONTHS
      step = 1
    }

    return generateBreaksByUnit(minDt, maxDt, unit, step)
  }

  /**
   * Generate breaks from a specification like '1 hour', '30 minutes', etc.
   */
  private List generateDatetimeBreaksFromSpec(LocalDateTime minDt, LocalDateTime maxDt, String spec) {
    String[] parts = spec.trim().split('\\s+')
    int step = parts.length > 1 ? Integer.parseInt(parts[0]) : 1
    String unitStr = parts.length > 1 ? parts[1] : parts[0]

    ChronoUnit unit = parseUnit(unitStr)
    return generateBreaksByUnit(minDt, maxDt, unit, step)
  }

  private ChronoUnit parseUnit(String unitStr) {
    String u = unitStr.toLowerCase().replaceAll('s$', '')  // Remove plural 's'
    switch (u) {
      case 'second': return ChronoUnit.SECONDS
      case 'minute': return ChronoUnit.MINUTES
      case 'hour': return ChronoUnit.HOURS
      case 'day': return ChronoUnit.DAYS
      case 'week': return ChronoUnit.WEEKS
      case 'month': return ChronoUnit.MONTHS
      case 'year': return ChronoUnit.YEARS
      default: return ChronoUnit.HOURS
    }
  }

  private List<LocalDateTime> generateBreaksByUnit(LocalDateTime minDt, LocalDateTime maxDt, ChronoUnit unit, int step) {
    List<LocalDateTime> breaks = []

    // Round min to appropriate boundary
    LocalDateTime current = roundDatetimeToUnit(minDt, unit)

    while (!current.isAfter(maxDt)) {
      if (!current.isBefore(minDt)) {
        breaks << current
      }
      current = advanceDatetime(current, unit, step)
    }

    return breaks
  }

  private LocalDateTime roundDatetimeToUnit(LocalDateTime dt, ChronoUnit unit) {
    switch (unit) {
      case ChronoUnit.SECONDS:
        return dt.withNano(0)
      case ChronoUnit.MINUTES:
        return dt.withSecond(0).withNano(0)
      case ChronoUnit.HOURS:
        return dt.withMinute(0).withSecond(0).withNano(0)
      case ChronoUnit.DAYS:
        return dt.toLocalDate().atStartOfDay()
      case ChronoUnit.WEEKS:
        return dt.minusDays(dt.getDayOfWeek().getValue() - 1).toLocalDate().atStartOfDay()
      case ChronoUnit.MONTHS:
        return dt.withDayOfMonth(1).toLocalDate().atStartOfDay()
      case ChronoUnit.YEARS:
        return dt.withDayOfYear(1).toLocalDate().atStartOfDay()
      default:
        return dt
    }
  }

  private LocalDateTime advanceDatetime(LocalDateTime dt, ChronoUnit unit, int step) {
    switch (unit) {
      case ChronoUnit.SECONDS:
        return dt.plusSeconds(step)
      case ChronoUnit.MINUTES:
        return dt.plusMinutes(step)
      case ChronoUnit.HOURS:
        return dt.plusHours(step)
      case ChronoUnit.DAYS:
        return dt.plusDays(step)
      case ChronoUnit.WEEKS:
        return dt.plusWeeks(step)
      case ChronoUnit.MONTHS:
        return dt.plusMonths(step)
      case ChronoUnit.YEARS:
        return dt.plusYears(step)
      default:
        return dt.plusHours(step)
    }
  }

  /**
   * Convert various datetime types to epoch milliseconds.
   */
  private static Long toEpochMillis(Object value) {
    if (value == null) return null

    if (value instanceof Number) {
      return (value as Number).longValue()
    }

    if (value instanceof Date) {
      return (value as Date).getTime()
    }

    if (value instanceof LocalDateTime) {
      return (value as LocalDateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    if (value instanceof LocalDate) {
      return (value as LocalDate).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    if (value instanceof Temporal) {
      try {
        LocalDateTime ldt = LocalDateTime.from(value as Temporal)
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
      } catch (Exception ignored) {
        try {
          LocalDate ld = LocalDate.from(value as Temporal)
          return ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (Exception ignored2) {
          return null
        }
      }
    }

    if (value instanceof CharSequence) {
      String s = value.toString().trim()
      if (s.isEmpty()) return null
      try {
        LocalDateTime ldt = LocalDateTime.parse(s)
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
      } catch (Exception ignored) {
        try {
          LocalDate ld = LocalDate.parse(s)
          return ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (Exception ignored2) {
          return null
        }
      }
    }

    return null
  }
}
