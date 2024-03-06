package se.alipsa.groovy.spreadsheet

import java.text.NumberFormat
import java.text.ParseException

/**
 * A ValueExtractor is a helper class that makes it easier to get values from a spreadsheet.
 */
abstract class ValueExtractor {

   protected NumberFormat percentFormat = NumberFormat.getPercentInstance()

   Double getDouble(Object val) {
      if (val == null) {
         return null
      }
      if (val instanceof Double) {
         return (Double) val
      }
      String strVal = val.toString()
      try {
         return Double.parseDouble(strVal)
      } catch (NumberFormatException e) {
         try {
            percentFormat.parse(strVal).doubleValue()
         } catch (ParseException ignored) {
            // do nothing
         }
         return null
      }
   }

   Double getPercentage(Object val) {
      if (val == null) {
         return null
      }
      if (val instanceof Double) {
         return (Double) val
      }
      String strVal = val.toString()
      if (strVal.contains("%")) {
         try {
            return percentFormat.parse(strVal).doubleValue()
         } catch (ParseException e) {
            return null
         }
      } else {
         return Double.parseDouble(strVal)
      }
   }

   static Integer getInt(Object objVal) {
      if (objVal == null) {
         return null
      }
      if (objVal instanceof Double) {
         return (int)(Math.round((Double) objVal))
      }
      if (objVal instanceof Boolean) {
         return (boolean)objVal ? 1 : 0
      }
      return Integer.parseInt(objVal.toString())
   }

   Long getLong(Object objVal) {
      if (objVal == null) {
         return null
      }
      if (objVal instanceof Double) {
         return (Math.round((Double) objVal))
      }
      if (objVal instanceof Boolean) {
         return (boolean)objVal ? 1L : 0L
      }
      return Long.parseLong(objVal.toString())
   }

   Boolean getBoolean(Object val) {
      if (val == null || "" == val) {
         return null
      }
      if (val instanceof Boolean) {
         return (Boolean) val
      } else if (val instanceof Number) {
         int num = (int)Math.round(((Number)val).doubleValue())
         return num == 1
      } else {
         String strVal = String.valueOf(val).toLowerCase()
         switch (strVal) {
            case "j":
            case "y":
            case "ja":
            case "yes":
            case "1":
            case "true":
            case "on":
               return true
            default:
               return false
         }
      }
   }

   static String getString(Object val) {
      return val == null ? null : String.valueOf(val)
   }
}
