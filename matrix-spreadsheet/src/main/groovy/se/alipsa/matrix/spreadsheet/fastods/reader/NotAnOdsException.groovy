package se.alipsa.matrix.spreadsheet.fastods.reader;

import se.alipsa.matrix.spreadsheet.fastods.FastOdsException
/**
 * The file provided is not an ODS file
 * Based on com.github.miachm.sods.NotAnOdsException
 */
class NotAnOdsException extends FastOdsException {

  NotAnOdsException(String string) {
    super(string)
  }

  NotAnOdsException(Exception e) {
    super(e)
  }

}
