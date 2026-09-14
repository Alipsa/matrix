package se.alipsa.matrix.bigquery

import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.JobStatistics

/**
 * Describes the outcome of a Matrix insert operation.
 *
 * <p>InsertAll does not create a load job, so its {@link #loadStatistics} and
 * {@link #writeJobId} are null. A successful InsertAll response establishes the
 * successful row count, while a write-channel result obtains it from its load job.</p>
 */
class BqInsertResult {

  /** The API used to write the rows. */
  enum WriteMechanism {
    WRITE_CHANNEL,
    INSERT_ALL
  }

  final long requestedRowCount
  final Long successfulRowCount
  final WriteMechanism writeMechanism
  final JobId writeJobId
  final JobStatistics.LoadStatistics loadStatistics

  /**
   * Creates an insert result.
   *
   * @param requestedRowCount number of rows submitted by the caller
   * @param successfulRowCount number of successfully written rows when known
   * @param writeMechanism API used for the write
   * @param writeJobId load job identifier, or null for InsertAll
   * @param loadStatistics load statistics, or null for InsertAll
   */
  BqInsertResult(long requestedRowCount, Long successfulRowCount, WriteMechanism writeMechanism,
                 JobId writeJobId, JobStatistics.LoadStatistics loadStatistics) {
    this.requestedRowCount = requestedRowCount
    this.successfulRowCount = successfulRowCount
    this.writeMechanism = writeMechanism
    this.writeJobId = writeJobId
    this.loadStatistics = loadStatistics
  }
}
