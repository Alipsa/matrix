package se.alipsa.matrix.pictura

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
