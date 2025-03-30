# Matrix Parquet Module

The Matrix Parquet module enables importing data from Apache Parquet files into Matrix objects and exporting Matrix objects to Parquet files. This module is particularly useful when working with big data ecosystems that commonly use the Parquet format.

## What is Apache Parquet?

Apache Parquet is a columnar storage file format designed for efficient data storage and retrieval. It offers efficient data compression and encoding schemes with enhanced performance to handle complex data in bulk. Parquet is commonly used in big data processing frameworks like Hadoop, Spark, and other data processing systems.

## Installation

To use the matrix-parquet module, add the following dependencies to your project:

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:4.0.26'
implementation 'se.alipsa.matrix:matrix-core:3.0.0'
implementation 'se.alipsa.matrix:matrix-parquet:0.2'
```

### Maven Configuration

```xml
<dependencies>
  <dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>4.0.26</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-core</artifactId>
    <version>3.0.0</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-parquet</artifactId>
    <version>0.2</version>
  </dependency>
</dependencies>
```

## Using the Matrix Parquet Module

The matrix-parquet module provides a simple API through the `MatrixParquetIO` class, which has methods for reading from and writing to Parquet files.

### Importing Data from a Parquet File

To import data from a Parquet file into a Matrix object:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.parquet.MatrixParquetIO

// Read a Parquet file into a Matrix
File parquetFile = new File("path/to/data.parquet")
Matrix data = MatrixParquetIO.read(parquetFile, 'myData')

// You can also omit the name parameter, and it will use the file name as the Matrix name
Matrix data2 = MatrixParquetIO.read(parquetFile)
```

### Exporting a Matrix to a Parquet File

To export a Matrix object to a Parquet file:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.parquet.MatrixParquetIO

// Create or obtain a Matrix
Matrix data = Dataset.cars()

// Write the Matrix to a Parquet file
File outputFile = new File("path/to/output.parquet")
MatrixParquetIO.write(data, outputFile)
```

## Complete Example

Here's a complete example that demonstrates creating a Matrix, writing it to a Parquet file, and then reading it back:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.parquet.MatrixParquetIO

// Create a Matrix using a built-in dataset
Matrix cars = Dataset.cars()
println "Original Matrix:"
println cars.head(5)  // Display the first 5 rows

// Write the Matrix to a Parquet file
File parquetFile = new File("build/cars.parquet")
MatrixParquetIO.write(cars, parquetFile)
println "Matrix written to ${parquetFile.absolutePath}"

// Read the Parquet file back into a new Matrix
Matrix carsFromParquet = MatrixParquetIO.read(parquetFile, 'cars')
println "\nMatrix read from Parquet file:"
println carsFromParquet.head(5)  // Display the first 5 rows

// Verify that the original and imported Matrices are equal
assert cars == carsFromParquet
println "\nThe original Matrix and the Matrix read from Parquet are identical."
```

## Working with Large Datasets

When working with Parquet files, it's important to remember that Matrix objects are in-memory data structures. This means you need to have enough RAM available to fit the entire dataset when reading a Parquet file into a Matrix.

If you're working with very large Parquet files, consider these approaches:

1. **Filter the data**: If possible, filter the data before loading it into a Matrix.
2. **Sample the data**: Load only a sample of the data for exploratory analysis.
3. **Process in chunks**: Process the data in smaller chunks if your workflow allows it.

## Technical Implementation Details

The matrix-parquet module uses the [carpet-record](https://github.com/jerolba/carpet-record) library to handle Parquet file operations. This implementation converts each row in the Matrix to a Record class that is created dynamically to match the structure of the Matrix.

### Dependency Resolution

The carpet-record library requires some dependencies that are fetched using Groovy's `@Grab` annotation. If you encounter dependency resolution issues, you might need to manually resolve some dependencies using Maven:

```bash
mvn dependency:get -Dartifact='commons-pool:commons-pool:1.6'
mvn dependency:get -Dartifact='com.google.guava:guava:27.0-jre'
```

### Limitations

There are a few limitations to be aware of when using the matrix-parquet module:

1. **In-memory processing**: As mentioned, Matrix objects are in-memory structures, so very large Parquet files might cause memory issues.
2. **Classpath conflicts**: If you have carpet-record in your system classpath, the dynamic Record class creation might not work correctly.
3. **Experimental status**: The matrix-parquet module is marked as experimental, which means its API might change in future releases.

## Third-Party Libraries

The matrix-parquet module relies on the following third-party libraries:

1. **com.jerolba:carpet-record**: Used to import and export Matrices to and from Parquet files.
   - License: Apache License 2.0

2. **org.apache.hadoop:hadoop-client**: Required for carpet-record to work.
   - License: Apache License 2.0

## Best Practices

Here are some best practices for working with the matrix-parquet module:

1. **Memory management**: Be mindful of memory usage when working with large Parquet files.
2. **Error handling**: Implement proper error handling to catch and handle exceptions that might occur during file operations.
3. **File paths**: Use absolute file paths to avoid issues with relative paths.
4. **Testing**: Always test your Parquet file operations with small datasets before working with larger ones.
5. **Version compatibility**: Keep track of the version compatibility between matrix-core and matrix-parquet modules.

## Conclusion

The matrix-parquet module provides a convenient way to work with Parquet files in the Matrix library. It allows you to easily import data from Parquet files into Matrix objects and export Matrix objects to Parquet files, enabling integration with big data ecosystems.

In the next section, we'll explore the matrix-bigquery module, which provides functionality for interacting with Google BigQuery.
