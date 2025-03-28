# Tutorial: Getting Started with the Alipsa Matrix Library in Groovy

The Alipsa matrix library is a powerful, open-source Groovy library designed for working with tabular data on the JVM.
It provides a suite of modules to handle data creation, import/export, database interaction, statistical analysis, and
more. This tutorial assumes you have basic knowledge of Groovy and a development environment set up (e.g., IntelliJ
IDEA, Gradle, or Maven). Let's dive in!

## Setup and Installation

### Prerequisites

- Java: JDK 17 or higher.
- Groovy: Version 4.x or higher.
- Build Tool: Gradle or Maven to manage dependencies.
- Setup: Add the dependencies to your build.gradle file (see below).

Here’s a sample build.gradle snippet to include all matrix modules:

```gradle
plugins {
    id 'groovy'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.apache.groovy:groovy:4.0.26'
    implementation platform('se.alipsa.matrix:matrix-bom:2.1.0')
    implementation 'se.alipsa.matrix:matrix-core'
    implementation 'se.alipsa.matrix:matrix-datasets'
    implementation 'se.alipsa.matrix:matrix-sql'
    implementation 'se.alipsa.matrix:matrix-spreadsheet'
    implementation 'se.alipsa.matrix:matrix-json'
    implementation 'se.alipsa.matrix:matrix-csv'
    implementation 'se.alipsa.matrix:matrix-parquet'
    implementation 'se.alipsa.matrix:matrix-bigquery'
    implementation 'se.alipsa.matrix:matrix-xchart'
}
```

Run gradle build to download the dependencies. 
For Maven, the equivalent configuration is:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <groupId>your.group.id</groupId>
  <artifactId>your-artifact-id</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>jar</packaging>
  <properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <modelVersion>4.0.0</modelVersion>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-bom</artifactId>
        <version>2.1.0</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy-all</artifactId>
      <version>4.0.26</version>
      <type>pom</type>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-stats</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-sql</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-datasets</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-spreadsheet</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-json</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-csv</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-parquet</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-bigquery</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-charts</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-xchart</artifactId>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.gmavenplus</groupId>
        <artifactId>gmavenplus-plugin</artifactId>
        <version>4.1.1</version>
        <executions>
          <execution>
            <goals>
              <goal>generateStubs</goal>
              <goal>compile</goal>
              <goal>generateTestStubs</goal>
              <goal>compileTests</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

### Groovy Scripts (Grape) 
you can use @Grab annotations to fetch the jars. For example, at the top of your Groovy script or notebook cell:

```groovy
@Grab(group='se.alipsa.matrix', module='matrix-core', version='3.1.0')
@Grab('se.alipsa.matrix:matrix-stats:2.0.0')
@Grab('se.alipsa.matrix:matrix-datasets:2.0.1')
@Grab('se.alipsa.matrix:matrix-sql:2.0.1')
import se.alipsa.matrix.core.Matrix
// ... (other imports as needed)
```
Note:  Make sure to grab the compatible versions of each module. (Using the BOM with Grape is not straightforward, so list each module explicitly.)

### Jupyter BeakerX Notebook 
If you’re using Groovy in Jupyter via BeakerX, you can add Maven dependencies with a magic command. For example, in a notebook cell:
```html
<code>%classpath add mvn</code> 
<code>se.alipsa.matrix:matrix-core:3.1.0</code>
```

(and similarly for other modules). After that, you can import and use Matrix classes in subsequent cells. You may also use @Grab in BeakerX, but the %classpath magic is provided by BeakerX for this purpose.


Now, let’s explore each module.

## 1. Matrix-Core: Creating and Manipulating Tabular Data
The matrix-core module is the foundation of the library, allowing you to create, manipulate, and work with tabular data.

### Sample Usage
```groovy
import se.alipsa.matrix.core.*

// Create a simple matrix with employee data
def empData = Matrix.builder(
    'empData',
    emp_id: 1..3,
    emp_name: ['Alice', 'Bob', 'Charlie'],
    salary: [50000, 60000, 75000],
    [Integer, String, Number] // Column types
).build()

// Print the matrix
println empData.content()

// Access a specific row
def row = empData.row(1) // Gets Bob's data
println "Row 1: $row"

// Access a column
def names = empData['emp_name']
println "Names: $names"

// Add a new column
empData['bonus'] = [1000, 1500, 2000]
println "Updated matrix with bonus:\n${empData.content()}"
```
### Output:
```
empData: 3 obs * 3 variables 
emp_id	emp_name	salary
     1	Alice   	 50000
     2	Bob     	 60000
     3	Charlie 	 75000

Row 1: [2, Bob, 60000]
Names: [Alice, Bob, Charlie]
Updated matrix with bonus:
empData: 3 obs * 4 variables 
emp_id	emp_name	salary	bonus
     1	Alice   	 50000	 1000
     2	Bob     	 60000	 1500
     3	Charlie 	 75000	 2000
```

## Various ways of creating a Matrix
A Matrix can be created in several ways: 
1. Programmatically using the `Matrix.builder()` method
2. From a CSV file using the `Matrix.builder().data(file).build()` method or more advanced methods using the matrix-csv module
3. From a Qinq query result using the `Matrix.builder().ginqResult(ginqResult).build()` method
4. From a SQL ResultSet using the `Matrix.builder().data(resultSet).build()` method
5. From a spreadsheet (Excel or LibreOffice/OpenOffice) by using the matrix-spreadsheet module.
6. From a JSON file using the matrix-json module
7. From a Parquet file using the matrix-parquet module
8. From a BigQuery table using the matrix-bigquery module

## Rows and Columns
You can access rows and columns in a Matrix using the `row()` and `column()` methods.
```groovy
// Access a specific row
table.row(1) // Gets the second row
// Iterate over all rows
table.each{ row -> println row } 
// Select a subset of rows. This will select the second and third row and return it in a new Matrix
Matrix loosers = table.subset('place', { it > 1 })
```
A row is "aware" of the matrix it came from so it has methods such as `getColumnNames()` and `getColumnIndex()`. You can also use shorthand notation such as `row[0]` or `row['place']` to get the value of a column in the row.

Columns can be directly accessed using the column name or index. For example:
```groovy
def firstNames = table['firstname'] // Access by column name
```
or 
```groovy
def firstNames = table[1] // Access by column index
```
You can also use the `getColumn()` method to get a column by name or index.



## Converting data

You might want to convert some data before the Matrix is created. There are 2 classes that can be used for this:
- `ListConverter`: This class provides static methods for converting lists of data to different types. 
- ValueConverter: This class provides methods for converting a single value to a different type. 

After you have created your Matrix you can use the `convert()` method to convert the columns in the Matrix to different types. 

### Example of converting data using convert
```groovy
import se.alipsa.matrix.core.Matrix
import java.time.LocalDate

def table = Matrix.builder().columns([
    'place'    : ['1', '2', '3'],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start'    : ['2021-12-01', '2022-07-10', '2023-05-27']
]).build()
    
// Convert two columns to different types:
table.convert(place: int, start: LocalDate)
```

If you want to do some calculation of the data and convert it , you can use the apply() method. Using the example above we use the apply() method to add 10 days to the start date and convert it to a LocalDateTime. 
```groovy
table.apply("start") { startDate ->
      startDate.plusDays(10).atStartOfDay()
    }
println table.content()
```
Which will output:
```
place	firstname	start           
    1	Lorena   	2021-12-11T00:00
    2	Marianne 	2022-07-20T00:00
    3	Lotte    	2023-06-06T00:00
```