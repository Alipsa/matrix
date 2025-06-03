[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-csv/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-csv)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-csv/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-csv)
# matrix-csv
Comprehensive support for creating a Matrix from structured text files (CSV files) and writing a Matrix to
a CSV file in the format of choice.

To use it in your project, add the following dependencies to your code
```groovy
implementation 'se.alipsa.matrix:matrix-core:3.3.0'
implementation 'se.alipsa.matrix:matrix-csv:2.2.0' 
```

## Import a CSV file into a Matrix
Matrix-csv uses apache-commons csv to parse the csv file. Here is a simple example:

```groovy
import org.apache.commons.csv.CSVFormat
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.matrixcsv.CsvImporter

URL url = getClass().getResource("/basic.csv")
CSVFormat format = CSVFormat.Builder.create().setTrim(true).build()
Matrix basic = CsvImporter.importCsv(url, format)
```

For more advanced cases see [the apache commons csv user guide](https://commons.apache.org/proper/commons-csv/user-guide.html)

A slightly more complicated example:
Given the following text file:
```
1;"Per";"2023-Apr-30";234,12
2;"Karin";"2023-May-10";345,22

3;"Tage";"2023-Jun-20";3489,01
4;"Arne";"2023-Jul-01";222,99
```

...we can parse as follows:
```groovy
import org.apache.commons.csv.CSVFormat
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvImporter

URL url = getClass().getResource("/colonQuotesEmptyLine.csv")
CSVFormat format = CSVFormat.Builder.create()
    .setTrim(true)
    .setDelimiter(';')
    .setIgnoreEmptyLines(true)
    .setQuote('"' as Character)
    .setHeader('id', 'name', 'date', 'amount')
    .build()
Matrix matrix = CsvImporter.importCsv(url, format)
```

The resulting Matrix will be all strings. To convert the content to the appropriate type, use the `convert` method e.g.
```groovy
Matrix table = matrix.convert(
  [
      "id": Integer, 
      "name": String, 
      "date": LocalDate, 
      "amount": BigDecimal
  ],
  DateTimeFormatter.ofPattern("yyyy-MMM-dd"),
  NumberFormat.getInstance(Locale.GERMANY)
)
//the following assertions then applies
assert 4 == table.rowCount() // Number of rows
assert ['id', 'name', 'date', 'amount'] == table.columnNames() // Column names
assert [4, 'Arne', LocalDate.parse('2023-07-01'), 222.99] == table.row(3) // last row
```

## Exporting a Matrix to a CSV file

```groovy
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.csv.CsvExporter
import org.apache.commons.csv.CSVFormat

File file = File.createTempFile('mtcars', '.csv')
CsvExporter.exportToCsv(Dataset.mtcars(), CSVFormat.DEFAULT, file)
```
exportToCsv() takes a File or a Writer as output parameter.


# Release version compatibility matrix
The following table illustrates the version compatibility of the matrix-csv and matrix core

|     Matrix csv |    Matrix core | 
|---------------:|---------------:|
|          1.0.0 | 1.2.3 -> 1.2.4 |
|          1.0.1 | 2.0.0 -> 2.1.1 |
|          1.1.0 |          2.2.0 |
| 2.0.0 -> 2.2.0 | 2.2.0 -> 3.3.0 |