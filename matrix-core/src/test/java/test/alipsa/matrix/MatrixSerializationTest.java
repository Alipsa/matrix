package test.alipsa.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.alipsa.matrix.core.ListConverter.toDoubles;
import static se.alipsa.matrix.core.ListConverter.toIntegers;
import static se.alipsa.matrix.core.ListConverter.toStrings;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.alipsa.matrix.core.Matrix;

public class MatrixSerializationTest {

  @Test
  void serializeMatrixRoundTrip() {
    // Create a Matrix with typed data (data includes header row)
    Matrix matrix = Matrix.builder()
        .matrixName("TestMatrix")
        .columnNames("Name", "Age", "Score")
        .columns(
            toStrings("Alice", "Bob"),
            toIntegers(25, 30),
            toDoubles(95.5, 87.3)
        )
        .types(String.class, Integer.class, Double.class)
        .build();

    // Serialize
    String csv = matrix.toCsvString();
    assertNotNull(csv, "Serialized matrix should not be null");

    System.out.println(csv);
    assertNotNull(csv, "CSV should not be null");
    assertTrue(csv.contains("#name: TestMatrix"), "CSV should contain matrix name");
    assertTrue(csv.contains("#types:"), "CSV should contain types");
    assertTrue(csv.contains("String,Integer,Double"));
    assertTrue(csv.contains("Name,Age,Score"), "CSV should contain header");
    assertTrue(csv.contains("Alice,25,95.5"), "CSV should contain data row 1");
    assertTrue(csv.contains("Bob,30,87.3"), "CSV should contain data row 2");

    // Deserialize
    Matrix resultMatrix = Matrix.builder().csvString(csv).build();
    assertEquals("TestMatrix", resultMatrix.getMatrixName(), "Matrix name should match");
    assertEquals(3, resultMatrix.columnCount(), "Should have 3 columns");
    assertEquals(2, resultMatrix.rowCount(), "Should have 2 data rows (excluding header)");
    assertEquals("Alice", resultMatrix.getAt(0, "Name"), "First row name should match");
    assertEquals(Integer.valueOf(25), resultMatrix.getAt(0, "Age"), "First row age should match");
    assertIterableEquals(List.of(String.class, Integer.class, Double.class), resultMatrix.types(), "Types should match");
  }
}
