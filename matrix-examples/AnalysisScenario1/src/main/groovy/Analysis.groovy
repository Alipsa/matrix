/**************************************************************
 * Complete Data Analysis Example using Alipsa Matrix Library *
 *                                                            *
 * This script demonstrates a full data analysis workflow:    *
 * 1. Importing data                                          *
 * 2. Exploring data                                          *
 * 3. Cleaning data                                           *
 * 4. Analyzing data                                          *
 * 5. Visualizing data                                        *
 *************************************************************/

import se.alipsa.matrix.core.*
import se.alipsa.matrix.csv.*
import se.alipsa.matrix.stats.*
import se.alipsa.matrix.stats.regression.LinearRegression
import se.alipsa.matrix.xchart.*
import java.time.LocalDate
import java.text.NumberFormat
import org.apache.commons.csv.CSVFormat

import java.time.YearMonth

import static se.alipsa.matrix.core.ListConverter.toDates
import static se.alipsa.matrix.core.ValueConverter.*

// Create a directory to store our results
File analysisResults = new File("analysis_results")
analysisResults.mkdirs()

println "=== COMPLETE DATA ANALYSIS EXAMPLE USING ALIPSA MATRIX LIBRARY ==="
println "=================================================================="

// ============================================================
// PART 1: IMPORTING DATA
// ============================================================
println "\n=== PART 1: IMPORTING DATA ==="

// For this example, we'll create a synthetic dataset representing sales data
// In a real scenario, you would import from CSV, database, or other sources
def salesData = Matrix.builder().data(
    date: [
        "2023-01-15", "2023-01-22", "2023-02-05", "2023-02-19",
        "2023-03-10", "2023-03-25", "2023-04-08", "2023-04-22",
        "2023-05-06", "2023-05-20", "2023-06-03", "2023-06-17",
        "2023-07-01", "2023-07-15", "2023-07-29", "2023-08-12",
        "2023-08-26", "2023-09-09", "2023-09-23", "2023-10-07"
    ],
    product: [
        "Laptop", "Phone", "Tablet", "Laptop",
        "Phone", "Tablet", "Laptop", "Phone",
        "Tablet", "Laptop", "Phone", "Tablet",
        "Laptop", "Phone", "Tablet", "Laptop",
        "Phone", "Tablet", "Laptop", "Phone"
    ],
    region: [
        "North", "South", "East", "West",
        "North", "South", "East", "West",
        "North", "South", "East", "West",
        "North", "South", "East", "West",
        "North", "South", "East", "West"
    ],
    units_sold: [
        45, 120, 35, 50,
        135, 40, 55, 140,
        42, 48, 125, 38,
        52, 130, 45, 60,
        145, 50, 65, 150
    ],
    revenue: [
        45000, 60000, 17500, 50000,
        67500, 20000, 55000, 70000,
        21000, 48000, 62500, 19000,
        52000, 65000, 22500, 60000,
        72500, 25000, 65000, 75000
    ],
    customer_satisfaction: [
        4.2, 4.5, 3.8, 4.3,
        4.6, 3.9, 4.4, 4.7,
        4.0, 4.1, 4.4, 3.7,
        4.5, 4.8, 4.1, 4.6,
        4.9, 4.2, 4.7, 4.8
    ],
    returns: [
        3, 5, 2, 4,
        6, 3, 2, 7,
        2, 3, 5, 4,
        2, 4, 3, 5,
        7, 2, 3, 6
    ]
).types(String, String, String, Integer, Double, Double, Integer)
    .build()

// Let's also save this data to a CSV file for demonstration
def outputFile = new File(analysisResults, "sales_data.csv")
CsvExporter.exportToCsv(salesData, outputFile)

// Now let's read it back to demonstrate CSV import
def importedData = CsvImporter.importCsv(new File(analysisResults,"sales_data.csv"), CSVFormat.DEFAULT)

println "Data imported successfully with ${importedData.rowCount()} rows and ${importedData.columnCount()} columns."
println "Column names: ${importedData.columnNames()}"
println "Column types: ${importedData.types()}"

// ============================================================
// PART 2: EXPLORING DATA
// ============================================================
println "\n=== PART 2: EXPLORING DATA ==="

// Let's convert the date strings to actual dates for better analysis
salesData.convert("date": LocalDate, "yyyy-MM-dd")

// Display the first few rows
println "\nFirst 5 rows of data:"
println salesData.head(5)

// Get basic statistics for numeric columns
println "\nBasic statistics for units_sold:"
println Stat.summary(salesData)

// Count unique values in categorical columns
println "\nUnique products:"
def uniqueProducts = salesData["product"].unique()
println uniqueProducts

println "\nUnique regions:"
def uniqueRegions = salesData["region"].unique()
println uniqueRegions

// Distribution of sales by product
println "\nSales distribution by product:"
def productSales = Stat.sumBy(salesData, "units_sold", "product")
println productSales.content()

// Distribution of sales by region
println "\nSales distribution by region:"
def regionSales = Stat.sumBy(salesData,"units_sold", "region")
println regionSales.content()

// ============================================================
// PART 3: CLEANING DATA
// ============================================================
println "\n=== PART 3: CLEANING DATA ==="

// For demonstration, let's introduce some "dirty" data
println "Introducing some missing and outlier values for demonstration..."

// Create a copy with some "dirty" data
def dirtyData = salesData.clone()

// Introduce some missing values
dirtyData[3, "units_sold"] = null
dirtyData[7, "revenue"] = null
dirtyData[12, "customer_satisfaction"] = null

// Introduce some outliers
dirtyData[5, "units_sold"] = 500  // Unusually high
dirtyData[9, "revenue"] = 500000  // Unusually high
dirtyData[15, "returns"] = 50     // Unusually high

println "\nDirty data preview (with missing values and outliers):"
println dirtyData.head(5)

// 1. Check for missing values
println "\nChecking for missing values:"
Map<String, Number> missingValues = [:]
dirtyData.columnNames().each { colName ->
  def nullCount = dirtyData.column(colName).count { it == null }
  missingValues[colName] = nullCount
}
println missingValues

// 2. Handle missing values
println "\nHandling missing values..."
Matrix cleanedData = dirtyData.clone()

// Fill missing numeric values with column means
def numericColumns = missingValues.findAll{ k,v -> v > 0}.keySet()
numericColumns.each { String colName ->
  def mean = Stat.mean(cleanedData[colName])
  //def mean = colValues.findAll { it != null }.sum() / colValues.count { it != null }

  for (int i = 0; i < cleanedData.rowCount(); i++) {
    if (cleanedData[i, colName] == null) {
      cleanedData[i, colName] = mean
      println "Replaced missing value in $colName at row $i with mean: $mean"
    }
  }
}

// 3. Detect and handle outliers using Z-score method
println "\nDetecting and handling outliers..."
numericColumns.each { colName ->
  def values = cleanedData[colName]
  Number mean = values.mean()
  Number sd = values.sd()

  // Consider values with Z-score > 3 as outliers
  for (int i = 0; i < cleanedData.rowCount(); i++) {
    def value = cleanedData[i, colName]
    def zScore = (value - mean).abs() / sd

    if (zScore > 3) {
      println "Detected outlier in $colName at row $i: $value (Z-score: $zScore)"
      // Replace with median (more robust than mean for outliers)
      cleanedData[i, colName] = values.median()
      println "Replaced with median: ${values.median()}"
    }
  }
}

println "\nData cleaning completed."
println "Original data shape: ${salesData.rowCount()} rows, ${salesData.columnCount()} columns"
println "Cleaned data shape: ${cleanedData.rowCount()} rows, ${cleanedData.columnCount()} columns"

// ============================================================
// PART 4: ANALYZING DATA
// ============================================================
println "\n=== PART 4: ANALYZING DATA ==="

// Let's use the original clean data for analysis
Matrix analysisData = salesData.clone()

// 1. Calculate average revenue per unit by product
println "\nAverage revenue per unit by product:"
Matrix avgRevenueByProduct = Matrix.builder().data(
    product: uniqueProducts,
    total_units: [],
    total_revenue: [],
    avg_revenue_per_unit: []
).types(String, Integer, Double, Double)
    .build()

uniqueProducts.eachWithIndex { product, idx ->
  def productRows = analysisData.subset("product", product)
  def totalUnits = productRows["units_sold"].sum()
  def totalRevenue = productRows["revenue"].sum()
  def avgRevenuePerUnit = totalRevenue / totalUnits

  avgRevenueByProduct[idx, "total_units"] = totalUnits
  avgRevenueByProduct[idx, "total_revenue"] = totalRevenue
  avgRevenueByProduct[idx, "avg_revenue_per_unit"] = avgRevenuePerUnit
}

println avgRevenueByProduct.content()

// 2. Calculate correlation between units_sold and customer_satisfaction
println "\nCorrelation analysis:"
def unitsSold = analysisData["units_sold"]
def satisfaction = analysisData["customer_satisfaction"]
def correlation = Correlation.cor(unitsSold, satisfaction)
println "Correlation between units_sold and customer_satisfaction: $correlation"

// 3. Time series analysis - monthly sales trend
println "\nMonthly sales trend:"
def monthlySales = [:]
analysisData.rows().each { row ->
  def yearMonth = asYearMonth(row.date)
  monthlySales[yearMonth] = row.units_sold
}

def sortedMonthlySales = monthlySales.sort { it.key }
sortedMonthlySales.each {
  println "${it.key}: ${it.value}"
}

// 4. Perform hypothesis testing - is there a significant difference in sales between regions?
println "\nHypothesis testing - sales difference between regions:"
Map<String, List> regionData = [:]
uniqueRegions.each { region ->
  regionData[region] = analysisData.subset("region", region)["units_sold"]
}

// Perform ANOVA test
def anovaResult = Anova.aov(regionData)
println "ANOVA p-value: ${anovaResult.pValue.round(3)}"
println "Is there a significant difference between regions? ${anovaResult.evaluate(0.05) ? 'Yes' : 'No'}"

// 5. Create a simple predictive model - linear regression for revenue based on units_sold
println "\nSimple linear regression model:"
def regression = new LinearRegression(analysisData, "units_sold", "revenue")

println "Regression equation: revenue = ${regression.intercept} + ${regression.slope} * units_sold"
println "R-squared: ${regression.r2.round(3)}"

// Make a prediction
def newUnitsSold = 100
def predictedRevenue = regression.predict(newUnitsSold)
NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US)
println "Predicted revenue for $newUnitsSold units: ${formatter.format(predictedRevenue)}"

// ============================================================
// PART 5: VISUALIZING DATA
// ============================================================
println "\n=== PART 5: VISUALIZING DATA ==="

// 1. Bar chart - Sales by Product
BarChart salesByProductChart = BarChart.create(avgRevenueByProduct, 800, 600)
    .setTitle("Total Units Sold by Product")
    .addSeries("Total Units Sold", avgRevenueByProduct["product"], avgRevenueByProduct["total_units"])
    .setXLabel("Product")
    .setYLabel("Total Units Sold")
salesByProductChart.exportPng(new File(analysisResults, "sales_by_product.png"))
println "Created bar chart: analysis_results/sales_by_product.png"

// 2. Bar chart - Sales by Region
def salesByRegionMatrix = Matrix.builder().data(
    region: uniqueRegions,
    total_units: []
).types(String, Integer)
    .build()

uniqueRegions.eachWithIndex { region, idx ->
  def regionRows = analysisData.subset("region", region)
  def totalUnits = regionRows.column("units_sold").sum()
  salesByRegionMatrix[idx, "total_units"] = totalUnits
}

def salesByRegionChart = BarChart.create(salesByRegionMatrix, 800, 600)
    .setTitle("Total Units Sold by Region")
    .addSeries("Total Units Sold by Region", "region", "total_units")

File regionChartFile = new File(analysisResults, "sales_by_region.png")
salesByRegionChart.exportPng(regionChartFile)
println "Created bar chart: $regionChartFile"

// 3. Scatter plot - Units Sold vs Revenue
def scatterChart = ScatterChart.create(
    "Units Sold vs Revenue",
    analysisData,
    "units_sold",
    "revenue",
    800, 600
)
scatterChart.exportPng(new File(analysisResults,"units_vs_revenue.png"))
println "Created scatter plot: analysis_results/units_vs_revenue.png"

// 4. Line chart - Monthly Sales Trend
def monthlyTrendMatrix = Matrix.builder().data(
    month: toDates(sortedMonthlySales.keySet()),
    sales: sortedMonthlySales.values().toList()
).types(YearMonth, Integer)
    .build()

def lineChart = LineChart.create(monthlyTrendMatrix, 800, 600)
    .setTitle("Monthly Sales Trend")
    .addSeries("Monthly Sales", "month", "sales")

File lineChartFile = new File(analysisResults, "monthly_trend.png")
lineChart.exportPng(lineChartFile)
println "Created line chart: $lineChartFile"

// 5. Pie chart - Product Distribution
def pieChart = PieChart.create(avgRevenueByProduct, 800, 600)
    .setTitle("Product Sales Distribution")
    .addSeries("product", "total_units")

File pieChartFile = new File(analysisResults, "product_distribution.png")
pieChart.exportPng(pieChartFile)
println "Created pie chart: $pieChartFile"

// ============================================================
// SUMMARY
// ============================================================
println "\n=== ANALYSIS SUMMARY ==="
println "1. We imported sales data with information about products, regions, units sold, revenue, etc."
println "2. We explored the data to understand its structure and basic statistics."
println "3. We cleaned the data by handling missing values and outliers."
println "4. We performed various analyses including:"
println "   - Average revenue per unit by product"
println "   - Correlation analysis"
println "   - Time series analysis of monthly sales"
println "   - Hypothesis testing for regional differences"
println "   - Simple linear regression model"
println "5. We created visualizations to better understand the data:"
println "   - Bar charts for product and regional sales"
println "   - Scatter plot for units sold vs revenue"
println "   - Line chart for monthly sales trend"
println "   - Pie chart for product distribution"
println "\nAll results and visualizations have been saved to the $analysisResults.absolutePath directory."

