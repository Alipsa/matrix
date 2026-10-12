suppressPackageStartupMessages(library(Exact))
print(packageVersion('Exact'))
print(R.version.string)
print(fisher.test(matrix(c(200,10,10,200),2,byrow=TRUE)), digits=17)
a <- array(c(300,250,280,310,320,260,290,300),c(2,2,2))
print(mantelhaen.test(a), digits=17)
print(mantelhaen.test(a,correct=FALSE), digits=17)
print(prop.trend.test(c(300,350,420),c(1000,1000,1000)), digits=17)
suppressPackageStartupMessages(library(Exact))
print(packageVersion('Exact'))
for(t in list(c(7,12,8,3),c(10,2,3,9),c(5,5,9,1))) {
 for(method in c('z-pooled','boschloo')) print(exact.test(matrix(t,2,byrow=TRUE),model='Binomial',cond.row=TRUE,method=method))
}
fisherPrecise <- function(t, conf=.95) {
 x <- t[1,1]; m <- sum(t[,1]); n <- sum(t[,2]); k <- sum(t[1,])
 lo <- max(0,k-n); hi <- min(k,m); s <- lo:hi; ld <- dhyper(s,m,n,k,log=TRUE)
 dn <- function(u) { l <- ld + u*s; e <- exp(l-max(l)); e/sum(e) }
 root <- function(f) exp(uniroot(f,c(-50,50),tol=1e-14)$root)
 a <- (1-conf)/2
 c(estimate=if(x==lo) 0 else if(x==hi) Inf else root(function(u)sum(s*dn(u))-x),
   lower=if(x==lo) 0 else root(function(u)sum(dn(u)[s>=x])-a),
   upper=if(x==hi) Inf else root(function(u)sum(dn(u)[s<=x])-a))
}
for(t in list(c(3,1,1,3),c(200,10,10,200),c(10,0,3,12),c(2,7,8,2))) print(fisherPrecise(matrix(t,2,byrow=TRUE)),digits=17)
