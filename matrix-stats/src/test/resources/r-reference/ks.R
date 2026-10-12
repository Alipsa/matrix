print(R.version.string)
set.seed(7); x <- runif(150)
writeLines(format(x,digits=17),'matrix-stats/src/test/resources/r-reference/uniform150.csv')
print(ks.test(x,'punif'))
