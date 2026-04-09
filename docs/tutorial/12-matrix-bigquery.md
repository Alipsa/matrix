# Matrix BigQuery Module

The Matrix BigQuery module lets you query BigQuery into a `Matrix`, manage datasets and tables, and save a `Matrix` back to BigQuery with automatic schema creation.

## Installation

### Gradle

```groovy
implementation 'org.apache.groovy:groovy:5.0.5'
implementation 'se.alipsa.matrix:matrix-core:3.6.0'
implementation 'se.alipsa.matrix:matrix-bigquery:0.6.0'
```

### Maven

```xml
<dependencies>
  <dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>5.0.4</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-core</artifactId>
    <version>3.6.0</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-bigquery</artifactId>
    <version>0.6.0</version>
  </dependency>
</dependencies>
```

## Authentication

`Bq` uses normal Google Cloud authentication:

1. Set `GOOGLE_CLOUD_PROJECT` and use `new Bq()`
2. Or pass the project explicitly with `new Bq('my-project-id')`
3. Or pass credentials explicitly with `new Bq(credentials, 'my-project-id')`

Typical local setup uses Application Default Credentials:

```bash
gcloud auth application-default login
export GOOGLE_CLOUD_PROJECT=my-project-id
```

Service-account setup works too:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
export GOOGLE_CLOUD_PROJECT=my-project-id
```

## Creating a Bq instance

```groovy
import com.google.auth.oauth2.GoogleCredentials
import se.alipsa.matrix.bigquery.Bq

Bq defaultBq = new Bq()
Bq explicitProject = new Bq('my-project-id')
Bq asyncBq = new Bq('my-project-id', true)

GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream('service-account.json'))
Bq credentialBased = new Bq(credentials, 'my-project-id', true)
```

Sync mode is the default and is useful for emulator compatibility. Async mode removes the synchronous query size limit and is the better default for production workloads.

## Querying into a Matrix

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.Matrix

Bq bq = new Bq('my-project-id')

Matrix topCars = bq.query("""
  select model, mpg, hp
  from `my-project-id.analytics.mtcars`
  order by mpg desc
  limit 10
""").withMatrixName('topCars')
```

Use `.withMatrixName(...)` if you want the returned matrix to carry a stable, meaningful name in later processing.

## Creating datasets and tables safely

Dataset names are project-scoped to the `Bq` instance. The helper methods below all work against that configured project.

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.datasets.Dataset

Bq bq = new Bq('my-project-id')

if (!bq.datasetExist('analytics')) {
  bq.createDataset('analytics', 'Analytics data for Matrix examples')
}

def cars = Dataset.cars()
if (!bq.tableExist('analytics', cars.matrixName)) {
  bq.createTable(cars, 'analytics')
}
```

If you do not provide a custom schema, `createTable(...)` derives the BigQuery schema from the Matrix column types.

## Saving a Matrix

`saveToBigQuery(matrix, datasetName)` uses `matrix.matrixName` as the table name. The dataset argument is only the dataset.

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.datasets.Dataset

Bq bq = new Bq('my-project-id')
def cars = Dataset.cars()

// Overwrite existing table contents (default)
bq.saveToBigQuery(cars, 'analytics')

// Append instead of overwrite
bq.saveToBigQuery(cars, 'analytics', true)
```

If the table does not exist, `saveToBigQuery(...)` creates it first. If it already exists and `append` is `false`, existing rows are replaced.

## Round-trip example

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.core.MatrixAssertions

String projectId = 'my-project-id'
Bq bq = new Bq(projectId)
def cars = Dataset.cars()

bq.createDataset('analytics')
bq.saveToBigQuery(cars, 'analytics')

def copy = bq.query("select * from `${projectId}.analytics.${cars.matrixName}` order by speed")
    .withMatrixName(cars.matrixName)

MatrixAssertions.assertContentMatches(cars, copy)
```

## Date, time, and numeric types

The module maps common Matrix types to BigQuery types and converts values into BigQuery-friendly JSON formats when necessary.

Common cases:

- `Integer`, `Long`, `Short`, `Byte` -> `INT64`
- `Double`, `Float` -> `FLOAT64`
- `BigDecimal` -> `BIGNUMERIC`
- `LocalDate` and `Date` -> `DATE`
- `LocalTime` and `Time` -> `TIME`
- `LocalDateTime` -> `DATETIME`
- `Instant`, `Timestamp`, `ZonedDateTime` -> `TIMESTAMP`

Example:

```groovy
import se.alipsa.matrix.core.Matrix

Matrix events = Matrix.builder()
    .matrixName('events')
    .columnNames(['id', 'created_at', 'amount'])
    .types([Integer, Instant, BigDecimal])
    .rows([
        [1, Instant.parse('2026-03-21T10:15:30Z'), 12.50],
        [2, Instant.parse('2026-03-21T11:45:30Z'), 19.95]
    ])
    .build()
```

## Metadata and administration helpers

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq('my-project-id')

List<String> datasets = bq.getDatasets()
List<String> tables = bq.getTableNames('analytics')
Matrix tableInfo = bq.getTableInfo('analytics', 'mtcars')
def projects = bq.getProjects()
```

`getTableInfo(...)` returns column metadata from `INFORMATION_SCHEMA.COLUMNS`.

## Executing DDL and DML

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq('my-project-id', true)

bq.execute("create table `my-project-id.analytics.temp_people` (id int64, name string)")
bq.execute("insert into `my-project-id.analytics.temp_people` (id, name) values (1, 'Alice')")
bq.execute("update `my-project-id.analytics.temp_people` set name = 'Bob' where id = 1")
```

## Emulator and test-oriented configuration

The BigQuery emulator usually works best with sync queries and `InsertAll`.

```groovy
import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQueryOptions
import se.alipsa.matrix.bigquery.Bq

System.setProperty('bigquery.enable_write_api', 'false')
System.setProperty('bigquery.enable_progress_bar', 'false')

BigQueryOptions options = BigQueryOptions.newBuilder()
    .setProjectId('emulator-project')
    .setHost('http://localhost:9050')
    .setLocation('http://localhost:9050')
    .setCredentials(NoCredentials.getInstance())
    .build()

Bq bq = new Bq(options)
```

Useful properties:

- `bigquery.enable_write_api=false` forces `InsertAll` instead of the write channel
- `bigquery.enable_progress_bar=false` suppresses the write progress bar even when a TTY is present

## Error handling

Matrix BigQuery wraps failures in `BqException`:

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.bigquery.BqException

try {
  new Bq('my-project-id').query('select * from `missing.dataset.table`')
} catch (BqException e) {
  println "BigQuery error: ${e.message}"
}
```
