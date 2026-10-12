print(R.version.string)
d <- data.frame(y=1:8,x=c(1,3,2,5,4,6,8,7),g=factor(c('a','b','c','a','b','c','a','b')),
  h=factor(c('u','v','u','v','u','v','u','v')))
for (f in c('y~0+g','y~g:x','y~g+g:x','y~x+g:x','y~0+g+h','y~g:h','y~0+x+g',
  'y~0+g:h','y~0+g+g:h','y~0+g:x+h')) print(colnames(model.matrix(as.formula(f),d)))
print(coef(lm(y~0+g,d)), digits=17)

d$z <- c(2,1,4,3,6,5,8,9); d$w <- c(1,1,2,2,3,3,4,5)
for (f in c('y~x+I(x^2)','y~z+x','y~g*x','y~x:z+w','y~(x+z+w)^2','y~w+g+x')) print(colnames(model.matrix(as.formula(f),d)))
