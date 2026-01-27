[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-bigquery/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-bigquery)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-bigquery/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-bigquery)
# Matrix Big Query

This module makes it simple to query data from Google Big Query and get the result back as a Matrix and also to export a Matrix to Big Query.

To use it, add the following to your gradle build script
```groovy
implementation 'org.apache.groovy:groovy:5.0.3'
implementation 'se.alipsa.matrix:matrix-core:3.5.0'
implementation 'se.alipsa.matrix:matrix-bigquery:0.5.0'
```
To export and import data:
```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.bigquery.*

Matrix data = Dataset.cars()
// This assumes you have set the environment variable
// GOOGLE_CLOUD_PROJECT. An alternative would be to pass
// the Big Query projectId to the constructor
Bq bq = new Bq()
// Export
bq.saveToBigQuery(data, 'mydataset.cars')
// Import
Matrix d2 = bq.query("select * from 'mydataset.cars'")
assert data == d2
```

## Query Execution Modes

Matrix BigQuery supports two query execution modes:

**Synchronous (default):**
```groovy
Bq bq = new Bq()  // or new Bq(false)
// 10 GB response size limit
// Compatible with BigQuery emulators for testing
```

**Asynchronous (recommended for production):**
```groovy
Bq bq = new Bq(true)  // Enable async queries
// No size limit
// Optimal for large datasets in production
```

The async mode is superior for production use as it has no response size limitations. The sync mode (default) is maintained for compatibility with BigQuery emulators used in testing.
See the [BqTest](src/test/groovy/se/alipsa/matrix/bigquery/BqTest.groovy) for more usage examples.

# 3:rd party libraries used

## com.google.cloud:google-cloud-bigquery
Used to access Google Big Query
License: Apache 2.0

## com.google.auth:google-auth-library-oauth2-http
Used for authentication against Google Big Query
License: BSD-3-Clause

## com.google.cloud:google-cloud-resourcemanager
Used for Google project and other resource operations
License: Apache 2.0