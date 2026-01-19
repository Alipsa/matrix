package se.alipsa.matrix.gsheets

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

class GsConverter {

  private static Logger log = LogManager.getLogger(GsConverter)

  private static long secondsInDay = 24 * 60 * 60
  private static LocalDateTime epochDateTime = LocalDateTime.of(1899, 12, 30, 0, 0, 0)
  private static LocalDate epochDate = LocalDate.of(1899, 12, 30)

  private GsConverter() {}

  static LocalDate asLocalDate(Object o, DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE) {
    if (o == null) {
      return null
    }
    if (o instanceof LocalDate) {
      return (LocalDate) o
    } else if (o instanceof Number) {
      return asLocalDate((Number) o)
    } else {
      try {
        return LocalDate.parse(o.toString(), formatter)
      } catch (DateTimeParseException e) {
        // Try parsing as a numeric serial value
        log.warn("Failed to parse '{}' as date with formatter {}, attempting numeric conversion: {}",
                 o, formatter, e.getMessage())
        try {
          return asLocalDate(new BigDecimal(o.toString().replace(' ', '')))
        } catch (NumberFormatException nfe) {
          throw new IllegalArgumentException("Cannot convert '${o}' to LocalDate: not a valid date format or numeric value", nfe)
        }
      }
    }
  }

  static LocalDate asLocalDate(Number val) {
    def daysSinceEpoch = val.intValue()
    return epochDate.plusDays(daysSinceEpoch)
  }

  static List<LocalDate> toLocalDates(List<Object> list) {
    if (list == null) {
      return null
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
    if (o instanceof LocalDateTime) {
      return (LocalDateTime) o
    } else if (o instanceof Number) {
      return asLocalDateTime((Number) o)
    } else {
      try {
        return LocalDateTime.parse(o.toString(), formatter)
      } catch (DateTimeParseException e) {
        // Try parsing as a numeric serial value
        log.warn("Failed to parse '{}' as datetime with formatter {}, attempting numeric conversion: {}",
                 o, formatter, e.getMessage())
        try {
          return asLocalDateTime(new BigDecimal(o.toString().replace(' ', '')))
        } catch (NumberFormatException nfe) {
          throw new IllegalArgumentException("Cannot convert '${o}' to LocalDateTime: not a valid datetime format or numeric value", nfe)
        }
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
    long seconds = Math.round(fractionalPart * 24 * 60 * 60)

    return epochDateTime.plusDays(days).plusSeconds(seconds)
  }

  static List<LocalDateTime> toLocalDateTimes(List<Object> list) {
    if (list == null) {
      return null
    }
    List<LocalDateTime> dateTimes = []
    for (Object o : list) {
      dateTimes.add(asLocalDateTime(o))
    }
    return dateTimes
  }

  static asLocalTime(Object o) {
    if (o == null) {
      return null
    }
    if (o instanceof LocalTime) {
      return (LocalTime) o
    } else if(o instanceof Number) {
      return asLocalTime((Number) o)
    } else {
      try {
        return LocalTime.parse(o.toString())
      } catch (DateTimeParseException e) {
        // Try parsing as a numeric serial value
        log.warn("Failed to parse '{}' as time, attempting numeric conversion: {}", o, e.getMessage())
        try {
          return asLocalTime(new BigDecimal(o.toString()))
        } catch (NumberFormatException nfe) {
          throw new IllegalArgumentException("Cannot convert '${o}' to LocalTime: not a valid time format or numeric value", nfe)
        }
      }
    }
  }

  static LocalTime asLocalTime(Number val) {
    // The serial number is the fraction of a day
    long totalSeconds = (val * secondsInDay).round() as long

    // Create a LocalTime object from the total seconds
    return LocalTime.ofSecondOfDay(totalSeconds)
  }

  static List<LocalTime> toLocalTimes(List<Object> list) {
    if (list == null) {
      return null
    }
    List<LocalTime> times = []
    for (Object o : list) {
      times.add(asLocalTime(o))
    }
    return times
  }

  static BigDecimal asSerial(LocalDate date) {
    if (date == null) {
      throw new IllegalArgumentException("Date cannot be null")
    }

    // Calculate the number of days since the epoch
    long days = ChronoUnit.DAYS.between(epochDate, date)

    return days
  }

  static BigDecimal asSerial(LocalDateTime dateTime) {
    if (dateTime == null) {
      throw new IllegalArgumentException("DateTime cannot be null")
    }

    // Calculate the number of days since the epoch
    def days = ChronoUnit.DAYS.between(epochDateTime, dateTime)

    // Calculate the fraction of the day for the time component
    def secondsSinceMidnight = dateTime.toLocalTime().toSecondOfDay()
    def fraction = secondsSinceMidnight / (24.0 * 60.0 * 60.0)

    return days + fraction
  }

  static BigDecimal asSerial(LocalTime time) {
    long totalSecondsInDay = 24 * 60 * 60
    long secondsSinceMidnight = time.toSecondOfDay()
    return secondsSinceMidnight / totalSecondsInDay
  }

  static asSerial(Date date) {
    if (date == null) return null
    return asSerial(new Timestamp(date.getTime()).toLocalDateTime())
  }

  static BigDecimal asSerial(Object o) {
    if (o instanceof LocalDate) {
      return asSerial((LocalDate) o)
    } else if (o instanceof LocalDateTime) {
      return asSerial((LocalDateTime) o)
    } else if (o instanceof LocalTime) {
      return asSerial((LocalTime) o)
    } else {
      throw new IllegalArgumentException("Cannot convert object of type ${o?.getClass()} to serial number")
    }
  }

  static List<BigDecimal> toSerials(List<Object> list) {
    if (list == null) {
      return null
    }
    List<BigDecimal> serials = []
    for (Object o : list) {
      serials.add(asSerial(o))
    }
    return serials
  }
}
