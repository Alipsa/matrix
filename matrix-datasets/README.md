# Matrix Datasets
Common, public domain and oss datasets in Matrix format for Groovy applications running on JDK 17 or later.

Includes mtcars, iris, PlantGrowth, ToothGrowth, USArrests, diamonds, mpg, and map data

## Setup
Gradle:
```groovy
implementation 'se.alipsa.groovy:matrix-core:2.0.0'
implementation 'se.alipsa.groovy:matrix-datasets:1.0.4'
```
Maven:
```xml
<dependencies>
  <dependency>
      <groupId>se.alipsa.groovy</groupId>
      <artifactId>matrix-core</artifactId>
      <version>2.0.0</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.groovy</groupId>
      <artifactId>matrix-datasets</artifactId>
      <version>1.0.4</version>
  </dependency>
</dependencies>
```

## Usage:
```groovy
import se.alipsa.groovy.datasets.*
import se.alipsa.groovy.matrix.*

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
|           1.0.4 |          2.0.0 |

