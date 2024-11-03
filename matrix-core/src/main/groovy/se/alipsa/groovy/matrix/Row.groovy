package se.alipsa.groovy.matrix

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
class Row implements List<Object> {
    private int rowNumber
    private List<?> content
    private Matrix parent
    private List<String> columnNames
    private List<Class> types

    /** this method must be package scoped as only a Matrix should be able to use it */
    @PackageScope
    Row(int rowNumber, Matrix parent) {
        this.rowNumber = rowNumber
        this.content = []
        columnNames = parent.columnNames().collect()
        types = parent.types().collect()
        this.parent = parent
    }

    Row(int rowNumber, List<?> rowContent, Matrix parent) {
        this.rowNumber = rowNumber
        this.content = rowContent
        columnNames = parent.columnNames().collect()
        types = parent.types().collect()
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
        if (propertyName in columnNames) {
            getAt(propertyName)
        } else {
            super.getProperty(propertyName)
        }
    }

    /**
     * Add support for other libraries to treat the row as a bean
     *
     * @param propertyName the column name
     * @param newValue the new value to set
     */
    @Override
    void setProperty(String propertyName, Object newValue) {
        if (propertyName in columnNames) {
            putAt(propertyName, newValue)
        } else {
            super.setProperty(propertyName, newValue)
        }
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

    /**
     * Convert this Row into a Map&lt;String, ?&gt; where each key corresponds to the column name
     * and each value corresponds to the row value.
     *
     * @return a Map&lt;String, ?&gt; with the column names and data
     */
    Map<String, ?> toMap() {
        Map<String, ?> map = [:]
        this.eachWithIndex { Object entry, int i ->
            map[columnNames[i]] = entry
        }
        map
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException
     *
     * @param o element whose presence in this collection is to be added
     * @return throws UnsupportedOperationException
     */
    @Override
    boolean add(Object o) {
        throw new UnsupportedOperationException('Adding and deleting values from a row is not supported.')
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException
     *
     * @param o element whose presence in this collection is to be removed
     * @return throws UnsupportedOperationException
     */
    @Override
    boolean remove(Object o) {
        throw new UnsupportedOperationException('Adding and deleting values from a row is not supported.')
    }

    @Override
    boolean containsAll(Collection c) {
        return content.containsAll(c)
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException
     *
     * @param c the collection to add
     * @return throws UnsupportedOperationException
     */
    @Override
    boolean addAll(Collection c) {
        throw new UnsupportedOperationException('Adding and deleting values from a row is not supported.')
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException
     *
     * @param index index at which to insert the first element from the
     *              specified collection
     * @param c collection containing elements to be added to this list
     * @return throws UnsupportedOperationException
     */
    @Override
    boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException('Adding and deleting values from a row is not supported.')
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException
     *
     * @param c collection containing elements to be removed from this list
     * @return throws UnsupportedOperationException
     */
    @Override
    boolean removeAll(Collection c) {
        throw new UnsupportedOperationException('Adding and deleting values from a row is not supported.')
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException
     *
     * @param c collection containing elements to be retained in this list
     * @return throws UnsupportedOperationException
     */
    @Override
    boolean retainAll(Collection c) {
        throw new UnsupportedOperationException('Adding and deleting values from a row is not supported.')
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException
     */
    @Override
    void clear() {
        throw new UnsupportedOperationException('Adding and deleting values from a row is not supported.')
    }

    @Override
    Object get(int index) {
        return content.get(index)
    }

    /**
     * Change a value at the specified index
     *
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     */
    @Override
    Object set(int index, Object element) {
        def result = content.set(index, element)
        parent.putAt([rowNumber, index] as List<Number>, element)
        return result
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException.
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     */
    @Override
    void add(int index, Object element) {
        throw new UnsupportedOperationException('Adding and deleting values from a row is not supported.')
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException.
     *
     * @param index the index of the element to be removed
     * @return throws UnsupportedOperationException
     */
    @Override
    Object remove(int index) {
        throw new UnsupportedOperationException('Adding and deleting values from a row is not supported.')
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
     * change the Matrix content (Numbers and Strings, java.util.time classes are all immutable).
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a list with the columns values specified in the range
     */
    @Override
    List subList(int fromIndex, int toIndex) {
        content.subList(fromIndex, toIndex)
    }

    /**
     * NOTE this method returns a disconnected list, no longer representing a row of the
     * backing matrix although changes to values that can be mutated (e.g. java.util.Date) will still
     * change the Matrix content (Numbers and Strings, java.util.time classes are all immutable).
     *
     * @param range (inclusive) of all the indexes to include
     * @return a new list with the values for the indices
     */
    List subList(IntRange range) {
        content[range]
    }

    /**
     * NOTE this method returns a disconnected list, no longer representing a row of the
     * backing matrix although changes to values that can be mutated (e.g. java.util.Date) will still
     * change the Matrix content (Numbers and Strings, java.util.time classes are all immutable).
     *
     * @param indices a collection (inclusive) of all the indexes to include
     * @return a new list with the values for the indices
     */
    List subList(Collection indices) {
        def vals = []
        indices.each {
            vals << get(it as int)
        }
        vals
    }

    /**
     * NOTE this method returns a disconnected list, no longer representing a row of the
     * backing matrix although changes to values that can be mutated (e.g. java.util.Date) will still
     * change the Matrix content (Numbers and Strings, java.util.time classes are all immutable).
     *
     * @param colNames an array of column names to include
     * @return a new list with the values for the colNames
     */
    List subList(String... colNames) {
        def vals = []
        colNames.each {
            vals << getAt(it)
        }
        vals
    }

    /**
     * Short notation to set a value e.g.
     * <code> row[1] = 'foo'</code>
     *
     * @param index the column index to set
     * @param value the new value
     * @return
     */
    Object putAt(int index, Object value) {
        return set(index, value)
    }

    /**
     * Short notation to set a value e.g.
     * <code> row[nameIndex] = 'foo'</code>
     *
     * @param index the column index to set
     * @param value the new value
     * @return
     */
    Object putAt(Number index, Object value) {
        return set(index.intValue(), value)
    }

    /**
     * Short notation to set a value e.g.
     * <code> row['name'] = 'foo'</code>
     *
     * @param columnName the column columnName to set
     * @param value the new value
     * @return
     */
    Object putAt(String columnName, Object value) {
        return set(columnNames.indexOf(columnName), value)
    }

    /**
     * Short notation to get a value e.g. <code>def val = row[1]</code>
     *
     * @param index
     * @return the value as the type specified in the types() assignment
     */
    <T> T getAt(int index) {
        Class<T> type = types[index] as Class<T>
        return get(index).asType(type)
    }

    /**
     * Short notation to get a value e.g. <code>def val = row[foo]</code>
     *
     * @param index
     * @return the value as the type specified in the types() assignment
     */
    <T> T getAt(Number index) {
        Class<T> type = types[index.intValue()] as Class<T>
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

    /**
     * Short notation to get a value e.g. <code>def val = row['foo']</code>
     *
     * @param columnName the column name to get the value from
     * @return the value as the type specified in the types() assignment
     */
    <T> T getAt(String columnName) {
        int idx = columnNames.indexOf(columnName)
        if (idx == -1) {
            throw new IllegalArgumentException("Failed to find a column with the name " + columnName)
        }
        Class<T> type = types[idx] as Class<T>
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
        int idx = columnNames.indexOf(columnName)
        if (idx == -1) {
            throw new IllegalArgumentException("Failed to find a column with the name " + columnName)
        }
        ValueConverter.convert(get(idx), type, null, null, valueIfNull)
    }

    /**
     * @return the row number where this row appears in the Matrix
     */
    int getRowNumber() {
        return rowNumber
    }

    List<String> columnNames() {
        return columnNames
    }

    String columnName(int index) {
        return columnNames[index]
    }

    List<Class> types() {
        return types
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
