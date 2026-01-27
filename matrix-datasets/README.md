[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-datasets/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-datasets)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-datasets/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-datasets)
# Matrix Datasets
Common, public domain and oss datasets in Matrix format for Groovy applications running on JDK 17 or later.

Includes mtcars, iris, PlantGrowth, ToothGrowth, USArrests, diamonds, mpg, and map data

## Setup
Gradle:
```groovy
implementation 'org.apache.groovy:groovy:5.0.2'
implementation 'org.apache.groovy:groovy-ginq:5.0.2'
implementation 'se.alipsa.matrix:matrix-core:3.5.0'
implementation 'se.alipsa.matrix:matrix-datasets:2.1.1'
```
Maven:
```xml
<dependencies>
  <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>5.0.2</version>
  </dependency>
  <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy-ginq</artifactId>
      <version>5.0.2</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
      <version>3.5.0</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-datasets</artifactId>
      <version>2.1.1</version>
  </dependency>
</dependencies>
```

## Usage:
```groovy
import se.alipsa.matrix.datasets.*
import se.alipsa.matrix.core.*

Matrix iris = Dataset.iris()
Matrix speciesMeans = Stat.meanBy(iris, 'Sepal Length', 'Species')
println speciesMeans.content()
```
| Species         | Sepal Length |
|-----------------|--------------|
| Iris-versicolor | 5.936        |
| Iris-virginica	 | 6.588        |
| Iris-setosa	    | 5.006        |

## Datasets included
The following datasets are included in the matrix-datasets module through the Dataset class:

| Dataset     | Description                                                                                                           | Method                              |
|-------------|-----------------------------------------------------------------------------------------------------------------------|-------------------------------------|
| airquality  | Daily air quality measurements in New York, 1973-1974. 153 obs of 6 vars.                                             | Dataset.airquality()                |
| cars        | The data give the speed of cars and the distances taken to stop. 50 obs of 2 vars                                     | Dataset.cars()                      |
| mtcars      | Motor Trend Car Road Tests. 32 obs of 11 vars.                                                                        | Dataset.mtcars()                    |
| diamonds    | Prices of over 50,000 round cut diamonds. 53940 obs of 10 vars.                                                       | Dataset.diamonds()                  |
| iris        | Measurements of iris flowers. 150 obs of 5 vars.                                                                      | Dataset.iris()                      |
| PlantGrowth | Results from an experiment on the effect of different treatments on plant growth. 30 obs of 3 vars.                   | Dataset.plantGrowth()               |
| ToothGrowth | The effect of vitamin C on tooth growth in guinea pigs. 60 obs of 4 vars.                                             | Dataset.toothGrowth()               |
| USArrests   | Statistics on arrests per 100,000 residents for assault, murder, and rape in each of the 50 US states in 1973         | Dataset.usArrests()                 |
| mpg         | The mpg (miles per gallon) dataset includes information about the fuel economy of popular car models in 1999 and 2008 | Dataset.mpg()                       |
| mapdata     | Geographical data in 6 variables for various regions                                                                  | mapData(datasetName, region, exact) |

### The Rdatasets
Matrix-datasets provides easy access to the [R datasets repository](https://vincentarelbundock.github.io/Rdatasets/). Rdatasets is a collection of 2536 datasets which were originally distributed alongside the statistical software environment R and some of its add-on packages.

You can get a Matrix of all the dataset by doing `Rdatasets.overview()`
To view the description for the dataset use `Rdatasets.fetchInfo(packageName, itemName, htmlOrPLainText)` where package names and item names can be seen in the overview Matrix.
To get a specific dataset as a Matrix, use `Rdatasets.fetchData(packageName, itemName)`. For example, to get the iris dataset, you can do `Rdatasets.fetchData('datasets', 'iris')`. Note that the format of these datasets are all strings, so you may need to convert the columns to the appropriate types using the `Matrix.convert()` method.

# Release version compatibility matrix
The following table illustrates the version compatibility of the matrix datasets and matrix core

| Matrix datasets |    Matrix core | 
|----------------:|---------------:|
|           1.0.3 | 1.2.3 -> 1.2.4 |
|           1.0.4 | 2.0.0 -> 2.1.1 |
|           1.1.0 | 2.2.0 -> 2.2.1 |
|  2.0.0 -> 2.1.0 | 3.0.0 -> 3.0.0 |


