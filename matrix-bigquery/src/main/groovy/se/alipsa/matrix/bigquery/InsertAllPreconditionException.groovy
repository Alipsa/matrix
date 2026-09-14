package se.alipsa.matrix.bigquery

import groovy.transform.PackageScope

/**
 * Indicates that InsertAll cannot preserve the requested overwrite semantics before submission.
 *
 * <p>This is package-scoped intentionally: it controls fallback logging within {@link Bq} and is
 * not part of the public write-outcome contract.</p>
 */
@PackageScope
class InsertAllPreconditionException extends BqException {

  InsertAllPreconditionException(String message) {
    super(message)
  }
}
