# example from https://www.r-bloggers.com/2024/02/why-pandas-feels-clunky-when-coming-from-r/?utm_source=phpList&utm_medium=email&utm_campaign=R-bloggers-daily&utm_content=HTML
import pandas as pd
purchases = pd.read_csv("purchases.csv")
purchases.head()

# “How much do we sell..? Let’s take the total sum!”
purchases["amount"].sum()

# Ah, they wanted it by country…”
(purchases
  .groupby("country")["amount"]
  .sum()
)

(purchases
  .groupby("country")
  .agg(total=("amount", "sum"))
  .reset_index()             
)

# And I guess I should deduct the discount.”
(purchases
  .groupby("country")
  .apply(lambda df: (df["amount"] - df["discount"]).sum())
  .reset_index()
  .rename(columns={0: "total"})                           
)

# “Oh, and Maria asked me to remove any outliers.”
(purchases
  .query("amount <= amount.median() * 10")
  .groupby("country")
  .apply(lambda df: (df["amount"] - df["discount"]).sum())
  .reset_index()
  .rename(columns={0: "total"})
)

# “I probably should use the median within each country”
(purchases
  .groupby("country")                                               
  .apply(lambda df: df[df["amount"] <= df["amount"].median() * 10]) 
  .reset_index(drop=True)                                           
  .groupby("country")
  .apply(lambda df: (df["amount"] - df["discount"]).sum())
  .reset_index()
  .rename(columns={0: "total"})
)

# alternative to the above:
(purchases
  .assign(country_median=lambda df:                         #👈
      df.groupby("country")["amount"].transform("median")   #👈
  )
  .query("amount <= country_median * 10")                   #👈                   
  .groupby("country")
  .apply(lambda df: (df["amount"] - df["discount"]).sum())
  .reset_index()
  .rename(columns={0: "total"})
)