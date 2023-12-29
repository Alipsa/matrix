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
@Grab("org.knowm.xchart:xchart:3.8.1")
@Grab("org.apache.commons:commons-math3:3.6.1")

import tech.tablesaw.api.*
import tech.tablesaw.plotly.api.*
import org.knowm.xchart.CategoryChartBuilder
import org.knowm.xchart.Histogram
import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.XChartPanel
import javax.swing.JPanel

binCount = 50
table = Table.read().csv(new File(io.scriptDir(), "../data/kc_house_data.csv"))

price = table.where(table.column("bedrooms").isLessThan(30)).column("price")
println price.summary()

def hist2 = new Histogram(price.asList(), binCount)
def chart = new CategoryChartBuilder().width(900).height(450)
        .title("Price Histogram").xAxisTitle("Price").yAxisTitle("Count").build()
chart.addSeries("Price", hist2.xAxisData, hist2.yAxisData)
chart.styler.with {
    XAxisLabelRotation = 90
    availableSpaceFill = 0.98
    XAxisMin = 0
    XAxisMax = 8_000_000
}

JPanel chartPanel = new XChartPanel<Chart>(chart);
io.display(chartPanel, "Price histogram")
//new SwingWrapper(chart).displayChart()