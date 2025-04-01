# Matrix Datasets Module

The matrix-datasets module provides a collection of common, public domain, and open-source datasets in Matrix format for Groovy applications. These datasets are similar to those commonly used in R and Python for data analysis and machine learning tasks.

## Installation

To use the matrix-datasets module, you need to add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation platform('se.alipsa.matrix:matrix-bom:2.2.0')
implementation 'se.alipsa.matrix:matrix-datasets'
```

### Maven Configuration

```xml
<project>
  <!-- Other project configurations -->
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
    <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-datasets</artifactId>
    </dependency>
  </dependencies>
</project>
```

## Available Datasets

The matrix-datasets module includes several popular datasets that are commonly used in data science and statistics:

1. **iris**: A famous dataset containing measurements of iris flowers
2. **mtcars**: Motor Trend Car Road Tests
3. **PlantGrowth**: Results from an experiment on plant growth
4. **ToothGrowth**: The effect of vitamin C on tooth growth in guinea pigs
5. **USArrests**: Violent crime rates by US state
6. **diamonds**: A dataset containing prices and attributes of diamonds
7. **mpg**: Fuel economy data from the EPA
8. **Map data**: Various geographical data

## Using the Datasets

Using the datasets is straightforward. You simply import the necessary classes and access the datasets through the `Dataset` class:

```groovy
import se.alipsa.matrix.datasets.*
import se.alipsa.matrix.core.*

// Load the iris dataset
Matrix iris = Dataset.iris()

// Print the first few rows
println(iris.head())

// Get basic information about the dataset
println("Dimensions: ${iris.rowCount()} rows x ${iris.columnCount()} columns")
println("Column names: ${iris.columnNames()}")
```

## Example: Analyzing the Iris Dataset

Let's explore the iris dataset in more detail:

```groovy
import se.alipsa.matrix.datasets.*
import se.alipsa.matrix.core.*

// Load the iris dataset
Matrix iris = Dataset.iris()

// Calculate mean sepal length by species
Matrix speciesMeans = Stat.meanBy(iris, 'Sepal Length', 'Species')
println(speciesMeans.content())

// Output:
// Iris-means by Species: 3 obs * 2 variables
// Species   	Sepal Length
// virginica 	 6.588000000
// setosa    	 5.006000000
// versicolor	 5.936000000

// Calculate summary statistics for each numeric column
def summary = Stat.summary(iris)
println(summary)

// Filter the dataset to get only one species
def speciesIdx = iris.columnIndex("Species")
def setosa = iris.subset {
    it[speciesIdx] == 'setosa'
}

// Calculate the mean of each measurement for setosa
println("Setosa means:")
println("Sepal Length: ${setosa['Sepal Length'].mean()}")
println("Sepal Width: ${setosa['Sepal Width'].mean()}")
println("Petal Length: ${setosa['Petal Length'].mean()}")
println("Petal Width: ${setosa['Petal Width'].mean()}")
```

## Example: Working with the mtcars Dataset

The mtcars dataset contains information about various car models:

```groovy
import se.alipsa.matrix.datasets.*
import se.alipsa.matrix.core.*
import se.alipsa.matrix.stats.*

// Load the mtcars dataset
Matrix mtcars = Dataset.mtcars()

// Print the first few rows
println(mtcars.head())

// Calculate the average mpg (miles per gallon) by number of cylinders
Matrix mpgByCyl = Stat.meanBy(mtcars, 'mpg', 'cyl')
println(mpgByCyl.content())

// Find cars with high horsepower (> 200)
def highPowerCars = mtcars.subset('hp', { it > 200 })
println("Cars with high horsepower:")
println(highPowerCars.content())

// Calculate correlation between mpg and weight
def correlation = Correlation.cor(mtcars['mpg'], mtcars['wt'])
println("Correlation between mpg and weight: ${correlation}")
```

## Example: Analyzing Plant Growth Data

The PlantGrowth dataset contains results from an experiment on plant growth:

```groovy
import se.alipsa.matrix.datasets.*
import se.alipsa.matrix.core.*
import se.alipsa.matrix.stats.Student

// Load the PlantGrowth dataset
Matrix plantGrowth = Dataset.plantGrowth()

// Print the dataset structure
println(plantGrowth.content())

// Calculate mean weight by group
Matrix weightByGroup = Stat.meanBy(plantGrowth, 'weight', 'group')
println(weightByGroup.content())

// Extract control and treatment groups
def ctrl = plantGrowth.subset('group', { it == 'ctrl' })
def trt1 = plantGrowth.subset('group', { it == 'trt1' })

// Perform t-test to compare control vs treatment 1
def tTestResult = Student.tTest(ctrl['weight'], trt1['weight'], false)
println("T-test result (ctrl vs trt1):")
println(tTestResult)
```

## Example: Analyzing Diamond Data

The diamonds dataset contains information about diamond prices and attributes:

```groovy
import se.alipsa.matrix.datasets.*
import se.alipsa.matrix.core.*

// Load the diamonds dataset
Matrix diamonds = Dataset.diamonds()

// Print the first few rows
println(diamonds.head())

// Calculate average price by diamond cut
Matrix priceByQuality = Stat.meanBy(diamonds, 'price', 'cut')
println("Average price by cut:")
println(priceByQuality.content())

// Calculate average price by diamond color
Matrix priceByColor = Stat.meanBy(diamonds, 'price', 'color')
println("Average price by color:")
println(priceByColor.content())

// Find the most expensive diamonds (top 5)
def sortedByPrice = diamonds.orderBy('price', false)
println("Top 5 most expensive diamonds:")
println(sortedByPrice.subset(0..4).content())
```

## Creating Your Own Datasets

While the matrix-datasets module provides several common datasets, you might want to create your own datasets for specific use cases. You can do this by creating a Matrix object from your data:

```groovy
import se.alipsa.matrix.core.*
import se.alipsa.matrix.csv.*

// Create a custom dataset
def customData = Matrix.builder().data(
    name: ['Alice', 'Bob', 'Charlie', 'David', 'Eve'],
    age: [25, 30, 35, 40, 45],
    score: [85, 90, 78, 92, 88]
).build()

// Save the dataset to a CSV file for future use
CsvExporter.exportToCsv(customData, new File('/path/to/custom_dataset.csv'))

// Later, you can load it back
def loadedData = Matrix.builder().data(new File('/path/to/custom_dataset.csv')).build()
```

## Conclusion

The matrix-datasets module provides a convenient way to access common datasets for data analysis and machine learning tasks in Groovy. These datasets can be used for learning, testing algorithms, or as reference data for your applications.

In the next section, we'll explore the matrix-spreadsheet module, which provides functionality for importing and exporting data between Matrix objects and Excel or OpenOffice Calc spreadsheets.

Go to [previous section](3-matrix-stats.md) | Go to [next section](5-matrix-spreadsheet.md) | Back to [outline](outline.md)