package se.alipsa.matrix.json

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import groovy.json.*

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

class JsonExporter {

    Matrix table

    JsonExporter(Grid grid, List<String> columnNames) {
        if (grid == null || columnNames == null) {
            throw new IllegalArgumentException("Grid and columnNames cannot be null")
        }
        this.table = Matrix.builder()
        .columnNames(columnNames)
        .data(grid)
        .build()
    }

    JsonExporter(Matrix table) {
        if (table == null) {
            throw new IllegalArgumentException("Matrix table cannot be null")
        }
        this.table = table
    }

    String toJson(boolean indent = false) {
        this.toJson([:], indent)
    }

    String toJson(Map<String, Closure> columnFormatters) {
        this.toJson(columnFormatters, false)
    }

    String toJson(String dateFormat) {
        this.toJson([:], false, dateFormat)
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
        // Only clone if we need to apply formatters (avoids unnecessary memory allocation)
        def t = columnFormatters.isEmpty() ? table : table.clone()
        if (!columnFormatters.isEmpty()) {
            columnFormatters.each { k,v ->
                t.apply(k, v)
            }
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat)

        def jsonGenerator = new JsonGenerator.Options()
            .dateFormat(dateFormat)
            .addConverter(new JsonGenerator.Converter() {
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

        // Build JSON by collecting row maps and letting the generator handle serialization
        def rowMaps = []
        for (def row : t) {
            rowMaps.add(row.toMap())
        }
        String json = jsonGenerator.toJson(rowMaps)
        return indent ? JsonOutput.prettyPrint(json) : json
    }

    /**
     * Export a Matrix to JSON string (static convenience method).
     *
     * <p>This provides a one-liner way to export a Matrix without creating an instance.</p>
     *
     * @param table the Matrix to export
     * @param indent whether to pretty print the JSON
     * @return JSON string representation
     */
    static String toJson(Matrix table, boolean indent = false) {
        new JsonExporter(table).toJson(indent)
    }

    /**
     * Export a Matrix to a JSON file.
     *
     * @param table the Matrix to export
     * @param outputFile file to write JSON to
     * @param indent whether to pretty print the JSON
     * @throws IOException if writing fails
     */
    static void toJsonFile(Matrix table, File outputFile, boolean indent = false) throws IOException {
        outputFile.text = new JsonExporter(table).toJson(indent)
    }
}
