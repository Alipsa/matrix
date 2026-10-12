# R 4.6.1; base R, no optional packages required.
print(R.version.string)
print(pnorm(c(-10, -9)), digits = 17)
print(qnorm(1e-20), digits = 17)
print(2 * pt(-10, 100), digits = 17)
print(pt(-5, 30), digits = 17)
print(pf(100, 3, 100, lower.tail = FALSE), digits = 17)
print(pchisq(c(1999, 9999, 99999), c(2000, 10000, 100000)), digits = 17)
print(pchisq(200, 2, lower.tail = FALSE), digits = 17)
