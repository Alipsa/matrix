# Introduction to the Alipsa Matrix Library

## Overview

The Alipsa Matrix library is a powerful Groovy library designed to make working with tabular (two-dimensional) data easy and intuitive. While primarily developed for Groovy, it also works seamlessly in Java environments. The library provides a comprehensive set of tools for data manipulation, analysis, visualization, and integration with various data sources.

At its core, the Matrix library addresses a common challenge in data processing: working with two-dimensional data structures. In Groovy, you might typically define such structures as nested lists:

```groovy
def myList = [
    [1, 2, 3],
    [3.4, 7.12, 0.19]
]
```

While this approach works, it can become cumbersome when performing complex operations on the data. The Matrix library enhances this experience by providing dedicated classes and methods that make working with such data structures more intuitive and powerful.

## Purpose and Benefits

The Matrix library offers several key benefits for developers working with tabular data:

1. **Simplified Data Handling**: The library provides intuitive ways to create, access, and manipulate tabular data.

2. **Type Safety**: Matrix objects can have defined column types, ensuring data consistency.

3. **Rich Functionality**: Built-in methods for statistical operations, data transformation, filtering, and more.

4. **Integration Capabilities**: Seamless integration with various data sources including databases, CSV files, JSON, Excel spreadsheets, and more.

5. **Visualization Support**: Create charts and visualizations directly from your data.

6. **Familiar Syntax**: Uses notation similar to R and Python data frames, making it accessible to data scientists.

7. **Extensibility**: The modular design allows for easy extension and customization.

Whether you're performing data analysis, building data processing pipelines, or developing data-driven applications, the Matrix library provides a solid foundation for working with tabular data in the JVM ecosystem.

## Installation and Setup

The Matrix library is designed to work with any Groovy 4.x version and requires JDK 21 or higher. You can add the library to your project using your preferred build system.

### Gradle Configuration

For Gradle projects, you can use the Bill of Materials (BOM) to simplify dependency management:

```groovy
implementation(platform('se.alipsa.matrix:matrix-bom:2.2.0'))
implementation('se.alipsa.matrix:matrix-core')
```

If you need additional modules, you can add them similarly:

```groovy
implementation('se.alipsa.matrix:matrix-stats')
implementation('se.alipsa.matrix:matrix-csv')
// Add other modules as needed
```

### Maven Configuration

For Maven projects, add the following to your `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-bom</artifactId>
            <version>2.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-core</artifactId>
    </dependency>
    <!-- Add other modules as needed -->
</dependencies>
```

### Using from Java

If you're using the Matrix library from Java, you'll need to add the Groovy core library as a dependency:

```groovy
// For Gradle
implementation('org.apache.groovy:groovy:5.0.1')
```

```xml
<!-- For Maven -->
<dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Library Structure

The Matrix project consists of multiple modules, each providing specific functionality:

1. **matrix-core**: The heart of the library, containing the Matrix and Grid classes along with utility classes for basic statistics and data conversion.

2. **matrix-stats**: Advanced statistical methods and tests including correlations, normalization, linear regression, t-tests, and more.

3. **matrix-datasets**: Common datasets used in data science, similar to those available in R and Python.

4. **matrix-spreadsheet**: Import and export functionality between Matrix objects and Excel/OpenOffice Calc spreadsheets.

5. **matrix-csv**: Advanced CSV file handling capabilities.

6. **matrix-json**: JSON import and export functionality.

7. **matrix-xchart**: Chart creation using the XCharts library.

8. **matrix-sql**: Database interaction capabilities.

9. **matrix-bom**: Bill of Materials for simplified dependency management.

10. **matrix-parquet**: Parquet file format support (experimental).

11. **matrix-bigquery**: Google BigQuery integration (experimental).

12. **matrix-charts**: Alternative chart creation capabilities (experimental).

13. **matrix-tablesaw**: Interoperability with the Tablesaw library (experimental).

In the following sections, we'll explore each of these modules in detail, starting with the core functionality provided by the matrix-core module.

Go to [previous section](outline.md) | Go to [next section](2-matrix-core.md)

