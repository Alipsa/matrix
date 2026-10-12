print(R.version.string)
print(c(scott=bw.nrd(iris$Sepal.Length),silverman=bw.nrd0(iris$Sepal.Length)),digits=17)
x <- c(1.2,2.3,2.9,3.1,3.8,4.4,5,5.2,6.7,7.1); u <- (4-x)/.8
print(c(gaussian=mean(dnorm(u))/.8,
 epanechnikov=mean(ifelse(abs(u)<=sqrt(5),.75*(1-u^2/5)/sqrt(5),0))/.8,
 uniform=mean(ifelse(abs(u)<=sqrt(3),.5/sqrt(3),0))/.8,
 triangular=mean(pmax(0,1-abs(u)/sqrt(6))/sqrt(6))/.8),digits=17)
