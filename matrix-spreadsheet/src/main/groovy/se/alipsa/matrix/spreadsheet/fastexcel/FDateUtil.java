/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
package se.alipsa.matrix.spreadsheet.fastexcel;

import groovy.transform.CompileStatic;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;

/**
 * This class contains methods for dealing with Excel dates.
 * It is based on the org.apache.poi.ss.usermodel.Dateutil class
 * of Apache POI.
 *
 * @author  Michael Harhen
 * @author  Glen Stampoultzis (glens at apache.org)
 * @author  Dan Sherman (dsherman at isisph.com)
 * @author  Hack Kampbjorn (hak at 2mba.dk)
 * @author  Alex Jacoby (ajacoby at gmail.com)
 * @author  Pavel Krupets (pkrupets at palmtreebusiness dot com)
 * @author  Thies Wellpott
 * @author  Per Nyfelt
 */
@CompileStatic
public class FDateUtil {

  private static final long DAY_MILLISECONDS = 86_400_000L;
  /**
   * The following patterns are used in {@link #isADateFormat(Integer, String)}
   */
  private static final Pattern DATE_PATTERN_1 = Pattern.compile("^\\[\\$\\-.*?\\]");
  private static final Pattern DATE_PATTERN_2 = Pattern.compile("^\\[[a-zA-Z]+\\]");
  private static final Pattern DATE_PATTERN_3 = Pattern.compile("^[\\[\\]yYmMdDhHsS\\-/,. :\"\\\\]+0*[ampAMP/]*$");
  //  elapsed time patterns: [h],[m] and [s]
  private static final Pattern DATE_PATTERN_4 = Pattern.compile("^\\[([hH]+|[mM]+|[sS]+)\\]");

  /**
   * Given a format ID and its format String, will check to see if the
   *  format represents a date format or not.
   * Firstly, it will check to see if the format ID corresponds to an
   *  internal excel date format (eg most US date formats)
   * If not, it will check to see if the format string only contains
   *  date formatting characters (ymd-/), which covers most
   *  non US date formats.
   *
   * @param formatIndex The index of the format, eg from ExtendedFormatRecord.getFormatIndex
   * @param formatString The format string, eg from FormatRecord.getFormatString
   * @see #isInternalDateFormat(int)
   */
  public static boolean isADateFormat(Integer formatIndex, String formatString) {
    if (formatIndex == null || formatString == null) {
      return false;
    }
    // First up, is this an internal date format?
    if(isInternalDateFormat(formatIndex)) {
      return true;
    }

    // If we didn't get a real string, it can't be
    if(formatString.isEmpty()) {
      return false;
    }

    String fs = formatString;

    StringBuilder sb = new StringBuilder(fs.length());
    for (int i = 0; i < fs.length(); i++) {
      char c = fs.charAt(i);
      if (i < fs.length() - 1) {
        char nc = fs.charAt(i + 1);
        if (c == '\\') {
          switch (nc) {
            case '-':
            case ',':
            case '.':
            case ' ':
            case '\\':
              // skip current '\' and continue to the next char
              continue;
          }
        } else if (c == ';' && nc == '@') {
          i++;
          // skip ";@" duplets
          continue;
        }
      }
      sb.append(c);
    }
    fs = sb.toString();

    // short-circuit if it indicates elapsed time: [h], [m] or [s]
    if(DATE_PATTERN_4.matcher(fs).matches()){
      return true;
    }

    // If it starts with [$-...], then could be a date, but
    //  who knows what that starting bit is all about
    fs = DATE_PATTERN_1.matcher(fs).replaceAll("");
    // If it starts with something like [Black] or [Yellow],
    //  then it could be a date
    fs = DATE_PATTERN_2.matcher(fs).replaceAll("");
    // You're allowed something like dd/mm/yy;[red]dd/mm/yy
    //  which would place dates before 1900/1904 in red
    // For now, only consider the first one
    if(fs.indexOf(';') > 0 && fs.indexOf(';') < fs.length()-1) {
      fs = fs.substring(0, fs.indexOf(';'));
    }

    // Otherwise, check it's only made up, in any case, of:
    //  y m d h s - \ / , . :
    // optionally followed by AM/PM
    return DATE_PATTERN_3.matcher(fs).matches();
  }

  /**
   * Given a format ID this will check whether the format represents
   *  an internal excel date format or not.
   * @see #isADateFormat(Integer, String)
   */
  public static boolean isInternalDateFormat(int format) {
    return switch (format) {
      // Internal Date Formats as described on page 427 in
      // Microsoft Excel Dev's Kit...
      case 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x2d, 0x2e, 0x2f -> true;
      default -> false;
    };
  }

  /**
   * This is based on the internal org.dhatim.fastexcel.reader.Cell.convertToDate(double value)
   * method.
   *
   * @param value the date number to convert
   * @param isDate1904 the isDate1904 property of the Workbook
   * @return a LocalDateTime corresponding to the numeric value
   */
  public static LocalDateTime convertToDate(double value, boolean isDate1904) {
    int wholeDays = (int) Math.floor(value);
    long millisecondsInDay = (long) (((value - wholeDays) * DAY_MILLISECONDS) + 0.5D);
    // sometimes the rounding for .9999999 returns the whole number of ms a day
    if(millisecondsInDay == DAY_MILLISECONDS) {
      wholeDays +=1;
      millisecondsInDay= 0;
    }

    int startYear = 1900;
    int dayAdjust = -1; // Excel thinks 2/29/1900 is a valid date, which it isn't
    if (isDate1904) {
      startYear = 1904;
      dayAdjust = 1; // 1904 date windowing uses 1/2/1904 as the first day
    } else if (wholeDays < 61) {
      // Date is prior to 3/1/1900, so adjust because Excel thinks 2/29/1900 exists
      // If Excel date == 2/29/1900, will become 3/1/1900 in Java representation
      dayAdjust = 0;
    }
    LocalDate localDate = LocalDate.of(startYear, 1, 1).plusDays((long) wholeDays + dayAdjust - 1);
    LocalTime localTime = LocalTime.ofNanoOfDay(millisecondsInDay * 1_000_000);
    return LocalDateTime.of(localDate, localTime);
  }

}
