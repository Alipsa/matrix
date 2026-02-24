![Groovy](https://img.shields.io/badge/groovy-4298B8.svg?style=for-the-badge&logo=apachegroovy&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
# Matrix
This is a Groovy library (that also works in Java) to make it easy to work with
a matrix (tabular i.e. 2-dimensional) data. Whenever you ḧave a structure like this
List<List<?>> (typically defined in 
Groovy like this `def myList = [ [1,2,3], [3.4, 7.12, 0.19] ]`) a Matrix or
a Grid can greatly enhance the experience of working with that data structure.

The Matrix project consist of the following modules:
1. _[matrix-core](https://github.com/Alipsa/matrix/blob/main/matrix-core/readme.md)_ The matrix-core is the heart of the matrix project. It
   contains the Matrix and Grid classes as well as several utility classes to
   do basic statistics (sum, mean, median, sd, variance, counts, frequency etc.) and to
   convert data into various shapes and formats
   See [tests](https://github.com/Alipsa/matrix/blob/main/matrix-core/src/test/groovy/MatrixTest.groovy) for more usage examples or
   the [javadocs](https://javadoc.io/doc/se.alipsa.matrix/matrix-core/latest/index.html) for more info.
1. _[matrix-stats](https://github.com/Alipsa/matrix/blob/main/matrix-stats/README.md)_ The stats library contains various statistical methods and tests
   (correlations, normalization, linear regression, t-test, etc.)
1. _[matrix-datasets](https://github.com/Alipsa/matrix/blob/main/matrix-datasets/README.md)_ contains some common datasets used in R and Python such as mtcars, iris, diamonds, plantgrowth, toothgrowth etc.
1. _[matrix-spreadsheet](https://github.com/Alipsa/matrix/blob/main/matrix-spreadsheet/README.md)_ provides ways to import and export between a Matrix and an Excel or OpenOffice Calc spreadsheet
1. _[matrix-gsheets](https://github.com/Alipsa/matrix/blob/main/matrix-gsheets/readme.md)_ provides ways to import and export between a Matrix and a Google Sheets spreadsheet
1. _[matrix-csv](https://github.com/Alipsa/matrix/blob/main/matrix-csv/README.md)_ provides a more advanced way to import and export between a Matrix and a CSV file using commons-csv(matrix-core has basic support
   for doing this built in)
1. _[matrix-arff](https://github.com/Alipsa/matrix/blob/main/matrix-arff/README.md)_ provides reading and writing of ARFF (Attribute-Relation File Format) files.
1. _[matrix-json](https://github.com/Alipsa/matrix/blob/main/matrix-json/README.md)_ provides ways to import and export between a Matrix and Json
1. _[matrix-charts](https://github.com/Alipsa/matrix/blob/main/matrix-charts/README.md)_ provides a grammar of graphics based groovy api for chart rendering based on the Charm rendering engine and also a familiar chart-type-first API for creating charts in various formats (file, javafx, svg) based on Matrix data.
1. _[matrix-ggplot](https://github.com/Alipsa/matrix/blob/main/matrix-ggplot/README.md)_ provides a ggplot2-style charting API (very close to the ggplot2 library in R). Delegates to the Charm engine in matrix-charts.
1. _[matrix-xcharts](https://github.com/Alipsa/matrix/blob/main/matrix-xcharts/README.md)_ allows you to create charts in various formats (file, svg, swing) based on Matrix data and the [XCharts library](https://github.com/knowm/XChart). 
1. _[matrix-sql](https://github.com/Alipsa/matrix/blob/main/matrix-sql/readme.md)_ relational database interaction
1. _[matrix-bom](https://github.com/Alipsa/matrix/blob/main/matrix-bom/readme.md)_ Bill of materials for simpler dependency management.
1. _[matrix-parquet](https://github.com/Alipsa/matrix/blob/main/matrix-parquet/readme.md)_ provides ways to import and export between Matrix and [Parquet](https://parquet.apache.org/). 
1. _[matrix-avro](https://github.com/Alipsa/matrix/blob/main/matrix-avro/README.md)_ provides ways to import and export between Matrix and [Avro](https://avro.apache.org/). 
2. _[matrix-bigquery](https://github.com/Alipsa/matrix/blob/main/matrix-bigquery/readme.md)_
   provides ways to import and export between Matrix and [Google Big Query](https://cloud.google.com/bigquery).
1. _[matrix-smile](https://github.com/Alipsa/matrix/blob/main/matrix-smile/README.md)_ Integration between Matrix and the Smile library (Statistical Machine Intelligence and Learning Engine).
1. _[matrix-tablesaw](https://github.com/Alipsa/matrix/blob/main/matrix-tablesaw/readme.md)_ interoperability between Matrix and the [Tablesaw](https://github.com/jtablesaw/tablesaw) library. Experimental

## Setup
Matrix should work with any 4.x version of groovy. Binary builds can be downloaded 
from the [Matrix project release page](https://github.com/Alipsa/matrix/releases) but if you use a build system that 
handles dependencies via maven central (gradle, maven ivy etc.) you can add your dependencies from there
. The group name is se.alipsa.matrix. 
The version numbers of the matrix modules does not align with each other so a way to handle this in a simpler way is to 
use the bom file.

An example for matrix-core is as follows for Gradle
```groovy
implementation(platform( 'se.alipsa.matrix:matrix-bom:2.4.0'))
implementation('se.alipsa.matrix:matrix-core')
```
...or the following for maven
```xml
<project>
   ...
   <dependencyManagement>
      <dependencies>
         <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-bom</artifactId>
            <version>2.4.0</version>
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
   </dependencies>
   ...
</project>
```

## Java Version Requirements

The project requires **JDK 21**. While some modules may work with higher JDK versions, the following constraints apply:

| Module(s)                   | Constraint | Reason                                                                                |
|-----------------------------|------------|---------------------------------------------------------------------------------------|
| matrix-parquet, matrix-avro | JDK 21 max | Hadoop 3.4.x dependencies do not support JDK 22+                                      |
| matrix-charts               | JDK 21 max | JavaFX 23.x is the latest version compatible with JDK 21; JavaFX 24+ requires JDK 22+ |
| matrix-smile                | JDK 21 min | Smile 4.x is used (requires at least java 21; Smile 5+ requires Java 25)              |

Since Matrix is a library, it makes sense to stay on java 21 to ensure compatibility across all modules for the foreseeable future.
These constraints are enforced in `build.gradle` via dependency version ceiling rules.

For more information see the [tutorial](docs/tutorial/outline.md) and the readme file and test classes in each subproject.
<!---
[Cookbook](docs/cookbook/cookbook.md)
-->


