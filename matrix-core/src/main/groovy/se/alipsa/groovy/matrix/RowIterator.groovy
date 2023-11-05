package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic

/**
 * This was needed in earlier versions of a Matrix when rows returned a List<List<?>>
 *   but since this is no longer case, we do not need it.
 */
@CompileStatic
class RowIterator implements Iterator<Row> {
    Matrix parent
    int rowNumber
    int nRows
    List<List<?>> rows

    RowIterator(Matrix parent) {
        this.parent = parent
        rowNumber = 0
        rows = parent.rows() as List<List<?>>
        nRows = rows.size()
    }


    @Override
    boolean hasNext() {
        return rowNumber < nRows
    }

    @Override
    Row next() {
        List rowContent = rows[rowNumber]
        return new Row(rowNumber++, rowContent, parent)
    }
}
