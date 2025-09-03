package se.alipsa.matrix.gsheets

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.auth.http.HttpCredentialsAdapter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class GsUtil {

  private static Logger log = LogManager.getLogger(GsUtil)
  private static long secondsInDay = 24 * 60 * 60
  private static LocalDateTime epochDateTime = LocalDateTime.of(1899, 12, 30, 0, 0, 0)
  private static LocalDate epochDate = LocalDate.of(1899, 12, 30)

  static boolean deleteSheet(String spreadsheetId) {
    def scopes = ["https://www.googleapis.com/auth/drive"] + BqAuthenticator.SCOPES
    def credentials = BqAuthenticator.authenticate(scopes)
    HttpRequestInitializer cred = new HttpCredentialsAdapter(credentials)
    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    def driveService = new Drive.Builder(transport, gsonFactory, cred)
        .setApplicationName("Matrix GSheets")
        .build()

    try {
      // Perform the delete operation on the Drive file using its ID
      driveService.files().delete(spreadsheetId).execute()
      log.info "Successfully deleted spreadsheet with ID: ${spreadsheetId}"
      return true
    } catch (IOException e) {
      log.error("An error occurred while deleting the file: {}", e.getMessage())
    }
    return false
  }

  static LocalDate asLocalDate(Object o) {
    if (o instanceof LocalDate) {
      return (LocalDate) o
    } else if (o instanceof Number) {
      return asLocalDate((Number) o)
    } else {
      try {
        return LocalDate.parse(o.toString())
      } catch (Exception ignore) {
        return null
      }
    }
  }

  static LocalDate asLocalDate(Number val) {
    def daysSinceEpoch = val.intValue()
    return epochDate.plusDays(daysSinceEpoch)
  }

  static LocalDateTime asLocalDateTime(Object o) {
    if (o instanceof LocalDateTime) {
      return (LocalDateTime) o
    } else if (o instanceof Number) {
      return asLocalDateTime((Number) o)
    } else {
      try {
        return LocalDateTime.parse(o.toString())
      } catch (Exception ignore) {
        return null
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

  static asLocalTime(Object o) {
    if (o instanceof LocalTime) {
      return (LocalTime) o
    } else if(o instanceof Number) {
      return asLocalTime((Number) o)
    } else {
      try {
        return LocalTime.parse(o.toString())
      } catch (Exception ignore) {
        return null
      }
    }
  }

  static LocalTime asLocalTime(Number val) {
    // The serial number is the fraction of a day
    long totalSeconds = (val * secondsInDay).round() as long

    // Create a LocalTime object from the total seconds
    return LocalTime.ofSecondOfDay(totalSeconds)
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
}
