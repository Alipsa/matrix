package se.alipsa.matrix.gsheets

import se.alipsa.matrix.core.util.Logger

import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Converts between Google Sheets serial numbers and Java date/time types.
 */
class GsConverter {

  private static final Logger log = Logger.getLogger(GsConverter)

  @SuppressWarnings('DuplicateNumberLiteral')
  private static final long SECONDS_PER_DAY = 24 * 60 * 60
  private static final int EPOCH_YEAR = 1899
  private static final int EPOCH_MONTH = 12
  private static final int EPOCH_DAY = 30
  private static final String SPACE = ' '
  private static final LocalDateTime EPOCH_DATE_TIME = LocalDateTime.of(EPOCH_YEAR, EPOCH_MONTH, EPOCH_DAY, 0, 0, 0)
  private static final LocalDate EPOCH_DATE = LocalDate.of(EPOCH_YEAR, EPOCH_MONTH, EPOCH_DAY)

  private GsConverter() { }

  static LocalDate asLocalDate(Object o, DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE) {
    if (o == null) {
      return null
    }
    if (o in LocalDate) {
      return (LocalDate) o
    }
    if (o in Number) {
      return asLocalDate((Number) o)
    }
    try {
      return LocalDate.parse(o.toString(), formatter)
    } catch (DateTimeParseException e) {
      // Try parsing as a numeric serial value
      log.debug("Failed to parse '$o' as date with formatter $formatter, attempting numeric conversion: ${e.message}")
      try {
        return asLocalDate(new BigDecimal(o.toString().replace(SPACE, '')))
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException("Cannot convert '${o}' to LocalDate: not a valid date format or numeric value", nfe)
      }
    }
  }

  static LocalDate asLocalDate(Number val) {
    def daysSinceEpoch = val.intValue()
    return EPOCH_DATE.plusDays(daysSinceEpoch)
  }

  static List<LocalDate> toLocalDates(List<Object> list) {
    if (list == null) {
      return []
    }
    List<LocalDate> dates = []
    for (Object o : list) {
      dates.add(asLocalDate(o))
    }
    return dates
  }

  static LocalDateTime asLocalDateTime(Object o, DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
    if (o == null) {
      return null
    }
    if (o in LocalDateTime) {
      return (LocalDateTime) o
    }
    if (o in Number) {
      return asLocalDateTime((Number) o)
    }
    try {
      return LocalDateTime.parse(o.toString(), formatter)
    } catch (DateTimeParseException e) {
      // Try parsing as a numeric serial value
      log.debug("Failed to parse '$o' as datetime with formatter $formatter, attempting numeric conversion: ${e.message}")
      try {
        return asLocalDateTime(new BigDecimal(o.toString().replace(SPACE, '')))
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException("Cannot convert '${o}' to LocalDateTime: not a valid datetime format or numeric value", nfe)
      }
    }
  }

  // Google Sheets' serial number system has a bug where 1900 is
  // incorrectly counted as a leap year, so we must subtract one day
  // for dates after February 28, 1900. The simplest fix is to adjust the epoch.
  // For dates after 1900-02-28, we need to add a day to the epoch to account
  // for the incorrect leap day. A simpler way is to just add 2 days to the epoch
  // and handle the day part.
  static LocalDateTime asLocalDateTime(Number val) {
    // The integer part of the serial number is the number of days
    long days = val.longValue()

    // The fractional part is the time of day
    double fractionalPart = val.doubleValue() - days
    long seconds = Math.round(fractionalPart * SECONDS_PER_DAY)

    return EPOCH_DATE_TIME.plusDays(days).plusSeconds(seconds)
  }

  static List<LocalDateTime> toLocalDateTimes(List<Object> list) {
    if (list == null) {
      return []
    }
    List<LocalDateTime> dateTimes = []
    for (Object o : list) {
      dateTimes.add(asLocalDateTime(o))
    }
    return dateTimes
  }

  static LocalTime asLocalTime(Object o) {
    if (o == null) {
      return null
    }
    if (o in LocalTime) {
      return (LocalTime) o
    }
    if (o in Number) {
      return asLocalTime((Number) o)
    }
    try {
      return LocalTime.parse(o.toString())
    } catch (DateTimeParseException e) {
      // Try parsing as a numeric serial value
      log.debug("Failed to parse '$o' as time, attempting numeric conversion: ${e.message}")
      try {
        return asLocalTime(new BigDecimal(o.toString()))
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException("Cannot convert '${o}' to LocalTime: not a valid time format or numeric value", nfe)
      }
    }
  }

  static LocalTime asLocalTime(Number val) {
    // The serial number is the fraction of a day
    long totalSeconds = ((val as BigDecimal) * SECONDS_PER_DAY).round() as long

    // Create a LocalTime object from the total seconds
    return LocalTime.ofSecondOfDay(totalSeconds)
  }

  static List<LocalTime> toLocalTimes(List<Object> list) {
    if (list == null) {
      return []
    }
    List<LocalTime> times = []
    for (Object o : list) {
      times.add(asLocalTime(o))
    }
    return times
  }

  static BigDecimal asSerial(LocalDate date) {
    if (date == null) {
      throw new IllegalArgumentException('Date cannot be null')
    }

    // Calculate the number of days since the epoch
    long days = ChronoUnit.DAYS.between(EPOCH_DATE, date)

    return days
  }

  static BigDecimal asSerial(LocalDateTime dateTime) {
    if (dateTime == null) {
      throw new IllegalArgumentException('DateTime cannot be null')
    }

    // Calculate the number of days since the epoch
    def days = ChronoUnit.DAYS.between(EPOCH_DATE_TIME, dateTime)

    // Calculate the fraction of the day for the time component
    def secondsSinceMidnight = dateTime.toLocalTime().toSecondOfDay()
    def fraction = secondsSinceMidnight / (SECONDS_PER_DAY as BigDecimal)

    return days + fraction
  }

  static BigDecimal asSerial(LocalTime time) {
    long totalSecondsInDay = SECONDS_PER_DAY
    long secondsSinceMidnight = time.toSecondOfDay()
    return secondsSinceMidnight / totalSecondsInDay
  }

  static BigDecimal asSerial(Date date) {
    if (date == null) {
      return null
    }
    return asSerial(new Timestamp(date.getTime()).toLocalDateTime())
  }

  static BigDecimal asSerial(Object o) {
    if (o in LocalDate) {
      return asSerial((LocalDate) o)
    }
    if (o in LocalDateTime) {
      return asSerial((LocalDateTime) o)
    }
    if (o in LocalTime) {
      return asSerial((LocalTime) o)
    }
    throw new IllegalArgumentException("Cannot convert object of type ${o?.getClass()} to serial number")
  }

  static List<BigDecimal> toSerials(List<Object> list) {
    if (list == null) {
      return []
    }
    List<BigDecimal> serials = []
    for (Object o : list) {
      serials.add(asSerial(o))
    }
    return serials
  }

}
