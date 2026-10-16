package se.alipsa.matrix.arff

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

class MatrixArffWriterInternalsTest {

  @Test
  void ensureParentDirectoriesAcceptsPathWithoutParent() {
    MatrixArffWriter.ensureParentDirectories(java.nio.file.Path.of('out.arff'))
    assertEquals(null, java.nio.file.Path.of('out.arff').parent)
  }

}
