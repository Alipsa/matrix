library(ggplot2)

df <- data.frame(
  x = 1:5,
  y = c(1, 4, 2, 5, 3),
  value = c(0, 2.5, 5, 7.5, 10)
)

legend_plot <- ggplot(df, aes(x, y, colour = value)) +
  geom_point(size = 3) +
  scale_colour_gradient(
    low = "#132B43",
    high = "#F6C141",
    breaks = c(0, 5, 10),
    labels = c("low", "mid", "high")
  ) +
  guides(colour = guide_legend()) +
  labs(title = "Guide legend")

colorbar_plot <- ggplot(df, aes(x, y, colour = value)) +
  geom_point(size = 3) +
  scale_colour_gradient(
    low = "#132B43",
    high = "#F6C141",
    breaks = c(0, 5, 10),
    labels = c("low", "mid", "high")
  ) +
  guides(colour = guide_colourbar()) +
  labs(title = "Guide colorbar")

ggsave("guide_legend.svg", plot = legend_plot, width = 4, height = 3)
ggsave("guide_colorbar.svg", plot = colorbar_plot, width = 4, height = 3)
