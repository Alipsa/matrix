package se.alipsa.groovy.matrixcsv

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import se.alipsa.groovy.matrix.Matrix

class CsvExporter {


  static void exportToCsv(Matrix table, CSVFormat format, File out, boolean withHeader = true) {
    exportToCsv(table, format, new PrintWriter(out), withHeader)
  }

  static void exportToCsv(Matrix table, CSVFormat format, Writer out, boolean withHeader = true) {
    exportToCsv(table, format, new PrintWriter(out), withHeader)
  }

  static void exportToCsv(Matrix table, CSVFormat format, PrintWriter out, boolean withHeader = true) {
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
