/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import se.alipsa.groovy.matrix.*

import static se.alipsa.groovy.matrix.Stat.*

def file = io.projectFile("src/main/resources/data/kc_house_data.csv")
assert file != null

Matrix rows = Matrix.builder().data(file).build()

println "House data overview"
println "-------------------"

println str(rows)

println summary(rows.bedrooms)

println "\nHouses with more than 10 bedrooms"
println   "---------------------------------"
println rows.subset("bedrooms") {
  it as Integer > 10
}.content()

// another way is to use ginq:
def bigHouse = GQ {
  from r in rows
  where r.bedrooms as Integer > 10
  select r
}

''
println Matrix.builder().ginqResult(bigHouse).build().content()

// TODO continue matrix conversion here
println "\nHouses with less than 30 bedrooms"
println   "---------------------------------"
cleaned = rows.dropWhere(rows.column("bedrooms").isGreaterThan(30))
println cleaned.shape()
println cleaned.summarize("price", mean, min, max).by("bedrooms")

helper.show(ScatterPlot.create("Price x bathrooms x grade", cleaned, "bathrooms", "price", 'grade'), 'PriceBathroomsGrade')

cleaned.addColumns(
    StringColumn.create("waterfrontDesc", cleaned.column("waterfront").collect{ it ? 'waterfront' : 'interior' }),
    DoubleColumn.create("scaledGrade", cleaned.column("grade").collect{ it * 2 }),
    DoubleColumn.create("scaledPrice", cleaned.column("price").collect{ it / 100000 })
)

helper.show(BubblePlot.create("Price vs living area and grade (bubble size)",
        cleaned, "sqft_living", "price", "scaledGrade", "waterfrontDesc"), 'LivingPriceGradeWaterfront')

helper.show(Scatter3DPlot.create("Grade, living space, bathrooms and price (bubble size)",
        cleaned, "sqft_living", "bathrooms", "grade", "scaledPrice", "waterfrontDesc"), 'LivingBathroomsGradePriceWaterfront')
