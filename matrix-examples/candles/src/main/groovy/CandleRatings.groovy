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
 *
 * Original code is here: https://github.com/paulk-asert/groovy-data-science/blob/master/subprojects/Candles/src/main/groovy/CandleRatings.groovy
 * Slightly modified to behave nicely in Gade
 */
import tech.tablesaw.api.*
import tech.tablesaw.io.xlsx.XlsxReader
import tech.tablesaw.plotly.components.*
import tech.tablesaw.plotly.traces.ScatterTrace
import tech.tablesaw.plotly.traces.Trace
import tech.tablesaw.selection.Selection

import java.time.*
import java.util.function.Function

import static java.time.Month.JANUARY
import static tech.tablesaw.aggregate.AggregateFunctions.mean
import static tech.tablesaw.api.QuerySupport.and
import static tech.tablesaw.io.xlsx.XlsxReadOptions.builder

// helper function
List<Trace> traces(URL url, String lineColor, String markerColor) {
    def table = new XlsxReader().read(builder(url).build())

    table.addColumns(
        DateColumn.create('YearMonth', table.column('Date').collect { LocalDate.of(it.year, it.month, 15) })
    )
    def janFirst2017 = LocalDateTime.of(2017, JANUARY, 1, 0, 0)
    Function<Table, Selection> from2017 = r -> r.dateTimeColumn('Date').isAfter(janFirst2017)
    Function<Table, Selection> top3 = r -> r.intColumn('CandleID').isLessThanOrEqualTo(3)

    def byMonth = table.sortAscendingOn('Date')
            .where(and(from2017, top3))
            .summarize('Rating', mean).by('YearMonth')
    def byDate = table.sortAscendingOn('Date')
            .where(and(from2017, top3))
            .summarize('Rating', mean).by('Date')

    def averaged = ScatterTrace.builder(byMonth.dateColumn('YearMonth'), byMonth.nCol('Mean [Rating]'))
            .mode(ScatterTrace.Mode.LINE)
            .line(Line.builder().width(5).color(lineColor).shape(Line.Shape.SPLINE).smoothing(1.3).build())
            .build()
    def scatter = ScatterTrace.builder(byDate.dateTimeColumn('Date'), byDate.nCol('Mean [Rating]'))
            .marker(Marker.builder().opacity(0.3).color(markerColor).build())
            .build()
    [averaged, scatter]
}

Layout layout(String variant) {
    Layout.builder("Top 3 $variant candles Amazon reviews 2017-2020", 'Date', 'Average daily rating (1-5)')
            .showLegend(false).width(1000).height(500).build()
}

// create the start of COVID line
covidReported = LocalDateTime.of(2020, JANUARY, 20, 0, 0)
reported = Table.create(DateTimeColumn.create('Date'), IntColumn.create('Val'))
reported.appendRow().with {setDateTime('Date', covidReported); setInt('Val', 1) }
reported.appendRow().with {setDateTime('Date', covidReported); setInt('Val', 5) }
line = ScatterTrace.builder(reported.dateTimeColumn('Date'), reported.nCol('Val'))
        .mode(ScatterTrace.Mode.LINE)
        .line(Line.builder().width(2).dash(Line.Dash.DOT).color('red').build())
        .build()

url = new File(io.scriptDir(), '../data/Scented_all.xlsx').toURL()
(sAverage, sScatter) = traces(url, 'seablue', 'lightskyblue')

url = new File(io.scriptDir(), '../data/Unscented_all.xlsx').toURL()
(uAverage, uScatter) = traces(url, 'seagreen', 'lightgreen')

io.display(new Figure(layout(''), sAverage, sScatter, uAverage, uScatter, line))
io.display(new Figure(layout('scented'), sAverage, sScatter, line), 'ScentedRatings')
io.display(new Figure(layout('unscented'), uAverage, uScatter, line), 'UnscentedRatings')
//helper.show(new Figure(layout(''), sAverage, sScatter, uAverage, uScatter, line))
//helper.show(new Figure(layout('scented'), sAverage, sScatter, line), 'ScentedRatings')
//helper.show(new Figure(layout('unscented'), uAverage, uScatter, line), 'UnscentedRatings')