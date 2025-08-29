package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import se.alipsa.matrix.core.Matrix

@CompileStatic
class CsvExporter {

  static void exportToCsv(Matrix table, File out, boolean withHeader = true) {
    exportToCsv(table, CSVFormat.DEFAULT, new PrintWriter(out), withHeader)
  }

  static void exportToCsv(Matrix table, CSVFormat format, File out, boolean withHeader = true) {
    exportToCsv(table, format, new PrintWriter(out), withHeader)
  }

  static void exportToCsv(Matrix table, CSVFormat format = CSVFormat.DEFAULT, Writer out, boolean withHeader = true) {
    exportToCsv(table, format, new PrintWriter(out), withHeader)
  }

  static void exportToCsv(Matrix table, CSVFormat format = CSVFormat.DEFAULT, PrintWriter out, boolean withHeader = true) {
    try(CSVPrinter printer = new CSVPrinter(out, format)) {
      if (format.header != null && format.header.size() == table.columnCount()) {
        printer.printRecord(format.header)
      } else if (withHeader) {
        if (table.columnNames() != null) {
          printer.printRecord(table.columnNames())
        } else {
          printer.printRecord((1..table.columnCount()).collect{ 'c' + it})
        }
      }
      printer.printRecords(table.rows())
    }
  }

  static void exportToCsv(Matrix table, CSVPrinter printer, boolean withHeader = true) {
    if(withHeader) {
      printer.printRecord(table.columnNames())
    }
    printer.printRecords(table.rows())
  }
}
