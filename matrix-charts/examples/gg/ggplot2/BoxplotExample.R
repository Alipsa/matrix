library(Require)
Require(c("ggplot2", "svglite"))

p <- ggplot(mpg, aes(cty, hwy)) +
  geom_boxplot(aes(group = cut_width(displ, 1)))
ggsave("boxplot.svg", plot = p)