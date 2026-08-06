package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.logging.MatrixLogging

import static org.junit.jupiter.api.Assertions.assertNotNull

/** Verifies the optional logging module and Matrix logger are present in the resolved runtime. */
@Tag('logging')
class LoggingApiIT implements ApiItSupport {

  @Test
  void matrixLoggerUsesTheOptionalBackend() {
    assertNotNull(MatrixLogging)
    Logger logger = Logger.getLogger(LoggingApiIT)
    logger.info('matrix-bom logging integration test')
    logger.warn('matrix-bom logging warning integration test')
  }
}
