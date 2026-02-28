package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.ScaleTransform
import se.alipsa.matrix.core.ValueConverter

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor

/**
 * Shared temporal scale helpers for canonical conversion, break generation, and label formatting.
 *
 * Canonical values are always epoch-milliseconds:
 * - date/datetime: epoch-millis from instant
 * - time: millis since midnight
 */
@CompileStatic
class TemporalScaleUtil {

  static final ZoneId DEFAULT_ZONE = ZoneOffset.UTC

  private static final BigDecimal MILLIS_PER_DAY = 86_400_000
  private static final String DATE_TRANSFORM = 'date'
  private static final String TIME_TRANSFORM = 'time'
  private static final String DATETIME_TRANSFORM = 'datetime'
  private static final String DEFAULT_DATE_FORMAT = 'yyyy-MM-dd'
  private static final String DEFAULT_TIME_FORMAT = 'HH:mm'
  private static final String DEFAULT_DATETIME_FORMAT = 'yyyy-MM-dd HH:mm'
  private static final int MAX_BREAKS = 10_000

  static boolean isTemporalTransform(ScaleTransform strategy) {
    isTemporalTransformId(strategy?.id())
  }

  static boolean isTemporalTransformId(String transformId) {
    String normalized = normalizeTransformId(transformId)
    normalized == DATE_TRANSFORM || normalized == TIME_TRANSFORM || normalized == DATETIME_TRANSFORM
  }

  static String normalizeTransformId(String transformId) {
    transformId?.trim()?.toLowerCase(Locale.ROOT)
  }

  static ZoneId resolveZoneId(Map<String, Object> params) {
    String configured = params?.get('zoneId')?.toString()?.trim()
    if (!configured) {
      return DEFAULT_ZONE
    }
    try {
      return ZoneId.of(configured)
    } catch (Exception ignored) {
      return DEFAULT_ZONE
    }
  }

  static BigDecimal toCanonicalValue(Object value, ScaleTransform strategy, Map<String, Object> params = [:]) {
    String transformId = normalizeTransformId(strategy?.id())
    if (!isTemporalTransformId(transformId)) {
      return ValueConverter.asBigDecimal(value)
    }
    toCanonicalValue(value, transformId, params)
  }

  static BigDecimal toCanonicalValue(Object value, String transformId, Map<String, Object> params = [:]) {
    if (value == null) {
      return null
    }
    if (value instanceof Number) {
      return ValueConverter.asBigDecimal(value)
    }
    if (value instanceof CharSequence && value.toString().trim().isEmpty()) {
      return null
    }

    String normalized = normalizeTransformId(transformId)
    ZoneId zoneId = resolveZoneId(params)
    return switch (normalized) {
      case DATE_TRANSFORM -> toDateMillis(value, zoneId, params)
      case TIME_TRANSFORM -> toTimeMillis(value, zoneId, params)
      case DATETIME_TRANSFORM -> toDatetimeMillis(value, zoneId, params)
      default -> ValueConverter.asBigDecimal(value)
    }
  }

  static String formatTick(BigDecimal value, ScaleTransform strategy, Map<String, Object> params = [:]) {
    formatTick(value, normalizeTransformId(strategy?.id()), params)
  }

  static String formatTick(BigDecimal value, String transformId, Map<String, Object> params = [:]) {
    if (value == null) {
      return ''
    }
    String normalized = normalizeTransformId(transformId)
    ZoneId zoneId = resolveZoneId(params)
    return switch (normalized) {
      case DATE_TRANSFORM -> LocalDateTime.ofInstant(Instant.ofEpochMilli(value.longValue()), zoneId)
          .toLocalDate()
          .format(dateFormatter(params))
      case TIME_TRANSFORM -> fromMillisOfDay(value).format(timeFormatter(params))
      case DATETIME_TRANSFORM -> LocalDateTime.ofInstant(Instant.ofEpochMilli(value.longValue()), zoneId)
          .format(datetimeFormatter(params))
      default -> value.stripTrailingZeros().toPlainString()
    }
  }

  static List<BigDecimal> autoBreaks(
      String transformId,
      BigDecimal min,
      BigDecimal max,
      int count,
      Map<String, Object> params = [:]
  ) {
    if (min == null || max == null || max < min) {
      return []
    }
    String normalized = normalizeTransformId(transformId)
    if (!isTemporalTransformId(normalized)) {
      return []
    }

    int preferred = count < 2 ? 2 : count
    long spanMillis = (max - min).abs().longValue()
    BreakInterval interval
    switch (normalized) {
      case DATE_TRANSFORM -> {
        long spanDays = (spanMillis / MILLIS_PER_DAY.longValue()) as long
        if (spanDays <= 14) {
          interval = BreakInterval.days(1)
        } else if (spanDays <= 60) {
          interval = BreakInterval.weeks(1)
        } else if (spanDays <= 365) {
          interval = BreakInterval.months(1)
        } else if (spanDays <= 365 * 5) {
          interval = BreakInterval.months(3)
        } else {
          interval = BreakInterval.years(1)
        }
      }
      case TIME_TRANSFORM -> {
        if (spanMillis <= 60_000) {
          interval = BreakInterval.seconds(10)
        } else if (spanMillis <= 300_000) {
          interval = BreakInterval.seconds(30)
        } else if (spanMillis <= 1_800_000) {
          interval = BreakInterval.minutes(5)
        } else if (spanMillis <= 3_600_000) {
          interval = BreakInterval.minutes(10)
        } else if (spanMillis <= 7_200_000) {
          interval = BreakInterval.minutes(15)
        } else if (spanMillis <= 21_600_000) {
          interval = BreakInterval.minutes(30)
        } else if (spanMillis <= 43_200_000) {
          interval = BreakInterval.hours(1)
        } else {
          interval = BreakInterval.hours(2)
        }
      }
      case DATETIME_TRANSFORM -> {
        long spanMinutes = spanMillis.intdiv(60_000L) as long
        if (spanMinutes <= 60) {
          interval = BreakInterval.minutes(10)
        } else if (spanMinutes <= 6 * 60) {
          interval = BreakInterval.hours(1)
        } else if (spanMinutes <= 24 * 60) {
          interval = BreakInterval.hours(4)
        } else if (spanMinutes <= 7 * 24 * 60) {
          interval = BreakInterval.days(1)
        } else if (spanMinutes <= 60 * 24 * 60) {
          interval = BreakInterval.weeks(1)
        } else {
          interval = BreakInterval.months(1)
        }
      }
      default -> interval = null
    }

    List<BigDecimal> breaks = breaksForInterval(interval, normalized, min, max, params)
    if (breaks.size() > preferred * 3 && preferred > 0) {
      int stride = (breaks.size() / preferred).ceil() as int
      if (stride > 1) {
        List<BigDecimal> sampled = []
        for (int i = 0; i < breaks.size(); i += stride) {
          sampled << breaks[i]
        }
        if (!sampled.isEmpty() && sampled.last() != breaks.last()) {
          sampled << breaks.last()
        }
        return sampled
      }
    }
    breaks
  }

  static List<BigDecimal> breaksFromSpec(
      String transformId,
      String spec,
      BigDecimal min,
      BigDecimal max,
      Map<String, Object> params = [:]
  ) {
    if (min == null || max == null || max < min) {
      return []
    }
    BreakInterval interval = parseInterval(spec, normalizeTransformId(transformId))
    breaksForInterval(interval, normalizeTransformId(transformId), min, max, params)
  }

  private static List<BigDecimal> breaksForInterval(
      BreakInterval interval,
      String transformId,
      BigDecimal min,
      BigDecimal max,
      Map<String, Object> params
  ) {
    if (interval == null) {
      return []
    }
    ZoneId zoneId = resolveZoneId(params)
    if (interval.calendarBased) {
      if (transformId == DATE_TRANSFORM) {
        LocalDate minDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(min.longValue()), zoneId).toLocalDate()
        LocalDate maxDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(max.longValue()), zoneId).toLocalDate()
        return calendarDateBreaks(minDate, maxDate, interval, zoneId)
      }
      if (transformId == DATETIME_TRANSFORM) {
        ZonedDateTime minDateTime = Instant.ofEpochMilli(min.longValue()).atZone(zoneId)
        ZonedDateTime maxDateTime = Instant.ofEpochMilli(max.longValue()).atZone(zoneId)
        return calendarDatetimeBreaks(minDateTime, maxDateTime, interval)
      }
      return []
    }

    long stepMillis = interval.duration.toMillis()
    if (stepMillis <= 0) {
      return []
    }
    return fixedBreaks(min, max, stepMillis as BigDecimal)
  }

  private static List<BigDecimal> calendarDateBreaks(
      LocalDate minDate,
      LocalDate maxDate,
      BreakInterval interval,
      ZoneId zoneId
  ) {
    List<BigDecimal> result = []
    LocalDate current = alignDate(minDate, interval)
    while (current.isBefore(minDate)) {
      current = advanceDate(current, interval)
    }

    int guard = 0
    while (!current.isAfter(maxDate) && guard < MAX_BREAKS) {
      result << (current.atStartOfDay(zoneId).toInstant().toEpochMilli() as BigDecimal)
      current = advanceDate(current, interval)
      guard++
    }
    result
  }

  private static List<BigDecimal> calendarDatetimeBreaks(
      ZonedDateTime minDateTime,
      ZonedDateTime maxDateTime,
      BreakInterval interval
  ) {
    List<BigDecimal> result = []
    ZonedDateTime current = alignDateTime(minDateTime, interval)
    while (current.isBefore(minDateTime)) {
      current = advanceDateTime(current, interval)
    }

    int guard = 0
    while (!current.isAfter(maxDateTime) && guard < MAX_BREAKS) {
      result << (current.toInstant().toEpochMilli() as BigDecimal)
      current = advanceDateTime(current, interval)
      guard++
    }
    result
  }

  private static List<BigDecimal> fixedBreaks(BigDecimal min, BigDecimal max, BigDecimal stepMillis) {
    if (stepMillis == null || stepMillis <= 0) {
      return []
    }
    List<BigDecimal> result = []
    BigDecimal start = (min / stepMillis).floor() * stepMillis
    while (start < min) {
      start += stepMillis
    }

    int guard = 0
    BigDecimal current = start
    while (current <= max && guard < MAX_BREAKS) {
      result << current
      current += stepMillis
      guard++
    }
    result
  }

  private static LocalDate alignDate(LocalDate value, BreakInterval interval) {
    if (interval.unit == 'day') {
      if (interval.amount <= 1) {
        return value
      }
      long epochDay = value.toEpochDay()
      long aligned = (epochDay.intdiv(interval.amount)) * interval.amount
      return LocalDate.ofEpochDay(aligned)
    }
    if (interval.unit == 'week') {
      LocalDate weekStart = value.minusDays(value.dayOfWeek.value - 1)
      if (interval.amount <= 1) {
        return weekStart
      }
      long weekIndex = weekStart.toEpochDay().intdiv(7)
      long alignedWeek = (weekIndex.intdiv(interval.amount)) * interval.amount
      return LocalDate.ofEpochDay(alignedWeek * 7)
    }
    if (interval.unit == 'month') {
      LocalDate monthStart = value.withDayOfMonth(1)
      if (interval.amount <= 1) {
        return monthStart
      }
      int monthIndex = monthStart.year * 12 + monthStart.monthValue - 1
      int alignedIndex = (monthIndex.intdiv(interval.amount)) * interval.amount
      int year = alignedIndex.intdiv(12)
      int month = (alignedIndex % 12) + 1
      return LocalDate.of(year, month, 1)
    }
    if (interval.unit == 'year') {
      int year = value.year
      if (interval.amount > 1) {
        year = (year.intdiv(interval.amount)) * interval.amount
      }
      return LocalDate.of(year, 1, 1)
    }
    value
  }

  private static LocalDate advanceDate(LocalDate value, BreakInterval interval) {
    switch (interval.unit) {
      case 'day' -> value.plusDays(interval.amount)
      case 'week' -> value.plusWeeks(interval.amount)
      case 'month' -> value.plusMonths(interval.amount)
      case 'year' -> value.plusYears(interval.amount)
      default -> value.plusDays(interval.amount)
    }
  }

  private static ZonedDateTime alignDateTime(ZonedDateTime value, BreakInterval interval) {
    LocalDate alignedDate = alignDate(value.toLocalDate(), interval)
    alignedDate.atStartOfDay(value.zone)
  }

  private static ZonedDateTime advanceDateTime(ZonedDateTime value, BreakInterval interval) {
    switch (interval.unit) {
      case 'day' -> value.plusDays(interval.amount)
      case 'week' -> value.plusWeeks(interval.amount)
      case 'month' -> value.plusMonths(interval.amount)
      case 'year' -> value.plusYears(interval.amount)
      default -> value.plusDays(interval.amount)
    }
  }

  private static BreakInterval parseInterval(String spec, String transformId) {
    if (spec == null || spec.trim().isEmpty()) {
      return null
    }
    String[] parts = spec.trim().toLowerCase(Locale.ROOT).split('\\s+')
    if (parts.length == 0) {
      return null
    }
    int amount = 1
    String unit
    if (parts.length == 1) {
      unit = parts[0]
    } else {
      try {
        amount = Integer.parseInt(parts[0])
      } catch (NumberFormatException ignored) {
        return null
      }
      unit = parts[1]
    }
    if (amount <= 0) {
      return null
    }
    String normalizedUnit = unit.replaceAll('s$', '')
    if (normalizedUnit == 'second') {
      return transformId == DATE_TRANSFORM ? null : BreakInterval.seconds(amount)
    }
    if (normalizedUnit == 'minute') {
      return transformId == DATE_TRANSFORM ? null : BreakInterval.minutes(amount)
    }
    if (normalizedUnit == 'hour') {
      return transformId == DATE_TRANSFORM ? null : BreakInterval.hours(amount)
    }
    if (normalizedUnit == 'day') {
      return BreakInterval.days(amount)
    }
    if (normalizedUnit == 'week') {
      return BreakInterval.weeks(amount)
    }
    if (normalizedUnit == 'month') {
      return BreakInterval.months(amount)
    }
    if (normalizedUnit == 'year') {
      return BreakInterval.years(amount)
    }
    null
  }

  private static BigDecimal toDateMillis(Object value, ZoneId zoneId, Map<String, Object> params) {
    if (value instanceof LocalDate) {
      return ((value as LocalDate).atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof LocalDateTime) {
      return (((value as LocalDateTime).toLocalDate()).atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof ZonedDateTime) {
      ZonedDateTime dateTime = value as ZonedDateTime
      return (dateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof OffsetDateTime) {
      OffsetDateTime dateTime = value as OffsetDateTime
      return (dateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof Instant) {
      Instant instant = value as Instant
      return (instant.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof Date) {
      return ((value as Date).toInstant().atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof CharSequence) {
      String text = value.toString().trim()
      if (text.isEmpty()) {
        return null
      }
      LocalDate parsedDate = parseLocalDate(text, dateFormatter(params))
      if (parsedDate != null) {
        return (parsedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
      }
      LocalDateTime parsedDateTime = parseLocalDateTime(text, datetimeFormatter(params))
      if (parsedDateTime != null) {
        return (parsedDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
      }
      BigDecimal numeric = ValueConverter.asBigDecimal(text)
      if (numeric != null) {
        return numeric
      }
      return null
    }
    if (value instanceof TemporalAccessor) {
      try {
        LocalDate date = LocalDate.from(value as TemporalAccessor)
        return (date.atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
      } catch (Exception ignored) {
        return null
      }
    }
    ValueConverter.asBigDecimal(value)
  }

  private static BigDecimal toTimeMillis(Object value, ZoneId zoneId, Map<String, Object> params) {
    LocalTime time = extractTime(value, zoneId, params)
    if (time == null) {
      return null
    }
    long millis = (time.toSecondOfDay() * 1000L) + time.nano.intdiv(1_000_000)
    millis as BigDecimal
  }

  private static BigDecimal toDatetimeMillis(Object value, ZoneId zoneId, Map<String, Object> params) {
    if (value instanceof LocalDateTime) {
      return ((value as LocalDateTime).atZone(zoneId).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof LocalDate) {
      return ((value as LocalDate).atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof ZonedDateTime) {
      return ((value as ZonedDateTime).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof OffsetDateTime) {
      return ((value as OffsetDateTime).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof Instant) {
      return ((value as Instant).toEpochMilli()) as BigDecimal
    }
    if (value instanceof Date) {
      return ((value as Date).time) as BigDecimal
    }
    if (value instanceof LocalTime) {
      LocalTime time = value as LocalTime
      LocalDateTime dateTime = LocalDate.ofEpochDay(0).atTime(time)
      return (dateTime.atZone(zoneId).toInstant().toEpochMilli()) as BigDecimal
    }
    if (value instanceof CharSequence) {
      String text = value.toString().trim()
      if (text.isEmpty()) {
        return null
      }
      LocalDateTime dateTime = parseLocalDateTime(text, datetimeFormatter(params))
      if (dateTime != null) {
        return (dateTime.atZone(zoneId).toInstant().toEpochMilli()) as BigDecimal
      }
      LocalDate date = parseLocalDate(text, dateFormatter(params))
      if (date != null) {
        return (date.atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
      }
      LocalTime time = parseLocalTime(text, timeFormatter(params))
      if (time != null) {
        LocalDateTime timeOnEpochDay = LocalDate.ofEpochDay(0).atTime(time)
        return (timeOnEpochDay.atZone(zoneId).toInstant().toEpochMilli()) as BigDecimal
      }
      BigDecimal numeric = ValueConverter.asBigDecimal(text)
      if (numeric != null) {
        return numeric
      }
      return null
    }
    if (value instanceof TemporalAccessor) {
      try {
        LocalDateTime dateTime = LocalDateTime.from(value as TemporalAccessor)
        return (dateTime.atZone(zoneId).toInstant().toEpochMilli()) as BigDecimal
      } catch (Exception ignored) {
        try {
          LocalDate date = LocalDate.from(value as TemporalAccessor)
          return (date.atStartOfDay(zoneId).toInstant().toEpochMilli()) as BigDecimal
        } catch (Exception ignored2) {
          return null
        }
      }
    }
    ValueConverter.asBigDecimal(value)
  }

  private static LocalTime extractTime(Object value, ZoneId zoneId, Map<String, Object> params) {
    if (value == null) {
      return null
    }
    if (value instanceof LocalTime) {
      return value as LocalTime
    }
    if (value instanceof LocalDateTime) {
      return (value as LocalDateTime).toLocalTime()
    }
    if (value instanceof ZonedDateTime) {
      return (value as ZonedDateTime).withZoneSameInstant(zoneId).toLocalTime()
    }
    if (value instanceof OffsetDateTime) {
      return (value as OffsetDateTime).atZoneSameInstant(zoneId).toLocalTime()
    }
    if (value instanceof Instant) {
      return LocalDateTime.ofInstant(value as Instant, zoneId).toLocalTime()
    }
    if (value instanceof Date) {
      return (value as Date).toInstant().atZone(zoneId).toLocalTime()
    }
    if (value instanceof CharSequence) {
      String text = value.toString().trim()
      if (text.isEmpty()) {
        return null
      }
      LocalTime parsedTime = parseLocalTime(text, timeFormatter(params))
      if (parsedTime != null) {
        return parsedTime
      }
      LocalDateTime parsedDateTime = parseLocalDateTime(text, datetimeFormatter(params))
      if (parsedDateTime != null) {
        return parsedDateTime.toLocalTime()
      }
      return null
    }
    if (value instanceof TemporalAccessor) {
      try {
        return LocalTime.from(value as TemporalAccessor)
      } catch (Exception ignored) {
        return null
      }
    }
    null
  }

  private static LocalDate parseLocalDate(String text, DateTimeFormatter preferred) {
    if (preferred != null) {
      try {
        return LocalDate.parse(text, preferred)
      } catch (DateTimeParseException ignored) {
      }
    }
    try {
      return LocalDate.parse(text)
    } catch (DateTimeParseException ignored) {
    }
    try {
      return LocalDateTime.parse(text).toLocalDate()
    } catch (DateTimeParseException ignored) {
    }
    try {
      return Instant.parse(text).atZone(DEFAULT_ZONE).toLocalDate()
    } catch (DateTimeParseException ignored) {
    }
    try {
      return OffsetDateTime.parse(text).toLocalDate()
    } catch (DateTimeParseException ignored) {
    }
    try {
      return ZonedDateTime.parse(text).toLocalDate()
    } catch (DateTimeParseException ignored) {
    }
    null
  }

  private static LocalDateTime parseLocalDateTime(String text, DateTimeFormatter preferred) {
    if (preferred != null) {
      try {
        return LocalDateTime.parse(text, preferred)
      } catch (DateTimeParseException ignored) {
      }
    }
    try {
      return LocalDateTime.parse(text)
    } catch (DateTimeParseException ignored) {
    }
    try {
      return OffsetDateTime.parse(text).toLocalDateTime()
    } catch (DateTimeParseException ignored) {
    }
    try {
      return ZonedDateTime.parse(text).toLocalDateTime()
    } catch (DateTimeParseException ignored) {
    }
    try {
      return LocalDate.parse(text).atStartOfDay()
    } catch (DateTimeParseException ignored) {
    }
    null
  }

  private static LocalTime parseLocalTime(String text, DateTimeFormatter preferred) {
    if (preferred != null) {
      try {
        return LocalTime.parse(text, preferred)
      } catch (DateTimeParseException ignored) {
      }
    }
    try {
      return LocalTime.parse(text)
    } catch (DateTimeParseException ignored) {
    }
    try {
      return LocalDateTime.parse(text).toLocalTime()
    } catch (DateTimeParseException ignored) {
    }
    try {
      return OffsetDateTime.parse(text).toLocalTime()
    } catch (DateTimeParseException ignored) {
    }
    try {
      return ZonedDateTime.parse(text).toLocalTime()
    } catch (DateTimeParseException ignored) {
    }
    null
  }

  private static LocalTime fromMillisOfDay(BigDecimal value) {
    long millis = value.longValue()
    long normalized = millis % MILLIS_PER_DAY.longValue()
    if (normalized < 0) {
      normalized += MILLIS_PER_DAY.longValue()
    }
    int seconds = (normalized / 1000L) as int
    int nanos = ((normalized % 1000L) * 1_000_000L) as int
    LocalTime.ofSecondOfDay(seconds).withNano(nanos)
  }

  private static DateTimeFormatter dateFormatter(Map<String, Object> params) {
    formatter((params?.get('dateFormat') ?: DEFAULT_DATE_FORMAT)?.toString(), DEFAULT_DATE_FORMAT)
  }

  private static DateTimeFormatter timeFormatter(Map<String, Object> params) {
    formatter((params?.get('timeFormat') ?: DEFAULT_TIME_FORMAT)?.toString(), DEFAULT_TIME_FORMAT)
  }

  private static DateTimeFormatter datetimeFormatter(Map<String, Object> params) {
    Object explicitDate = params?.get('dateFormat')
    Object explicitTime = params?.get('timeFormat')
    String pattern
    if (explicitDate != null && explicitDate.toString().trim()) {
      pattern = explicitDate.toString()
    } else if (explicitTime != null && explicitTime.toString().trim()) {
      pattern = explicitTime.toString()
    } else {
      pattern = DEFAULT_DATETIME_FORMAT
    }
    formatter(pattern, DEFAULT_DATETIME_FORMAT)
  }

  private static DateTimeFormatter formatter(String pattern, String fallback) {
    String resolved = pattern?.trim()
    if (!resolved) {
      resolved = fallback
    }
    try {
      DateTimeFormatter.ofPattern(resolved)
    } catch (Exception ignored) {
      DateTimeFormatter.ofPattern(fallback)
    }
  }

  @CompileStatic
  private static final class BreakInterval {

    final String unit
    final int amount
    final Duration duration
    final Period period

    private BreakInterval(String unit, int amount, Duration duration, Period period) {
      this.unit = unit
      this.amount = amount
      this.duration = duration
      this.period = period
    }

    boolean isCalendarBased() {
      period != null
    }

    static BreakInterval seconds(int amount) {
      new BreakInterval('second', amount, Duration.ofSeconds(amount), null)
    }

    static BreakInterval minutes(int amount) {
      new BreakInterval('minute', amount, Duration.ofMinutes(amount), null)
    }

    static BreakInterval hours(int amount) {
      new BreakInterval('hour', amount, Duration.ofHours(amount), null)
    }

    static BreakInterval days(int amount) {
      new BreakInterval('day', amount, null, Period.ofDays(amount))
    }

    static BreakInterval weeks(int amount) {
      new BreakInterval('week', amount, null, Period.ofWeeks(amount))
    }

    static BreakInterval months(int amount) {
      new BreakInterval('month', amount, null, Period.ofMonths(amount))
    }

    static BreakInterval years(int amount) {
      new BreakInterval('year', amount, null, Period.ofYears(amount))
    }
  }
}
