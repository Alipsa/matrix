package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
class Row implements List<Object> {
    private int rowNumber
    private List<?> content
    private Matrix parent

    /** this method must be package scoped as only a Matrix should be able to use it */
    @PackageScope
    Row(int rowNumber, Matrix parent) {
        this.rowNumber = rowNumber
        this.content = []
        this.parent = parent
    }

    Row(int rowNumber, List<?> rowContent, Matrix parent) {
        this.rowNumber = rowNumber
        this.content = rowContent
        this.parent = parent
    }

    /**
     * Enable use of ginq and other libraries that wants to access the row like a bean
     *
     * @param propertyName the column name
     * @return the corresponding value
     */
    @Override
    Object getProperty(String propertyName) {
        getAt(propertyName)
    }

    /**
     * Add support for other libraries to treat the row as a bean
     *
     * @param propertyName the column name
     * @param newValue the new value to set
     */
    @Override
    void setProperty(String propertyName, Object newValue) {
        putAt(propertyName, newValue)
    }

    @Override
    int size() {
        return content.size()
    }

    @Override
    boolean isEmpty() {
        return content.isEmpty()
    }

    @Override
    boolean contains(Object o) {
        return content.contains(o)
    }

    @Override
    Iterator iterator() {
        // TODO: how do we handle modifications?
        return content.iterator()
    }

    @Override
    Object[] toArray() {
        return content.toArray()
    }

    @Override
    Object[] toArray(Object[] a) {
        return content.toArray(a)
    }

    Map<String, ?> toMap() {
        Map<String, ?> map = [:]
        this.eachWithIndex { Object entry, int i ->
            map[columnName(i)] = entry
        }
        map
    }

    @Override
    boolean add(Object o) {
        throw new UnsupportedOperationException()
    }

    @Override
    boolean remove(Object o) {
        throw new UnsupportedOperationException()
    }

    @Override
    boolean containsAll(Collection c) {
        return content.containsAll(c)
    }

    @Override
    boolean addAll(Collection c) {
        throw new UnsupportedOperationException()
    }

    @Override
    boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException()
    }

    @Override
    boolean removeAll(Collection c) {
        throw new UnsupportedOperationException()
    }

    @Override
    boolean retainAll(Collection c) {
        throw new UnsupportedOperationException()
    }

    @Override
    void clear() {
        throw new UnsupportedOperationException()
    }

    @Override
    Object get(int index) {
        return content.get(index)
    }

    @Override
    Object set(int index, Object element) {
        def result = content.set(index, element)
        parent.putAt([rowNumber, index] as List<Number>, element)
        return result
    }

    @Override
    void add(int index, Object element) {
        throw new UnsupportedOperationException()
    }

    @Override
    Object remove(int index) {
        throw new UnsupportedOperationException()
    }

    @Override
    int indexOf(Object o) {
        return content.indexOf(o)
    }

    @Override
    int lastIndexOf(Object o) {
        return content.lastIndexOf(o)
    }

    @Override
    ListIterator listIterator() {
        return content.listIterator()
    }

    @Override
    ListIterator listIterator(int index) {
        return content.listIterator(index)
    }

    /**
     * NOTE this method returns a disconnected list, no longer representing a row of the
     * backing matrix although changes to values that can be mutated (e.g. java.util.Date) will still
     * change the Matrix content (Numbers and Strings, jav.util.time classes are all immutable).
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a list with the columns values specified in the range
     */
    @Override
    List subList(int fromIndex, int toIndex) {
        content.subList(fromIndex, toIndex)
    }

    List subList(IntRange range) {
        content[range]
    }

    List subList(Collection indices) {
        def vals = []
        indices.each {
            vals << get(it as int)
        }
        vals
    }

    List subList(String... colNames) {
        def vals = []
        colNames.each {
            vals << getAt(it)
        }
        vals
    }

    Object putAt(int index, Object value) {
        return set(index, value)
    }

    Object putAt(Number index, Object value) {
        return set(index.intValue(), value)
    }

    Object putAt(String columnName, Object value) {
        return set(parent.columnIndex(columnName), value)
    }

    <T> T getAt(int index) {
        Class<T> type = parent.type(index) as Class<T>
        return get(index).asType(type)
    }

    <T> T getAt(Number index) {
        Class<T> type = parent.type(index.intValue()) as Class<T>
        return get(index.intValue()).asType(type)
    }

    /**
     * This override is registered in RowExtension allowing for the short notation to work
     * Example: <code>row[2, BigDecimal]</code>
     *
     * @param index the index position of the variable (column) to get
     * @param type the class to convert the result to (using the ValueConverter)
     * @return the value converted to the type specified
     */
    <T> T getAt(Number index, Class<T> type) {
        return ValueConverter.convert(get(index.intValue()),type)
    }

    <T> T getAt(String columnName) {
        int idx = parent.columnIndex(columnName)
        if (idx == -1) {
            throw new IllegalArgumentException("Failed to find a column with the name " + columnName)
        }
        Class<T> type = parent.type(idx) as Class<T>
        return get(idx).asType(type)
    }

    /**
     * This override is registered in RowExtension allowing for the short notation to work
     * Example: <code>row['foo', BigDecimal]</code>
     *
     * @param columnName the name of the variable (column) to get
     * @param type the class to convert the result to (using the ValueConverter)
     * @return the value converted to the type specified
     */
    <T> T getAt(String columnName, Class<T> type, T valueIfNull = null) {
        int idx = parent.columnIndex(columnName)
        if (idx == -1) {
            throw new IllegalArgumentException("Failed to find a column with the name " + columnName)
        }
        ValueConverter.convert(get(idx), type, null, null, valueIfNull)
    }



    int getRowNumber() {
        return rowNumber
    }

    List<String> columnNames() {
        return parent.columnNames()
    }

    String columnName(int index) {
        return parent.columnName(index)
    }

    List<Class> types() {
        return parent.types()
    }

    @Override
    String toString() {
        return String.valueOf(content)
    }

    /**
     * specific package scope mutating method that does not change the backing parent
     * used to construct a row and adding the data afterwards.
     *
     * @param e the element to add
     */
    @PackageScope
    void addElement(Object e) {
        content.add(e)
    }

    @PackageScope
    void setElement(int index, Object e) {
        content.set(index, e)
    }
}
