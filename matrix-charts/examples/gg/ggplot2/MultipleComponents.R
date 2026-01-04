library(Require)
Require(c("ggplot2", "svglite", "Hmisc"))

geom_mean <- function() {
  list(
    stat_summary(fun = "mean", geom = "bar", fill = "grey70"),
    stat_summary(fun.data = "mean_cl_normal", geom = "errorbar", width = 0.4)
  )
}
geom_median <- function() {
  list(
    stat_summary(fun = "median", geom = "bar"),
    stat_summary(fun.data = "median_hilow", geom = "errorbar", width = 0.4)
  )
}
chart1 <- ggplot(mpg, aes(class, cty)) + geom_mean()
chart2 <- ggplot(mpg, aes(x = drv, y = cty, fill = drv)) +
  geom_median() +
  ggtitle('Median cty by drv') +
  theme(plot.title = element_text(hjust = 0.5))

ggsave("MultipleComponents1.svg", plot = chart1)
ggsave("MultipleComponents2.svg", plot = chart2)