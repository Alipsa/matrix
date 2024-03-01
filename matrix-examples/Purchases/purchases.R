# example from https://www.r-bloggers.com/2024/02/why-pandas-feels-clunky-when-coming-from-r/?utm_source=phpList&utm_medium=email&utm_campaign=R-bloggers-daily&utm_content=HTML

library(tidyverse)
purchases <- read_csv("purchases.csv")
purchases |> head()

# “How much do we sell..? Let’s take the total sum!”

purchases$amount |> sum()

# “Ah, they wanted it by country…”

purchases |>
  group_by(country) |>
  summarize(total = sum(amount))

# “And I guess I should deduct the discount.” 
purchases |> 
  group_by(country) |> 
  summarize(total = sum(amount - discount))

# “Oh, and Maria asked me to remove any outliers. Let’s remove everything 10x larger than the median.”
purchases |>
  filter(amount <= median(amount) * 10) |> 
  group_by(country) |> 
  summarize(total = sum(amount - discount))


# I probably should use the median within each country. Prices are quite different across the globe…
purchases |>
  group_by(country) |>
  filter(amount <= median(amount) * 10) |>
  summarize(total = sum(amount - discount))
