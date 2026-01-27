[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-json/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-json)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-json/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-json)
# matrix-json
Json import and export functionality to and from a Matrix or Grid

## Setup
Matrix-json should work with any 4.x version of groovy, and probably older versions as well. 
It requires version 2.0.0 or later of the Matrix-core package (se.alipsa.groovy:matrix-core:3.5.0)
Binary builds can be downloaded
from the [Matrix-json project release page](https://github.com/Alipsa/matrix-json/releases) but if you use a build system that
handles dependencies via maven central (gradle, maven ivy etc.) you can do the following for Gradle
```groovy
def groovyVersion = '5.0.2' // any 4.x version should work too
implementation "org.apache.groovy:groovy:$groovyVersion"
implementation "org.apache.groovy:groovy-json:$groovyVersion"
implementation 'se.alipsa.matrix:matrix-core:3.5.0'
implementation 'se.alipsa.matrix:matrix-json:2.1.0'
```
...and the following for maven
```xml
<dependencies>
  <dependency>
    <groupId>org.apache-groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>5.0.2</version>
  </dependency>
  <dependency>
    <groupId>org.apache-groovy</groupId>
    <artifactId>groovy-json</artifactId>
    <version>5.0.2</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
      <version>3.5.0</version>
  </dependency>  
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-json</artifactId>
      <version>2.1.0</version>
  </dependency>
</dependencies>
```

The jvm should be JDK 21 or higher.

## Exporting a Matrix to Json

The simplest way is to just use the toJson method on th JsonExporter, e.g:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonExporter
import java.time.LocalDate
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import groovy.json.JsonOutput

def empData = Matrix.builder().data(
        emp_id: 1..3,
        emp_name: ["Rick","Dan","Michelle"],
        salary: [623.3,515.2,611.0],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
      .types(int, String, Number, LocalDate)
      .build()

def exporter = new JsonExporter(empData)
println JsonOutput.prettyPrint(exporter.toJson())
```
will output
```json
[
    {
        "emp_id": 1,
        "emp_name": "Rick",
        "salary": 623.3,
        "start_date": "2012-01-01"
    },
    {
        "emp_id": 2,
        "emp_name": "Dan",
        "salary": 515.2,
        "start_date": "2013-09-23"
    },
    {
        "emp_id": 3,
        "emp_name": "Michelle",
        "salary": 611.0,
        "start_date": "2014-11-15"
    }
]
```

Sometimes you need to convert the Matrix data in some way. You can do that by supplying a Map of 
Closures for each column name that should be treated. E.g:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonExporter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import groovy.json.JsonOutput

DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern('yy/dd/MM')
def json = exporter.toJson([
        'salary': {it * 10 + ' kr'}, 
        'start_date': {dateTimeFormatter.format(it)}
])
println JsonOutput.prettyPrint(json)
```

which will result in the following
```json
[
    {
        "emp_id": 1,
        "emp_name": "Rick",
        "salary": "6233.0 kr",
        "start_date": "12/01/01"
    },
    {
        "emp_id": 2,
        "emp_name": "Dan",
        "salary": "5152.0 kr",
        "start_date": "13/23/09"
    },
    {
        "emp_id": 3,
        "emp_name": "Michelle",
        "salary": "6110.0 kr",
        "start_date": "14/15/11"
    }
]
```

## Importing json into a Matrix

The json needs to be in the format of a list `[]` with each row represented as an Object `{}`

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.*
import java.time.LocalDate
import static se.alipsa.matrix.core.ListConverter.toLocalDates

def importer = new JsonImporter()
def table = importer.parse('''[
        {
          "emp_id": 1,
          "emp_name": "Rick",
          "salary": 623.3,
          "start_date": "2012-01-01"
        },
        {
          "emp_id": 2,
          "emp_name": "Dan",
          "salary": 515.2,
          "start_date": "2013-09-23"
        },
        {
          "emp_id": 3,
          "emp_name": "Michelle",
          "salary": 611.0,
          "start_date": "2014-11-15"
        }
    ]''').convert([int, String, Number, LocalDate])
// Note: there are other conversion methods that can handle more complex scenarios

// the above will give you exactly this:
Matrix empData = Matrix.builder().data(
    emp_id: 1..3,
    emp_name: ["Rick","Dan","Michelle"],
    salary: [623.3,515.2,611.0],
    start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
    .types(int, String, Number, LocalDate)
    .build()
```

# Release version compatibility matrix
The following table illustrates the version compatibility of the matrix-csv and matrix core

| Matrix json |    Matrix core | 
|------------:|---------------:|
|       1.0.0 | 2.0.0 -> 2.1.1 |
|       1.1.0 | 2.2.0 -> 2.2.1 |
|       2.0.0 |          3.0.0 |
|       2.1.0 | 3.1.0 -> 3.3.0 |