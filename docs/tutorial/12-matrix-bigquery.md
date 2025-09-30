# Matrix BigQuery Module

The Matrix BigQuery module makes it simple to query data from Google BigQuery and get the results back as a Matrix object, as well as export a Matrix to BigQuery. This module provides a convenient interface for working with Google's powerful cloud data warehouse.

## What is Google BigQuery?

Google BigQuery is a fully-managed, serverless data warehouse that enables scalable analysis over petabytes of data. It is a platform for data analytics that allows you to run SQL-like queries against very large datasets, with the processing done in Google's cloud infrastructure.

## Installation

To use the matrix-bigquery module, add the following dependencies to your project:

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.1'
implementation platform('se.alipsa.matrix:matrix-bom:2.2.3')
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-bigquery'
```

### Maven Configuration

```xml
<project>
   <dependencyManagement>
      <dependencies>
         <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-bom</artifactId>
            <version>2.2.3</version>
            <type>pom</type>
            <scope>import</scope>
         </dependency>
      </dependencies>
   </dependencyManagement>
   <dependencies>
     <dependency>
       <groupId>org.apache.groovy</groupId>
       <artifactId>groovy</artifactId>
       <version>5.0.1</version>
     </dependency>
     <dependency>
       <groupId>se.alipsa.matrix</groupId>
       <artifactId>matrix-core</artifactId>
     </dependency>
     <dependency>
       <groupId>se.alipsa.matrix</groupId>
       <artifactId>matrix-bigquery</artifactId>
     </dependency>
   </dependencies>
</project>
```

## Authentication Setup

Before you can use the matrix-bigquery module, you need to set up authentication with Google Cloud:

1. **Create a Google Cloud Project**: If you don't already have one, create a project in the [Google Cloud Console](https://console.cloud.google.com/).

2. **Enable the BigQuery API**: In your Google Cloud project, enable the BigQuery API.

3. **Set up Authentication**: There are several ways to authenticate:

   a. **Using Application Default Credentials**:
      - Install the [Google Cloud SDK](https://cloud.google.com/sdk/docs/install)
      - Run `gcloud auth application-default login`
      - Set the environment variable `GOOGLE_CLOUD_PROJECT` to your project ID

   b. **Using a Service Account**:
      - Create a service account in the Google Cloud Console
      - Download the JSON key file
      - Set the environment variable `GOOGLE_APPLICATION_CREDENTIALS` to the path of your JSON key file

## Using the Matrix BigQuery Module

The matrix-bigquery module provides a simple API through the `Bq` class, which has methods for querying data from BigQuery and saving Matrix objects to BigQuery.

### Creating a Bq Instance

```groovy
import se.alipsa.matrix.bigquery.Bq

// Create a Bq instance using the GOOGLE_CLOUD_PROJECT environment variable
Bq bq = new Bq()

// Or specify the project ID explicitly
Bq bq = new Bq("my-project-id")
```

### Querying Data from BigQuery

To query data from BigQuery and get the results as a Matrix:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.bigquery.Bq

// Create a Bq instance
Bq bq = new Bq()

// Execute a query and get the results as a Matrix
Matrix data = bq.query("SELECT * FROM `my-project.my_dataset.my_table` LIMIT 1000")

// Print the first few rows of the result
println data.head(5)
```

### Saving a Matrix to BigQuery

To save a Matrix to a BigQuery table:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.bigquery.Bq

// Create or obtain a Matrix
Matrix data = Dataset.cars()

// Create a Bq instance
Bq bq = new Bq()

// Save the Matrix to BigQuery
bq.saveToBigQuery(data, "my_dataset.cars")
```

The `saveToBigQuery` method will create the table if it doesn't exist, or append to it if it does.

### Complete Example

Here's a complete example that demonstrates creating a Matrix, saving it to BigQuery, and then querying it back:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.bigquery.Bq

// Create a Matrix using a built-in dataset
Matrix cars = Dataset.cars()
println "Original Matrix:"
println cars.head(5)  // Display the first 5 rows

// Create a Bq instance
Bq bq = new Bq()

// Save the Matrix to BigQuery
String tableId = "my_dataset.cars"
bq.saveToBigQuery(cars, tableId)
println "Matrix saved to BigQuery table: ${tableId}"

// Query the data back from BigQuery
Matrix carsFromBigQuery = bq.query("SELECT * FROM `my_dataset.cars`")
println "\nMatrix queried from BigQuery:"
println carsFromBigQuery.head(5)  // Display the first 5 rows

// Verify that the original and queried Matrices are equal
// Note: The order of rows might be different, so we might need to sort both matrices first
assert cars.rowCount() == carsFromBigQuery.rowCount()
assert cars.columnCount() == carsFromBigQuery.columnCount()
println "\nThe original Matrix and the Matrix from BigQuery have the same dimensions."
```

## Advanced Usage

### Working with BigQuery Projects

You can list all available projects:

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq()
List<String> projects = bq.listProjects()
println "Available projects: ${projects}"
```

### Working with BigQuery Datasets

You can list all datasets in a project:

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq()
List<String> datasets = bq.listDatasets()
println "Available datasets: ${datasets}"
```

### Working with BigQuery Tables

You can list all tables in a dataset:

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq()
List<String> tables = bq.listTables("my_dataset")
println "Available tables in my_dataset: ${tables}"
```

### Executing DDL and DML Statements

You can execute Data Definition Language (DDL) and Data Manipulation Language (DML) statements:

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq()

// Create a new table
bq.execute("CREATE TABLE `my_dataset.new_table` (id INT64, name STRING)")

// Insert data into the table
bq.execute("INSERT INTO `my_dataset.new_table` (id, name) VALUES (1, 'Alice'), (2, 'Bob')")

// Update data in the table
bq.execute("UPDATE `my_dataset.new_table` SET name = 'Charlie' WHERE id = 2")

// Delete data from the table
bq.execute("DELETE FROM `my_dataset.new_table` WHERE id = 1")
```

### Handling BigQuery Exceptions

The matrix-bigquery module wraps BigQuery exceptions in a `BigQueryException` class. You can catch and handle these exceptions:

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.bigquery.BigQueryException

Bq bq = new Bq()

try {
    bq.query("SELECT * FROM `non_existent_table`")
} catch (BigQueryException e) {
    println "BigQuery error: ${e.message}"
    // Handle the exception
}
```

## Performance Considerations

When working with BigQuery, consider the following performance tips:

1. **Limit Query Results**: Always use `LIMIT` in your queries when possible to avoid transferring large amounts of data.

2. **Select Only Needed Columns**: Instead of using `SELECT *`, specify only the columns you need.

3. **Use Partitioned and Clustered Tables**: For large datasets, consider using BigQuery's partitioning and clustering features.

4. **Optimize Query Cost**: Be aware of BigQuery's pricing model, which is based on the amount of data processed by queries.

## Third-Party Libraries

The matrix-bigquery module relies on the following third-party libraries:

1. **com.google.cloud:google-cloud-bigquery**: Used to access Google BigQuery.
   - License: Apache 2.0

2. **com.google.auth:google-auth-library-oauth2-http**: Used for authentication against Google BigQuery.
   - License: BSD-3-Clause

3. **com.google.cloud:google-cloud-resourcemanager**: Used for Google project and other resource operations.
   - License: Apache 2.0

## Best Practices

Here are some best practices for working with the matrix-bigquery module:

1. **Manage Authentication Carefully**: Keep your service account credentials secure and use environment variables for configuration.

2. **Handle Large Datasets Appropriately**: Be mindful of memory usage when working with large datasets.

3. **Optimize Queries**: Write efficient BigQuery queries to minimize processing costs and improve performance.

4. **Error Handling**: Implement proper error handling to catch and handle exceptions that might occur during BigQuery operations.

5. **Testing**: Test your BigQuery operations with small datasets before working with larger ones.

## Conclusion

The matrix-bigquery module provides a convenient way to integrate the Matrix library with Google BigQuery. It allows you to easily query data from BigQuery into Matrix objects and save Matrix objects to BigQuery tables, enabling powerful data analysis workflows that combine the strengths of both systems.

In the next section, we'll explore the matrix-charts module, which provides functionality for creating various types of charts and visualizations.

Go to [previous section](11-matrix-parquet.md) | Go to [next section](13-matrix-charts.md) | Back to [outline](outline.md)