import java.io.File;
import java.util.List;
import se.alipsa.matrix.core.Matrix;
import se.alipsa.matrix.parquet.MatrixParquetReader;
import se.alipsa.matrix.parquet.MatrixParquetWriter;
import se.alipsa.matrix.parquet.Predicates;

/** Compile-only check: consumers need no parquet-java or Hadoop on their compile classpath. */
public final class ParquetConsumer {
  /** Selects an ID preview without upstream predicate types. */
  public static Matrix select(File file) {
    return MatrixParquetReader.builder().columns(List.of("id"))
        .filter(Predicates.ge("id", 1000L)).limit(10).read(file);
  }

  /** Writes using Matrix-owned tuning methods. */
  public static void save(Matrix matrix, File file) {
    MatrixParquetWriter.builder(matrix).pageSize(4096).writerVersion("V2").write(file);
  }
}
