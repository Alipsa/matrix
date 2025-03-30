# Alipsa Matrix Library Tutorial - Outline

## 1. [Introduction](1-introduction.md)
- Overview of the Alipsa Matrix library
- Purpose and benefits of using Matrix for tabular data
- Installation and setup instructions
  - Gradle configuration
  - Maven configuration
  - JDK requirements

## 2. [Matrix Core Module](2-matrix-core.md)
- Understanding the Matrix class
  - What is a Matrix?
  - Matrix vs. Grid
  - Creating a Matrix
    - From Groovy code
    - From a result set
    - From a CSV file
  - Accessing data in a Matrix
    - Using [] notation
    - Using column names
    - Using row indices
- Data manipulation
  - Converting data types
  - Transforming data
  - Subsetting data
- Performing calculations
  - Using the apply method
  - Column arithmetics
  - Statistical operations with Stat class
- Using Groovy Integrated queries (Ginq)
- Using Matrix from other JVM languages (Java)
- The Grid class
  - Creating a Grid
  - Grid operations
  - Differences from Matrix

## 3. [Matrix Stats Module](3-matrix-stats.md)
- Statistical methods and tests
- Correlations
- Normalization
- Linear regression
- T-test
- Other statistical functions

## 4. [Matrix Datasets Module](4-matrix-datasets.md)
- Overview of included datasets
- Using common datasets
  - mtcars
  - iris
  - diamonds
  - plantgrowth
  - toothgrowth
- Creating your own datasets

## 5. [Matrix Spreadsheet Module](5-matrix-spreadsheet.md)
- Importing data from Excel/OpenOffice
- Exporting Matrix to spreadsheets
- Working with multiple sheets

## 6. [Matrix CSV Module](6-matrix-csv.md)
- Advanced CSV import/export
- Customizing CSV parsing
- Handling different CSV formats

## 7. [Matrix JSON Module](7-matrix-json.md)
- Converting between Matrix and JSON
- JSON import/export options
- Working with nested JSON structures

## 8. [Matrix XChart Module](8-matrix-xchart.md)
- Creating charts with XCharts library
- Chart types and options
- Customizing chart appearance
- Exporting charts to different formats

## 9. [Matrix SQL Module](9-matrix-sql.md)
- Database interaction
- Querying databases
- Converting result sets to Matrix
- Performing operations on database data

## 10. [Matrix BOM Module](10-matrix-bom.md)
- Bill of materials for dependency management
- Simplifying dependency configuration
- Version management

## 11. Experimental (early development) Modules
- [Matrix Parquet Module](11-matrix-parquet.md)
  - Working with Parquet files
- [Matrix BigQuery Module](12-matrix-bigquery.md)
  - Google BigQuery integration
- [Matrix Charts Module](13-matrix-charts.md)
  - Alternative charting capabilities
- [Matrix Tablesaw Module](14-matrix-tablesaw.md)
  - Interoperability with Tablesaw library

## 12. Practical Examples
- [Data analysis workflow](15-analysis-workflow.md)
<!--
- Combining multiple modules
- Real-world use cases

## 13. Best Practices and Tips
- Performance considerations
- Memory management
- Code organization
- Common pitfalls and how to avoid them

## 14. Conclusion
- Summary of key concepts
- Additional resources
- Community and support
-->