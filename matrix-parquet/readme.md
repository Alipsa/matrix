# Matrix-Parquet

This module enables import of a [Parquet](https://parquet.apache.org/) file into a Matrix and export of a Matrix to a Parquet file. There are two implementations available, one (MatrixParquetIO) using the [carpet-record](https://github.com/jerolba/parquet-carpet)
and the other (MatrixParquetWriter and MatrixParquetReader) using the [Apache Parquet](https://parquet.apache.org/) library directly (it is much faster but has some limitations with regards to data types). 

To use it, add the following to your gradle build script
```groovy
implementation 'org.apache.groovy:groovy:4.0.27'
implementation 'se.alipsa.matrix:matrix-core:3.3.0'
implementation 'se.alipsa.matrix:matrix-parquet:0.3.0'
```
# Usage
## MatrixParquetWriter and MatrixParquetReader
### Reading a Parquet file
```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetReader

File file = new File("build/cars.parquet")
Matrix matrix = MatrixParquetReader.read(file)
```
### Writing a Matrix to a Parquet file
```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetWriter
import se.alipsa.matrix.datasets.Dataset

Matrix data = Dataset.cars().withMatrixName('cars')
File file = new File("build/cars.parquet")
MatrixParquetWriter.write(data, file)
```
### Supported data types
- Integer
- Long
- BigInteger
- Float
- Double
- BigDecimal
- Boolean
- LocalDate
- java.sql.Date
- java.sql.Time
- LocalDateTime
- java.util.Date
- Everything else is stored and read back as a String

## MatrixParquetIO
Parquet support in MatrixParquetIO is enabled by the [parquet-carpet](https://github.com/jerolba/parquet-carpet) library. Note that a Matrix is an in-memory structure, hence
you need to have enough RAM available to fit the Matrix resulting from reading a parquet file.


To export and import a parquet file:
```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.parquet.MatrixParquetIO

Matrix data = Dataset.cars()
File file = new File("build/cars.parquet")
// Export 
MatrixParquetIO.write(data, file)
// Import, the file and the name of the matrix (if omitted it will be same as the file name)
Matrix d2 = MatrixParquetIO.read(file, 'cars')
assert data == d2
```

## Technical notes
carpet-record requires each row to be a Record so this, initial, implementation converts each row to a Record class that is created dynamically matching each Row in the Matrix. 
For this to work, carpet-record must be fetched using @Grab.
If you have carpet-record in the system classpath this approach will not work since carpet-record will not be able to understand the dynamically created Record class. Grab has some quirks that makes it fail if the pom file is downloaded but not the jar. If you end up in this situation, the easiest way to handle it is to resolve the dependencies manually using maven and looking at the error message for what is missing e.g.
```shell
mvn dependency:get -Dartifact='commons-pool:commons-pool:1.6'
mvn dependency:get -Dartifact='com.google.guava:guava:27.0-jre'
```

# 3:rd party libraries used

## com.jerolba:carpet-record
Used to import and export Matrices to and from Parquet. 
- licence:  Apache License 2.0

## org.apache.hadoop:hadoop-client
Needed for carpet-record to work
- licence:  Apache License 2.0

## org.apache.parquet:parquet-column
Needed for the MatrixParquet reader and writer to work
- licence:  Apache License 2.0
 
## org.apache.parquet:parquet-hadoop
Needed for the MatrixParquet reader and writer to work
- licence:  Apache License 2.0

## org.apache.hadoop:hadoop-common
Needed for the MatrixParquet reader and writer to work
- licence:  Apache License 2.0

## org.apache.hadoop:hadoop-mapreduce-client-core
Needed for the MatrixParquet reader and writer to work
- licence:  Apache License 2.0