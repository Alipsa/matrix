# Matrix BigQuery

Focused recipes for practical BigQuery work with Matrix.

## Connect with an explicit project

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq('my-project-id')
```

Use `new Bq('my-project-id', true)` when you want async query execution for production-scale reads.

## Connect with explicit credentials and options

```groovy
import com.google.auth.oauth2.GoogleCredentials
import se.alipsa.matrix.bigquery.Bq

GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream('service-account.json'))
Bq bq = new Bq(credentials, 'my-project-id', true)
```

## Query a BigQuery table into a Matrix

```groovy
import se.alipsa.matrix.bigquery.Bq

String projectId = 'my-project-id'
Bq bq = new Bq(projectId)

def sales = bq.query("""
  select order_id, order_date, total_amount
  from `${projectId}.analytics.orders`
  where order_date >= '2026-01-01'
  order by order_date
""").withMatrixName('sales')
```

## Create a dataset only if it does not already exist

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq('my-project-id')

if (!bq.datasetExist('analytics')) {
  bq.createDataset('analytics', 'Analytics dataset for Matrix jobs')
}
```

## Create a table from a Matrix schema

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.datasets.Dataset

Bq bq = new Bq('my-project-id')
def cars = Dataset.cars()

bq.createDataset('analytics')
bq.createTable(cars, 'analytics')
```

`createTable(...)` uses the Matrix column types to build a BigQuery schema automatically.

## Overwrite a table with `saveToBigQuery`

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.datasets.Dataset

Bq bq = new Bq('my-project-id')
def cars = Dataset.cars()

bq.saveToBigQuery(cars, 'analytics')
```

`append` defaults to `false`, so this replaces existing rows in `analytics.cars`.

## Append instead of overwrite

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.datasets.Dataset

Bq bq = new Bq('my-project-id')
def cars = Dataset.cars()

bq.saveToBigQuery(cars, 'analytics', true)
```

Use `true` explicitly any time you want additive writes and do not want overwrite semantics.

## Round-trip date/time and numeric types

```groovy
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.Matrix

Bq bq = new Bq('my-project-id')

Matrix events = Matrix.builder()
    .matrixName('events')
    .columnNames(['id', 'event_date', 'event_time', 'created_at', 'amount'])
    .types([Integer, LocalDate, LocalTime, Instant, BigDecimal])
    .rows([
        [1, LocalDate.parse('2026-03-21'), LocalTime.parse('10:15:30'), Instant.parse('2026-03-21T10:15:30Z'), 12.50],
        [2, LocalDate.parse('2026-03-22'), LocalTime.parse('12:45:00'), Instant.parse('2026-03-22T12:45:00Z'), 99.95]
    ])
    .build()

bq.saveToBigQuery(events, 'analytics')
```

Common mappings:

- `BigDecimal` -> `BIGNUMERIC`
- `LocalDate` -> `DATE`
- `LocalTime` -> `TIME`
- `LocalDateTime` -> `DATETIME`
- `Instant` / `Timestamp` / `ZonedDateTime` -> `TIMESTAMP`

## Inspect datasets, tables, and table metadata

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq('my-project-id')

println bq.getDatasets()
println bq.getTableNames('analytics')
println bq.getTableInfo('analytics', 'orders').content()
```

## Execute DDL or DML

```groovy
import se.alipsa.matrix.bigquery.Bq

Bq bq = new Bq('my-project-id', true)

bq.execute("create table `my-project-id.analytics.temp_people` (id int64, name string)")
bq.execute("insert into `my-project-id.analytics.temp_people` (id, name) values (1, 'Alice')")
```

## Use the BigQuery emulator

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

The emulator path is usually easiest with:

- sync queries (`new Bq(options)`)
- `bigquery.enable_write_api=false`
- `bigquery.enable_progress_bar=false`

## Force progress bars off in an interactive shell

```groovy
System.setProperty('bigquery.enable_progress_bar', 'false')
```

Unset means auto-detect: progress bars appear only when an interactive terminal is available.

---
[Back to index](cookbook.md)
