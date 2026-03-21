package se.alipsa.matrix.bigquery

import groovy.transform.CompileStatic

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.DatasetId
import com.google.cloud.bigquery.TableId
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * These tests live in `se.alipsa.matrix.bigquery` so they can access the
 * `@PackageScope` ID helpers used to keep dataset and table operations scoped to `projectId`.
 */
@CompileStatic
class ProjectScopedIdTest {

  @Test
  void datasetIdUsesInstanceProjectId() {
    Bq bq = createBq('matrix-project')

    DatasetId datasetId = bq.datasetId('analytics')

    assertEquals('matrix-project', datasetId.project)
    assertEquals('analytics', datasetId.dataset)
  }

  @Test
  void tableIdUsesInstanceProjectId() {
    Bq bq = createBq('matrix-project')

    TableId tableId = bq.tableId('analytics', 'events')

    assertEquals('matrix-project', tableId.project)
    assertEquals('analytics', tableId.dataset)
    assertEquals('events', tableId.table)
  }

  @Test
  void tableIdOverloadUsesExplicitProjectId() {
    TableId tableId = Bq.tableId('other-project', 'analytics', 'events')

    assertEquals('other-project', tableId.project)
    assertEquals('analytics', tableId.dataset)
    assertEquals('events', tableId.table)
  }

  private static Bq createBq(String projectId) {
    BigQueryOptions options = BigQueryOptions.newBuilder()
        .setProjectId(projectId)
        .setCredentials(NoCredentials.getInstance())
        .build()
    new Bq(options)
  }
}
