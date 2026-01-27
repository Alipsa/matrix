[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-arff/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-arff)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-arff/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-arff)
Matrix-arff provides reading and writing of ARFF (Attribute-Relation File Format) files. ARFF is a standard format used by machine learning tools, particularly Weka, making this module essential for ML workflows and data interchange between different ML tools.

## What is ARFF?

ARFF (Attribute-Relation File Format) is a text-based format developed for the Weka machine learning toolkit. It's widely used in the machine learning community because it:

- Explicitly defines data types for each column
- Supports both numeric and categorical (nominal) data
- Includes metadata about the dataset (relation name, attribute definitions)
- Is human-readable and easy to inspect

An ARFF file consists of three main sections:

1. **Header comments** - Lines starting with `%` containing metadata
2. **Relation and attribute declarations** - `@RELATION` and `@ATTRIBUTE` directives
3. **Data section** - `@DATA` followed by comma-separated values

## Installation

To use the matrix-arff module, add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.4'
implementation platform("se.alipsa.matrix:matrix-bom:2.4.0")
implementation "se.alipsa.matrix:matrix-core"
implementation "se.alipsa.matrix:matrix-arff"
```

### Maven Configuration

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>5.0.3</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-core</artifactId>
        <version>3.5.0</version>
    </dependency>
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-arff</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

## Reading ARFF Files

The `MatrixArffReader` class provides several methods to read ARFF files from different sources.

### Reading from a File

```groovy
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.core.Matrix

// Read the classic iris dataset
Matrix iris = MatrixArffReader.read(new File("iris.arff"))

println "Dataset: ${iris.matrixName}"
println "Rows: ${iris.rowCount()}"
println "Columns: ${iris.columnNames()}"
```

Output:
```
Dataset: iris
Rows: 150
Columns: [sepallength, sepalwidth, petallength, petalwidth, class]
```

See [the tutorial](../docs/tutorial/16-matrix-arff.md) for more details.