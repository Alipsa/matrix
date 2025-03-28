# Matrix Big Query

This module makes it simple to query data from Google Big Query and get the result back as a Matrix and also to export a Matrix to Big Query.

To use it, add the following to your gradle build script
```groovy
implementation 'org.apache.groovy:groovy:4.0.26'
implementation 'se.alipsa.matrix:matrix-core:3.1.0'
implementation 'se.alipsa.matrix:matrix-bigquery:0.2'
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