# Matrix Big Query

This module makes it simple to query data from Google Big Query and get the result back as a Matrix and also to export a Matrix to Big Query.

**Note: This module is still work in progress and not published to Maven central yet!**

To use it, add the following to your gradle build script
```groovy
implementation 'org.apache.groovy:groovy:4.0.25'
implementation 'se.alipsa.matrix:matrix-core:2.2.0'
implementation 'se.alipsa.matrix:matrix-bigquery:0.1'
```
To export and import a parquet file:
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