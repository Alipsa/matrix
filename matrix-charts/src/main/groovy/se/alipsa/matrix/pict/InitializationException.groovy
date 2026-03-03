package se.alipsa.matrix.pict

import groovy.transform.CompileStatic

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
