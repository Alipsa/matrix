package se.alipsa.groovy.matrix

class RowIterator implements Iterator<Row> {
    Matrix parent
    int rowNumber
    int nRows
    List<?> rows

    RowIterator(Matrix parent) {
        this.parent = parent
        rowNumber = 0
        rows = parent.rows()
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
