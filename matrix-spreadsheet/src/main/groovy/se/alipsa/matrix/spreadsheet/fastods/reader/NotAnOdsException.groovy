package se.alipsa.matrix.spreadsheet.fastods.reader;

import groovy.transform.CompileStatic;
import se.alipsa.matrix.spreadsheet.fastods.FastOdsException;
/**
 * The file provided is not an ODS file
 * Based on com.github.miachm.sods.NotAnOdsException
 */
@CompileStatic
class NotAnOdsException extends FastOdsException {

  NotAnOdsException(String string) {
    super(string)
  }

  NotAnOdsException(Exception e) {
    super(e)
  }

}
