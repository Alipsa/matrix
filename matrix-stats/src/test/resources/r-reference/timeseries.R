suppressPackageStartupMessages(library(urca))
print(packageVersion('urca'))
set.seed(42)
y <- cumsum(rnorm(200))
# Run from repository root; values are written with 17 significant digits.
writeLines(c('y', format(y, digits = 17, trim = TRUE)),
  'matrix-stats/src/test/resources/r-reference/rw200.csv')
print(ur.ers(y, type='DF-GLS', model='trend', lag.max=4)@teststat, digits=17)
print(ur.ers(y, type='DF-GLS', model='constant', lag.max=4)@teststat, digits=17)
t <- 1:50000
z <- sin(0.1*t) + 0.5*cos(0.37*t) + ((7919*t) %% 101)/100
print(Box.test(z,lag=10,type='Ljung-Box'), digits=17)
# Ng-Perron (2001) common-sample MAIC; direct port of the equation in section 20.4.
maic <- function(y,type='drift') {
 n<-length(y); maxlag<-min(12,floor(12*(n/100)^.25),floor((n-12)/2))
 alpha<-1+if(type=='drift') -7/n else -13.5/n
 z<-if(type=='drift') matrix(1,n,1) else cbind(1,1:n)
 qy<-c(y[1],y[-1]-alpha*y[-n]); qz<-rbind(z[1,,drop=FALSE],z[-1,,drop=FALSE]-alpha*z[-n,,drop=FALSE])
 d<-y-as.vector(z%*%qr.coef(qr(qz),qy)); dy<-diff(d)
 rows<-(maxlag+1):(n-1); count<-length(rows)
 scores<-sapply(0:maxlag,function(p) {
  X<-matrix(d[rows],ncol=1)
  if(p>0) X<-cbind(X,sapply(1:p,function(j)dy[rows-j]))
  fit<-lm.fit(X,dy[rows]); sigma2<-sum(fit$residuals^2)/count
  log(sigma2)+2*(fit$coefficients[1]^2*sum(d[rows]^2)/sigma2+p)/count
 })
 which.min(scores)-1
}
set.seed(73); ar2<-as.numeric(arima.sim(list(ar=c(.6,-.25)),n=200))
writeLines(format(ar2,digits=17),'matrix-stats/src/test/resources/r-reference/ar2-200.csv')
print(c(maicLag=maic(ar2)))
# Explicit-lag Granger F on the same rows used by matrix-stats.
t<-1:60; x<-sin(.17*t)+cos(.31*t); y<-c(0,x[-60])+.2*sin(.43*t); p<-2
idx<-(p+1):60; response<-y[idx]
restricted<-lm(response~y[idx-1]+y[idx-2]); unrestricted<-lm(response~y[idx-1]+y[idx-2]+x[idx-1]+x[idx-2])
print(anova(restricted,unrestricted),digits=17)
