# Matrix-gsheets, Google Sheets support for Matrix


# Usage

Assuming a dataset like this:
```groovy
import se.alipsa.matrix.core.Matrix
def empData = Matrix.builder()
      .matrixName('empData')
      .data(
          emp_id: 1..5,
          emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
          salary: [623.3, 515.2, 611.0, 729.0, 843.25],
          start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
      )
      .types([Integer, String, BigDecimal, LocalDate])
      .build()
```
You can export it to a google sheet like this:
```groovy
import se.alipsa.matrix.gsheets.GsExporter
String spreadsheetId = GsExporter.exportSheet(empData)
println "Export completed: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
```
and import it like this:
```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsImporter
Matrix m = GsImporter.importSheet(spreadsheetId, "empData!A1:D6", true).withMatrixName(empData.matrixName)
// all column types will be Object so we need to convert them back to their original types
m.convert('emp_id': Integer,
        'emp_name': String,
        'salary': BigDecimal,
        'start_date': LocalDate
)
```
and delete it like this:
```groovy
import se.alipsa.matrix.gsheets.GsUtil
GsUtil.deleteSheet(spreadsheetId)
```

### handling date types
The Google API is a bit primitive in terms of available meta data. One such thing is handling date types.
Although it is possible to convert a date to a serial number, when downloading the sheet, there is now
way to distinguish that from an ordinary number. Hence, the gsheets converts date, time, timestamp etc 
to strings when exporting it. This allows for simple conversion back to a date type after downloading the 
sheet as seen above. However, if you want to handle dates and times as "real" date types (serials), 
you need to convert them to a serial number. The Gsutil class can help you with this.
Here is an example:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsImporter
import se.alipsa.matrix.gsheets.GsUtil

// Upload it where the local dates are converted to serials
Matrix ed = empData.clone().convert('start_date', BigDecimal) {
  GsUtil.asSerial(it)
}
def spreadsheetId = GsExporter.exportSheet(ed)

// Download it and covert the serials back to LocalDates 
Matrix m = GsImporter.importSheet(spreadsheetId, "empData!A1:D6", true).withMatrixName(empData.matrixName)
m.convert('start_date', LocalDate) {
  GsUtil.asLocalDate(it)
}
// Convert the other columns from Object to their desired types
m.convert('emp_id': Integer,
    'emp_name': String,
    'salary': BigDecimal,
)
```

### handling null values
The google api has the unfortunate feature to skipp null values.
This mean that if a cell has a null value, the returning row will be one 
element shorter. There is no way to know which column that was so the only thing we can 
do is to fill the remaining columns with null in the end which would be quit cumbersome to handle. 
To fix this, the GSExporter will send an empty string instead of null when exporting the matrix.
Correspondingly, the GsImporter will convert empty strings to null when importing the matrix.

Here is an example:
```groovy
def empData = Matrix.builder()
      .matrixName('empData')
      .data(
          emp_id: [null, 2, 3, 4, 5],
          emp_name: ["", "Dan", "Michelle", "Ryan", "Gary"],
          salary: [623.3, 515.2, null, 729.0, 843.25],
          start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
      )
      .types([Integer, String, BigDecimal, LocalDate])
      .build()
```
This will look like this:
```
empData: 5 obs * 4 variables 
emp_id	emp_name	salary	start_date
     1	    	     623.3	2012-01-01
     2	Dan     	 515.2	2013-09-23
     3	Michelle	  null	2014-11-15
     4	Ryan    	 729.0	2014-05-11
  null	Gary    	843.25	2015-03-27
```

```groovy
String spreadsheetId = GsExporter.exportSheet(ed.withMatrixName('empData'))
Matrix m = GsImporter.importSheet(spreadsheetId, "empData!A1:D6", true).withMatrixName(empData.matrixName)
println m.withMatrixName('imported').content()
```
The imported matrix will now look like this:
```
imported: 5 obs * 4 variables 
emp_id	emp_name	    salary 	start_date
     1	null    	     623.3	2012-01-01
     2	Dan     	     515.2	2013-09-23
     3	Michelle	      null  2014-11-15	      
     4	Ryan    	       729	2014-05-11
  null  Gary  	      843.25	2015-03-27   
```
If this side effect is not desired, you can use the GsImporter.importSheetAsObject method which will not convert empty strings to null, but you will have to handle the missing values yourself e.g. by using the Matrix 
move method to correct the misaligned rows values. Here is an example:

```groovy
Matrix m = GsImporter.importSheetAsObject(spreadsheetId, "empData!A1:D6", true).withMatrixName(empData.matrixName)
println m.withMatrixName('imported').content()
```
This will look like this:
```
imported: 5 obs * 4 variables 
emp_id	emp_name	salary	    start_date
     1	          623.3	      2012-01-01
     2	Dan     	515.2	2013-09-23
     3	Michelle	2014-11-15  null
     4	Ryan    	 729.0	2014-05-11
Gary    	843.25	2015-03-27  null
```
You can now use the Matrix move method to correct the misaligned rows:
```groovy
m.move(2, 'start_date', 'salary')
m.move(4, 'start_date', 'emp_id')
```

See the [Gsheets test](src/test/groovy/test/alipsa/matrix/gsheets/) for more examples.