package se.alipsa.matrix.spreadsheet.fastods.reader;

import groovy.transform.CompileStatic;

/**
 * The file provided is not an ODS file
 * Based on com.github.miachm.sods.NotAnOdsException
 */
@CompileStatic
public class NotAnOdsException extends RuntimeException {

  NotAnOdsException(String string) {
    super(string);
  }

  NotAnOdsException(Exception e) {
    super(e);
  }

}
