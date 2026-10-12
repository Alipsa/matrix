print(R.version.string)
set.seed(7); x <- runif(150)
writeLines(format(x,digits=17),'matrix-stats/src/test/resources/r-reference/uniform150.csv')
print(ks.test(x,'punif'))

# Large exact matrix powers need exponent scaling (PR review regression).
for (n in c(700,750,1000)) print(ks.test(qnorm(((1:n)-.5)/n)+.1,'pnorm',exact=TRUE),digits=17)
