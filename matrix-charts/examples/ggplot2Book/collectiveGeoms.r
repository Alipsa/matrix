# These examples are from the '4 Collective geoms' chapter of the Ggplot2 Book (https://ggplot2-book.org/collective-geoms.html)
library(ggplot2)

data(Oxboys, package = "nlme")
head(Oxboys)
#>   Subject     age height Occasion
#> 1       1 -1.0000    140        1
#> 2       1 -0.7479    143        2
#> 3       1 -0.4630    145        3
#> 4       1 -0.1643    147        4
#> 5       1 -0.0027    148        5
#> 6       1  0.2466    150        6


# 4.1 Multiple groups, one aesthetic
ggplot(Oxboys, aes(age, height, group = Subject)) +
  geom_point() +
  geom_line()

# If you incorrectly specify the grouping variable, you’ll get a characteristic sawtooth appearance:
ggplot(Oxboys, aes(age, height)) +
  geom_point() +
  geom_line()


# 4.2 Different groups on different layers

ggplot(Oxboys, aes(age, height, group = Subject)) +
  geom_line() +
  geom_smooth(method = "lm", se = FALSE)
#> `geom_smooth()` using formula 'y ~ x'

ggplot(Oxboys, aes(age, height)) +
  geom_line(aes(group = Subject)) +
  geom_smooth(method = "lm", linewidth = 2, se = FALSE)
#> `geom_smooth()` using formula = 'y ~ x'


# 4.3 Overriding the default grouping
ggplot(Oxboys, aes(Occasion, height)) +
  geom_boxplot()

# Simply adding geom_line() does not work: the lines are drawn within each occasion, not across each subject:
ggplot(Oxboys, aes(Occasion, height)) +
  geom_boxplot() +
  geom_line(colour = "#3366FF", alpha = 0.5)

# To get the plot we want, we need to override the grouping to say we want one line per boy:
ggplot(Oxboys, aes(Occasion, height)) +
  geom_boxplot() +
  geom_line(aes(group = Subject), colour = "#3366FF", alpha = 0.5)


# 4.4 Matching aesthetics to graphic objects

df <- data.frame(x = 1:3, y = 1:3, colour = c(1, 3, 5))

ggplot(df, aes(x, y, colour = factor(colour))) +
  geom_line(aes(group = 1), linewidth = 2) +
  geom_point(size = 5)

ggplot(df, aes(x, y, colour = colour)) +
  geom_line(aes(group = 1), linewidth = 2) +
  geom_point(size = 5)

xgrid <- with(df, seq(min(x), max(x), length = 50))
interp <- data.frame(
  x = xgrid,
  y = approx(df$x, df$y, xout = xgrid)$y,
  colour = approx(df$x, df$colour, xout = xgrid)$y
)
ggplot(interp, aes(x, y, colour = colour)) +
  geom_line(linewidth = 2) +
  geom_point(data = df, size = 5)

ggplot(mpg, aes(class)) +
  geom_bar()
ggplot(mpg, aes(class, fill = drv)) +
  geom_bar()

ggplot(mpg, aes(class, fill = hwy)) +
  geom_bar()
ggplot(mpg, aes(class, fill = hwy, group = hwy)) +
  geom_bar()