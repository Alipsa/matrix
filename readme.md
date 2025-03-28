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
1. _[matrix-csv](https://github.com/Alipsa/matrix/blob/main/matrix-csv/README.md)_ provides a more advanced way to import and export between a Matrix and a CSV file using commons-csv(matrix-core has basic support
   for doing this built in)
1. _[matrix-json](https://github.com/Alipsa/matrix/blob/main/matrix-json/README.md)_ provides ways to import and export between a Matrix and Json
1. _[matrix-xcharts](https://github.com/Alipsa/matrix/blob/main/matrix-xcharts/README.md)_ allows you to create charts in various formats (file, svg, swing) based on Matrix data and the [XCharts library](https://github.com/knowm/XChart). 
1. _[matrix-sql](https://github.com/Alipsa/matrix/blob/main/matrix-sql/readme.md)_ relational database interaction
1. _[matrix-bom](https://github.com/Alipsa/matrix/blob/main/matrix-bom/readme.md)_ Bill of materials for simpler dependency management.
1. _[matrix-parquet](https://github.com/Alipsa/matrix/blob/main/matrix-parquet/readme.md)_ provides ways to import and export between Matrix and [Parquet](https://parquet.apache.org/). Experimental
1. _[matrix-bigquery](https://github.com/Alipsa/matrix/blob/main/matrix-bigquery/readme.md)_
   provides ways to import and export between Matrix and [Google Big Query](https://cloud.google.com/bigquery). Experimental
1. _[matrix-charts](https://github.com/Alipsa/matrix/blob/main/matrix-charts/README.md)_ allows you to create charts in various formats (file, javafx, svg) based on Matrix data. Experimental
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
implementation(platform( 'se.alipsa.matrix:matrix-bom:1.1.2'))
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
            <version>1.1.2</version>
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

The jvm should be JDK 21 or higher.

For more information see the readme file in each subproject,
the test classes and the [Cookbook](docs/cookbook/cookbook.md)



