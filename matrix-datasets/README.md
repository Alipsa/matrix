[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-datasets/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-datasets)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-datasets/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-datasets)
# Matrix Datasets
Common, public domain and oss datasets in Matrix format for Groovy applications running on JDK 17 or later.

Includes mtcars, iris, PlantGrowth, ToothGrowth, USArrests, diamonds, mpg, and map data

## Setup
Gradle:
```groovy
implementation 'org.apache.groovy:groovy:4.0.27'
implementation 'org.apache.groovy:groovy-ant:4.0.27'
implementation 'se.alipsa.matrix:matrix-core:3.3.0'
implementation 'se.alipsa.matrix:matrix-datasets:2.1.0'
```
Maven:
```xml
<dependencies>
  <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>4.0.27</version>
  </dependency>
  <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy-ant</artifactId>
      <version>4.0.27</version>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
      <version>3.3.0</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-datasets</artifactId>
      <version>2.1.0</version>
  </dependency>
</dependencies>
```

## Usage:
```groovy
import se.alipsa.matrix.datasets.*
import se.alipsa.matrix.core.*

Matrix iris = Dataset.iris()
Matrix speciesMeans = Stat.meanBy(iris, 'Sepal Length','Species')
println speciesMeans.content()
```
| Species         | Sepal Length |
|-----------------|--------------|
| Iris-versicolor | 5.936        |
| Iris-virginica	 | 6.588        |
| Iris-setosa	    | 5.006        |

# Release version compatibility matrix
The following table illustrates the version compatibility of the matrix datasets and matrix core

| Matrix datasets |    Matrix core | 
|----------------:|---------------:|
|           1.0.3 | 1.2.3 -> 1.2.4 |
|           1.0.4 | 2.0.0 -> 2.1.1 |
|           1.1.0 | 2.2.0 -> 2.2.1 |
|           2.0.0 |          3.0.0 |


