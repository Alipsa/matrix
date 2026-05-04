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
import static java.time.Month.JANUARY
import static tech.tablesaw.aggregate.AggregateFunctions.mean
import static tech.tablesaw.api.QuerySupport.and
import static tech.tablesaw.io.xlsx.XlsxReadOptions.builder

import groovy.transform.Field

import tech.tablesaw.api.*
import tech.tablesaw.io.xlsx.XlsxReader
import tech.tablesaw.plotly.components.*
import tech.tablesaw.plotly.traces.ScatterTrace
import tech.tablesaw.plotly.traces.Trace
import tech.tablesaw.selection.Selection

import se.alipsa.gi.swing.*

import java.time.*
import java.util.function.Function

// Based on https://github.com/paulk-asert/groovy-data-science/blob/master/subprojects/Candles/src/main/groovy/CandleRatings.groovy

@Field String colDate = 'Date'
@Field String colYearMonth = 'YearMonth'
@Field String colRating = 'Rating'
@Field String colMeanRating = 'Mean [Rating]'
@Field String colVal = 'Val'
@Field int trendLineWidth = 5

List<Trace> traces(URL url, String lineColor, String markerColor) {
    def table = new XlsxReader().read(builder(url).build())

    table.addColumns(
        DateColumn.create(colYearMonth, table.column(colDate).collect { LocalDate.of(it.year, it.month, 15) })
    )
    def janFirst2017 = LocalDateTime.of(2017, JANUARY, 1, 0, 0)
    Function<Table, Selection> from2017 = r -> r.dateTimeColumn(colDate).isAfter(janFirst2017)
    Function<Table, Selection> top3 = r -> r.intColumn('CandleID').isLessThanOrEqualTo(3)

    def byMonth = table.sortAscendingOn(colDate)
            .where(and(from2017, top3))
            .summarize(colRating, mean).by(colYearMonth)
    def byDate = table.sortAscendingOn(colDate)
            .where(and(from2017, top3))
            .summarize(colRating, mean).by(colDate)

    def averaged = ScatterTrace.builder(byMonth.dateColumn(colYearMonth), byMonth.nCol(colMeanRating))
            .mode(ScatterTrace.Mode.LINE)
            .line(Line.builder().width(trendLineWidth).color(lineColor).shape(Line.Shape.SPLINE).smoothing(1.3).build())
            .build()
    def scatter = ScatterTrace.builder(byDate.dateTimeColumn(colDate), byDate.nCol(colMeanRating))
            .marker(Marker.builder().opacity(0.3).color(markerColor).build())
            .build()
    [averaged, scatter]
}

Layout layout(String variant) {
    Layout.builder("Top 3 $variant candles Amazon reviews 2017-2020", colDate, 'Average daily rating (1-5)')
            .showLegend(false).width(1000).height(500).build()
}

covidReported = LocalDateTime.of(2020, JANUARY, 20, 0, 0)
reported = Table.create(DateTimeColumn.create(colDate), IntColumn.create(colVal))
reported.appendRow().with { setDateTime(colDate, covidReported); setInt(colVal, 1) }
reported.appendRow().with { setDateTime(colDate, covidReported); setInt(colVal, trendLineWidth) }
line = ScatterTrace.builder(reported.dateTimeColumn(colDate), reported.nCol(colVal))
        .mode(ScatterTrace.Mode.LINE)
        .line(Line.builder().width(2).dash(Line.Dash.DOT).color('red').build())
        .build()

url = this.getClass().getResource('/data/Scented_all.xlsx')
def helper = new TablesawHelper(url.file)
(sAverage, sScatter) = traces(url, 'seablue', 'lightskyblue')

url = this.getClass().getResource('/data/Unscented_all.xlsx')
(uAverage, uScatter) = traces(url, 'seagreen', 'lightgreen')

helper.save(new Figure(layout('scented'), sAverage, sScatter, line), 'ScentedRatings')
helper.save(new Figure(layout('unscented'), uAverage, uScatter, line), 'UnscentedRatings')
