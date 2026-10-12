suppressPackageStartupMessages(library(nortest))
print(packageVersion('nortest'))
x <- c(148,154,158,160,161,162,166,170,182,195,236)
for (data in list(x, iris$Sepal.Length, mtcars$mpg, c(1,2,4), c(2.1,3.5,1.2,8.9,4.4))) {
  print(shapiro.test(data), digits = 17)
  if (length(data) >= 5) {
    print(lillie.test(data), digits = 17)
    print(sf.test(data), digits = 17)
  }
  if (length(data) >= 8) print(cvm.test(data), digits = 17)
}
suppressPackageStartupMessages(library(fBasics))
print(packageVersion('fBasics'))
for (x in list(iris$Sepal.Length, mtcars$mpg)) print(dagoTest(x))
