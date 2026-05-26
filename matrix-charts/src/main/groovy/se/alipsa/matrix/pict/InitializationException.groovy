package se.alipsa.matrix.pict

/** Exception thrown when chart initialization fails. */
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
