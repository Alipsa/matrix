# R reference values

These scripts regenerate the expected values used by the statistical regression tests.
The reference environment is R 4.6.1. Tests read literals or CSV fixtures from the
classpath and never run R or require R packages at build time.

From the repository root, run:

```sh
Rscript matrix-stats/src/test/resources/r-reference/distributions.R
Rscript matrix-stats/src/test/resources/r-reference/normality.R
Rscript matrix-stats/src/test/resources/r-reference/contingency.R
Rscript matrix-stats/src/test/resources/r-reference/formula.R
Rscript matrix-stats/src/test/resources/r-reference/regression.R
Rscript matrix-stats/src/test/resources/r-reference/timeseries.R
```

`distributions.R` uses base R. `normality.R` requires nortest 1.0.4, installed
in your R library (`install.packages('nortest')`), and prints its version.
The scripts have no parameters and print results to standard output.

`timeseries.R` requires urca 1.3.4 (`install.packages('urca')`) and
regenerates `rw200.csv` in place from `set.seed(42); cumsum(rnorm(200))`.
Run it from the repository root. `regression.R` also uses MASS for robust ellipse covariance.

Additional references:

```sh
Rscript matrix-stats/src/test/resources/r-reference/kde.R
Rscript matrix-stats/src/test/resources/r-reference/ks.R
```

`ks.R` regenerates the 150-observation uniform fixture in place. `timeseries.R`
also regenerates `ar2-200.csv` and prints the direct Ng–Perron MAIC reference.
`contingency.R` requires Exact 3.3 and prints precise Fisher conditional roots.
`normality.R` additionally requires fBasics 4052.98 for D'Agostino references.
