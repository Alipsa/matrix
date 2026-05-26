package se.alipsa.matrix.pict

import groovy.transform.CompileStatic

/** Exception thrown when chart initialization fails. */
@CompileStatic
class InitializationException extends RuntimeException {

  InitializationException() {
    super()
  }

  InitializationException(String message) {
    super(message)
  }

  InitializationException(String message, Throwable cause) {
    super(message, cause)
  }

}
