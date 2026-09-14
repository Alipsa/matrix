package se.alipsa.matrix.bigquery

import groovy.transform.PackageScope

/**
 * Indicates that InsertAll cannot preserve the requested overwrite semantics before submission.
 */
@PackageScope
class InsertAllPreconditionException extends BqException {

  InsertAllPreconditionException(String message) {
    super(message)
  }
}
