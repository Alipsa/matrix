import se.alipsa.matrix.charts.Chart
import se.alipsa.matrix.charts.Plot
import se.alipsa.matrix.charts.ScatterChart
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.Accuracy
import se.alipsa.matrix.stats.Correlation
import se.alipsa.matrix.stats.regression.LinearRegression
import se.alipsa.matrix.stats.Sampler


Matrix cars = Dataset.cars()

// Display the top 8 rows of the cars dataset
println cars.head(8)

// Display the structure of the cars dataset
println Stat.str(cars)

Chart chart = ScatterChart.create("Speed vs distance", cars, 'speed','dist')
File scatterplotFile = new File('cars.png')
Plot.png(chart, scatterplotFile, 800, 600)
println "Wrote ${scatterplotFile.getAbsolutePath()}"

println "Correlation between speed and distance is ${Correlation.cor(cars['speed'], cars['dist'])}"

def model = new LinearRegression(cars, 'speed', 'dist')
println model.summary()


def (trainingData, testData) = Sampler.split(cars, 0.8)

def lm = new LinearRegression(trainingData, 'speed', 'dist')
def predictions = lm.predict(testData['speed'], 1)
println "Predictions: ${predictions.sort()}"
println "Actuals: ${testData['dist'].sort()}"

def accuracy = Accuracy.evaluatePredictions(predictions, testData['dist'])
println "accuracy: $accuracy"
/*
https://www.simplilearn.com/tutorials/data-science-tutorial/data-science-with-r
Equivalent code in R:
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
