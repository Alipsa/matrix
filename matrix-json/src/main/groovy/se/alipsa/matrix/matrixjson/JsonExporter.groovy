package se.alipsa.matrix.matrixjson

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import groovy.json.*

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

class JsonExporter {

    Matrix table

    JsonExporter(Grid grid, List<String> columnNames) {
        this.table = Matrix.builder()
        .columnNames(columnNames)
        .data(grid)
        .build()
    }

    JsonExporter(Matrix table) {
        this.table = table
    }

    String toJson(boolean indent = false) {
        toJson([:], indent)
    }

    String toJson(Map<String, Closure> columnFormatters) {
        toJson(columnFormatters, false)
    }

    String toJson(String dateFormat) {
        toJson([:], false, dateFormat)
    }

    /**
     * If your data contains temporal data, you should add a converter for each such column
     * that converts the data into strings. If you are satisfied with converting all temporal data
     * to the format yyyy-MM-dd then you can skip that, or alternatively supply a dateFormat pattern which
     * applies to all Date and TemporalAccessor columns
     *
     * @param columnFormatters
     * @param indent whether to pretty print the json string or not
     * @param dateFormat optional date format pattern, default is yyyy-MM-dd
     * @return a json string
     */
    String toJson(Map<String, Closure> columnFormatters, boolean indent, String dateFormat='yyyy-MM-dd') {
        def t = table.clone()
        columnFormatters.each { k,v ->
            t.apply(k, v)
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat)
        /*
        // we do this in a custom formatter in the JsonGenerator below instead
        def names = (t.columnNames(TemporalAccessor) - columnFormatters.keySet() as List)
        names.each { name ->
            t.convert(name, String, {dtf.format(it)})
        }*/

        // convert each row to a Map so we get an array of objects as json output instead of an array of arrays
        List<Map<String,?>> mapList = []
        t.each {
            mapList << it.toMap()
        }
        def jsonOutput = new JsonGenerator.Options()
            .dateFormat(dateFormat)
            .addConverter(new JsonGenerator.Converter() {
                /**
                 * Indicate which type this converter can handle.
                 */
                @Override
                boolean handles(Class<?> type) {
                    return TemporalAccessor.isAssignableFrom(type)
                }
                @Override
                Object convert(Object date, String key) {
                    dtf.format(date as TemporalAccessor)
                }

            })
            .build()
        String json = jsonOutput.toJson(mapList)
        return indent ? JsonOutput.prettyPrint(json) : json
    }
}
