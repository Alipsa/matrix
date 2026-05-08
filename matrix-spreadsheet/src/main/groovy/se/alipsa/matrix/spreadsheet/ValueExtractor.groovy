package se.alipsa.matrix.spreadsheet

/**
 * A ValueExtractor is a helper class that makes it easier to get values from a spreadsheet.
 */
@SuppressWarnings('ReturnNullFromCatchBlock')
abstract class ValueExtractor {

   private static final String PERCENT = '%'
   private static final BigDecimal HUNDRED = 100.0

   BigDecimal getBigDecimal(Object val) {
      if (val == null) {
         return null
      }
      if (Number.isInstance(val)) {
         return (val as Number) as BigDecimal
      }
      String strVal = val.toString().trim()
      try {
         return new BigDecimal(strVal)
      } catch (NumberFormatException e) {
         // Try parsing as percentage (e.g., "50%" -> 0.5)
         if (strVal.endsWith(PERCENT)) {
            try {
               String numPart = strVal.substring(0, strVal.length() - 1).trim()
               return new BigDecimal(numPart) / HUNDRED
            } catch (NumberFormatException ignored) {
               // do nothing
            }
         }
         return null
      }
   }

   BigDecimal getPercentage(Object val) {
      if (val == null) {
         return null
      }
      if (Number.isInstance(val)) {
         return (val as Number) as BigDecimal
      }
      String strVal = val.toString().trim()
      if (strVal.endsWith(PERCENT)) {
         try {
            String numPart = strVal.substring(0, strVal.length() - 1).trim()
            return new BigDecimal(numPart) / HUNDRED
         } catch (NumberFormatException e) {
            return null
         }
      }
      try {
         return new BigDecimal(strVal)
      } catch (NumberFormatException e) {
         return null
      }
   }

   static Integer getInt(Object objVal) {
      if (objVal == null) {
         return null
      }
      if (Double.isInstance(objVal)) {
         return (int)(Math.round((Double) objVal))
      }
      if (Boolean.isInstance(objVal)) {
         return (boolean)objVal ? 1 : 0
      }
      return Integer.parseInt(objVal.toString())
   }

   Long getLong(Object objVal) {
      if (objVal == null) {
         return null
      }
      if (Double.isInstance(objVal)) {
         return (Math.round((Double) objVal))
      }
      if (Boolean.isInstance(objVal)) {
         return (boolean)objVal ? 1L : 0L
      }
      return Long.parseLong(objVal.toString())
   }

   @SuppressWarnings('BooleanMethodReturnsNull')
   Boolean getBoolean(Object val) {
      if (val == null || '' == val) {
         return null
      }
      if (Boolean.isInstance(val)) {
         return (Boolean) val
      }
      if (Number.isInstance(val)) {
         int num = (int)Math.round(((Number)val).doubleValue())
         return num == 1
      }
      String strVal = String.valueOf(val).toLowerCase()
      final Map<String, Boolean> truthyValues = [
          j: true, y: true, ja: true, yes: true,
          '1': true, 'true': true, 'on': true
      ]
      truthyValues.getOrDefault(strVal, false)
   }

   static String getString(Object val) {
      val == null ? null : String.valueOf(val)
   }

}
