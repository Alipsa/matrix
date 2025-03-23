package se.alipsa.matrix.spreadsheet.fastods.reader;

/**
 * The file provided is not an ODS file
 * Based on com.github.miachm.sods.NotAnOdsException
 */
public class NotAnOdsException extends RuntimeException {

  NotAnOdsException(String string) {
    super(string);
  }

  NotAnOdsException(Exception e) {
    super(e);
  }

}
