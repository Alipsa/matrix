# Matrix stats release history

## v2.3.0, In progress
-  added kernel density for Gaussian, Epanechnikov, uniform, and triangular kernels
- Add RegressionUtils for easy regression diagnostics and standard error calculations

## v2.2.0, 2025-06-02
- Ensure that all methods in Normalize can handle all zeroes as input
- Add KMeans clustering
  Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-stats/2.2.0/matrix-stats-2.2.0.jar)

## v2.1.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-stats/2.1.0/matrix-stats-2.1.0.jar)
- add temp implementation of one way anova using commons math.

## v2.0.0, 2025-03-12
- Require JDK 21
- overload Normalize methods with a way to normalize an entire matrix

## v1.1.0, 2025-01-06
- adapt to Matrix 2.2.0

## v1.0.2, 2024-10-31
- adapt to matrix 2.0.0
- Implement GoalSeek

## v1.0.1, 2024-07-04
- Implement Accuracy.evaluatePredictions
- Add DecisionTree and Randomize to support LineCharts

## v1.0.0, 2023-08-06
- initial version able to handle correlations,
- normalization, linear regression, and t-tests