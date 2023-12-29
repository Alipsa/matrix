import se.alipsa.groovy.charts.Chart
import se.alipsa.groovy.charts.Plot
import se.alipsa.groovy.charts.ScatterChart
import se.alipsa.groovy.datasets.*
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Stat
import se.alipsa.groovy.stats.Correlation
import se.alipsa.groovy.stats.regression.LinearRegression
import se.alipsa.groovy.stats.Sampler


Matrix cars = Dataset.cars()

// Display the top 8 rows of the cars dataset
println cars.head(8)

// Display the structure of the cars dataset
println Stat.str(cars)

Chart chart = ScatterChart.create("Speed vs distance", cars, 'dist', 'speed')
File scatterplotFile = new File('cars.png')
Plot.png(chart, scatterplotFile, 800, 600)
println "Wrote ${scatterplotFile.getAbsolutePath()}"

println "Correlation between speed and distance is ${Correlation.cor(cars['speed'], cars['dist'])}"

def model = new LinearRegression(cars, 'speed', 'dist')
println model.summary()

// https://www.simplilearn.com/tutorials/data-science-tutorial/data-science-with-r
def (trainingData, testData) = Sampler.split(cars, 0.8)

def lm = new LinearRegression(trainingData, 'speed', 'dist')
def predictions = lm.predict(testData['speed'], 1)
println "Predictions: ${predictions.sort()}"
println "Actuals: ${testData['dist'].sort()}"
System.exit(0)

/*
head(cars)
str(cars)
plot(cars)

cor(cars$speed, cars$dist)

linearMod <- lm(dist~speed, data=cars)
summary(linearMod)

set.seed(100)
trainingRowIndex <- sample(1:nrow(cars), 0.8 * nrow(cars))
trainingData <- cars[trainingRowIndex,]
testData <-cars[-trainingRowIndex,]

lmMod <- lm(dist~speed, data=trainingData)
distPred <- predict(lmMod, testData)

summary(lmMod)

str(distPred)
library(DMwR)
DMwR::regr.eval(testData[,'dist'], distPred)
 */
