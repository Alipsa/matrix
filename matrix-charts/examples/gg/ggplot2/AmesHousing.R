library(Require)
Require(c("ggplot2", "tidyverse", "svglite"))

AmesHousing <- read.csv("https://raw.githubusercontent.com/ds4stats/r-tutorials/master/data-viz/data/AmesHousing.csv")

p <- ggplot(data=AmesHousing) +
      geom_histogram(mapping = aes(SalePrice/100000),
          breaks=seq(0, 7, by = 1), col="red", fill="lightblue") +
      geom_density(mapping = aes(x=SalePrice/100000, y = (..count..)))  +
      labs(title="Figure 9: Housing Prices in Ames, Iowa (in $100,000)",
          x="Sale Price of Individual Homes")

ggsave("amesHousing_figure9.svg", plot = p)

p <- ggplot(data=AmesHousing, aes(x=log(GrLivArea), y=log(SalePrice)) ) +
  geom_point(shape = 3, color = "darkgreen") +
  geom_smooth(method=lm,  color="green") +
  labs(title="Figure 10: Housing Prices in Ames, Iowa")

ggsave("amesHousing_figure10.svg", plot = p)

p <- ggplot(data=AmesHousing) +
  geom_point(aes(x=log(GrLivArea), y=log(SalePrice), color=KitchenQual),shape=2, size=2) +
  geom_smooth(aes(x=log(GrLivArea), y=log(SalePrice), color=KitchenQual),
          method=loess, size=1) +
  labs(title="Figure 11: Housing Prices in Ames, Iowa")

ggsave("amesHousing_figure11.svg", plot = p)


p <- ggplot(data=AmesHousing) +
       geom_point(mapping = aes(x=log(GrLivArea), y=log(SalePrice), color=KitchenQual)) +
       geom_smooth(mapping = aes(x=log(GrLivArea), y=log(SalePrice), color=KitchenQual),
           method=lm, se=FALSE, fullrange=TRUE) +
       facet_grid(. ~ Fireplaces) +
       labs(title="Figure 12: Housing Prices in Ames, Iowa")

ggsave("amesHousing_figure12.svg", plot = p)

# Create a new data frame with only houses with less than 3 fireplaces
AmesHousing2 <- AmesHousing %>% filter( Fireplaces < 3)
# Create a new variable called Fireplace2
AmesHousing2 <-mutate(AmesHousing2,Fireplace2=as.factor(Fireplaces))
#str(AmesHousing2)
p <- ggplot(data=AmesHousing2) +
  geom_density(aes(SalePrice, color = Fireplace2,  fill = Fireplace2), alpha = 0.2)

ggsave("amesHousing_Fireplace2.svg", plot = p)