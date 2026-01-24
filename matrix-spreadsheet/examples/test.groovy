import se.alipsa.matrix.spreadsheet.*
import Matrix
import static ListConverter.*

// Create or obtain Matrix objects
Matrix salesByMonth = Matrix.builder().data(
    month: ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
    revenue: [12500, 13200, 15400, 14800, 16700, 18200]
).build()

Matrix salesDetails = Matrix.builder().data(
    date: se.alipsa.matrix.core.ListConverter.toLocalDates('2023-01-15', '2023-02-20', '2023-03-10', '2023-04-05', '2023-05-12', '2023-06-08'),
    product: ["Widget A", "Widget B", "Widget A", "Widget C", "Widget B", "Widget A"],
    units: [120, 85, 150, 95, 110, 180],
    revenue: [4800, 5100, 6000, 5700, 6600, 7200]
).build()

// Export both matrices to a single spreadsheet with multiple sheets
SpreadsheetWriter.writeSheets(
    file: new File(System.getProperty("user.dir"), "sales_report.xlsx"),
    data: [salesByMonth, salesDetails],
    sheetNames: ['Monthly Summary', 'Sales Details']
)
