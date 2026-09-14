package se.alipsa.matrix.bigquery

/**
 * Indicates that BigQuery completed a load job with a definite failure.
 *
 * <p>This differs from {@link BqException} errors that report an unknown write outcome: a
 * LoadJobFailedException means the load job was found and BigQuery reported its failure, so the
 * caller can handle that definite result without probing or retrying the original write.</p>
 */
class LoadJobFailedException extends BqException {

  /**
   * Creates an exception with BigQuery's reported load-job failure details.
   *
   * @param message the load-job failure details
   */
  LoadJobFailedException(String message) {
    super(message)
  }
}
