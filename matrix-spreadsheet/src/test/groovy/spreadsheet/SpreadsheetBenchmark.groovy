package spreadsheet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
import se.alipsa.matrix.spreadsheet.SpreadsheetImporter
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.spreadsheet.SpreadsheetWriter

import java.time.Duration
import java.time.Instant
import java.math.RoundingMode

/**
 * Simple read/write benchmark runner for matrix-spreadsheet.
 */
@CompileStatic
class SpreadsheetBenchmark {

  /**
   * Run the benchmark.
   * args: [format=all|xlsx|ods, rows=50000, cols=12, warmups=1, runs=3, mode=readwrite|read|write]
   */
  static void main(String[] args) {
    String format = args?.size() > 0 ? args[0] : 'all'
    int rows = (args?.size() > 1 ? args[1] : '50000') as int
    int cols = (args?.size() > 2 ? args[2] : '12') as int
    int warmups = (args?.size() > 3 ? args[3] : '1') as int
    int runs = (args?.size() > 4 ? args[4] : '3') as int
    String mode = args?.size() > 5 ? args[5] : 'readwrite'

    List<String> formats = format == 'all' ? ['xlsx', 'ods'] : [format]
    Matrix matrix = createMatrix(rows, cols)

    formats.each { fmt ->
      println "\n=== ${fmt.toUpperCase()} benchmark (rows=${rows}, cols=${cols}, warmups=${warmups}, runs=${runs}, mode=${mode}) ==="
      runBenchmark(fmt, matrix, warmups, runs, mode)
    }
  }

  private static void runBenchmark(String format, Matrix matrix, int warmups, int runs, String mode) {
    File file = File.createTempFile("matrix-benchmark-", ".${format}")
    file.deleteOnExit()
    String endColumn = SpreadsheetUtil.asColumnName(matrix.columnCount())

    if (mode in ['read', 'readwrite']) {
      SpreadsheetWriter.write(matrix, file, 'Data')
    }

    if (mode in ['write', 'readwrite']) {
      warmups.times {
        SpreadsheetWriter.write(matrix, file, 'Data')
      }
      List<Long> writeTimes = []
      runs.times {
        writeTimes << timeMillis {
          SpreadsheetWriter.write(matrix, file, 'Data')
        }
      }
      printStats('WRITE', writeTimes)
    }

    if (mode in ['read', 'readwrite']) {
      warmups.times {
        SpreadsheetImporter.importSpreadsheet(file.path, 1, 1, Integer.MAX_VALUE, 'A', endColumn, true)
      }
      List<Long> readTimes = []
      runs.times {
        readTimes << timeMillis {
          SpreadsheetImporter.importSpreadsheet(file.path, 1, 1, Integer.MAX_VALUE, 'A', endColumn, true)
        }
      }
      printStats('READ', readTimes)
    }
  }

  private static long timeMillis(Closure<?> work) {
    Instant start = Instant.now()
    work.call()
    Instant finish = Instant.now()
    return Duration.between(start, finish).toMillis()
  }

  private static void printStats(String label, List<Long> timings) {
    long min = timings.min()
    long max = timings.max()
    long total = timings.sum() as long
    BigDecimal avg = (total as BigDecimal) / (timings.size() as BigDecimal)
    println "${label}: avg=${avg.setScale(2, RoundingMode.HALF_UP)} ms, min=${min} ms, max=${max} ms"
  }

  private static Matrix createMatrix(int rows, int cols) {
    List<String> columnNames = new ArrayList<>(cols)
    for (int col = 1; col <= cols; col++) {
      columnNames.add("C${col}".toString())
    }
    List<List> columns = new ArrayList<>(cols)
    for (int colIdx = 0; colIdx < cols; colIdx++) {
      List<Integer> values = new ArrayList<>(rows)
      for (int row = 0; row < rows; row++) {
        values.add(row + colIdx)
      }
      columns.add(values)
    }
    MatrixBuilder builder = Matrix.builder()
    builder.columnNames(columnNames)
    builder.columns(columns)
    builder.build()
  }
}
