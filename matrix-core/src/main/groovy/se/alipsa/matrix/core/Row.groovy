package se.alipsa.matrix.core

import groovy.transform.PackageScope

import org.codehaus.groovy.runtime.DefaultGroovyMethods

import se.alipsa.matrix.core.util.ValueComparison



/**
 * A live row view backed by a parent {@link Matrix}.
 *
 * <p>Reads and in-place updates are reflected in the owning matrix, while
 * structural list operations such as adding or removing elements are rejected.</p>
 */
class Row implements GroovyObject, List<Object> {

    private static final String UNSUPPORTED_MUTATION_MESSAGE = 'Adding and deleting values from a row is not supported.'
    private static final String UNSUPPORTED_SORT_MESSAGE = 'Sorting a row is not supported because it would move values across columns and can violate their declared types.'
    private static final String UNKNOWN_COLUMN_MESSAGE_PREFIX = 'Failed to find a column with the name '

    @PackageScope
    static final int COLUMN_NOT_FOUND = -1

    private final int rowNumber
    private final List<Object> content
    private Matrix parent
    private final List<String> columnNames
    private final List<Class> types

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
            this[propertyName]
        } else {
          GroovyObject.super.getProperty(propertyName)
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
            this[propertyName] = newValue
        } else {
          GroovyObject.super.setProperty(propertyName, newValue)
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
    Iterator<Object> iterator() {
        return Collections.unmodifiableList(content).iterator()
    }

    @Override
    Object[] toArray() {
        return content.toArray()
    }

    @Override
    @SuppressWarnings('unchecked')
    <T> T[] toArray(T[] a) {
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
        throw new UnsupportedOperationException(UNSUPPORTED_MUTATION_MESSAGE)
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
        throw new UnsupportedOperationException(UNSUPPORTED_MUTATION_MESSAGE)
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
        throw new UnsupportedOperationException(UNSUPPORTED_MUTATION_MESSAGE)
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
        throw new UnsupportedOperationException(UNSUPPORTED_MUTATION_MESSAGE)
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
        throw new UnsupportedOperationException(UNSUPPORTED_MUTATION_MESSAGE)
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
        throw new UnsupportedOperationException(UNSUPPORTED_MUTATION_MESSAGE)
    }

    /**
     * Adding and deleting values from a row is not supported. This method always throws an
     * UnsupportedOperationException
     */
    @Override
    void clear() {
        throw new UnsupportedOperationException(UNSUPPORTED_MUTATION_MESSAGE)
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
        if (parent != null) {
            parent[[rowNumber, index]] = element
        }
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
        throw new UnsupportedOperationException(UNSUPPORTED_MUTATION_MESSAGE)
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
        throw new UnsupportedOperationException(UNSUPPORTED_MUTATION_MESSAGE)
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
    ListIterator<Object> listIterator() {
        return new CheckedRowListIterator(this, 0)
    }

    @Override
    ListIterator<Object> listIterator(int index) {
        return new CheckedRowListIterator(this, index)
    }

    /**
     * Returns a live view of the specified range of this row.
     * Value changes made through {@link List#set(int, Object)} are reflected in the
     * backing matrix; structural operations such as add or remove are rejected.
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a live view of the columns values specified in the range
     */
    @Override
    List<Object> subList(int fromIndex, int toIndex) {
        new CheckedRowSubList(this, content.subList(fromIndex, toIndex), fromIndex)
    }

    /**
     * Returns a disconnected copy selected by the range. Changes to the returned
     * list do not update the backing matrix; mutable objects contained in the list
     * are still shared.
     *
     * @param range (inclusive) of all the indexes to include
     * @return a new list with the values for the indices
     */
    List<Object> subList(IntRange range) {
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
    List<Object> subList(Collection indices) {
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
    List<Object> subList(String... colNames) {
        def vals = []
        colNames.each {
            vals << this[it]
        }
        vals
    }

    /**
     * Short notation to set a value e.g.
     * <code> row[1] = 'foo'</code>
     *
     * @param index the column index to set
     * @param value the new value
     * @return the previous value at the column index
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
     * @return the previous value at the column index
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
     * @return the previous value in the named column
     */
    Object putAt(String columnName, Object value) {
        int idx = columnNames.indexOf(columnName)
        if (idx == COLUMN_NOT_FOUND) {
            throw new IllegalArgumentException(UNKNOWN_COLUMN_MESSAGE_PREFIX + columnName)
        }
        return set(idx, value)
    }

    /**
     * Short notation to get a value e.g. <code>def val = row[1]</code>
     *
     * @param index the column index
     * @return the value as the type specified in the types() assignment
     */
    <T> T getAt(int index) {
        Class<T> type = types[index] as Class<T>
        def value = get(index)
        return value == null ? null : value.asType(type)
    }

    /**
     * Short notation to get a value e.g. <code>def val = row[foo]</code>
     *
     * @param index the column index
     * @return the value as the type specified in the types() assignment
     */
    <T> T getAt(Number index) {
        Class<T> type = types[index.intValue()] as Class<T>
        def value = get(index.intValue())
        return value == null ? null : value.asType(type)
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
        return ValueConverter.convert(get(index.intValue()), type)
    }

    /**
     * Short notation to get a value e.g. <code>def val = row['foo']</code>
     *
     * @param columnName the column name to get the value from
     * @return the value as the type specified in the types() assignment
     */
    <T> T getAt(String columnName) {
        int idx = columnNames.indexOf(columnName)
        if (idx == COLUMN_NOT_FOUND) {
            throw new IllegalArgumentException(UNKNOWN_COLUMN_MESSAGE_PREFIX + columnName)
        }
        Class<T> type = types[idx] as Class<T>
        def value = get(idx)
        return value == null ? null : value.asType(type)
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
        if (idx == COLUMN_NOT_FOUND) {
            throw new IllegalArgumentException(UNKNOWN_COLUMN_MESSAGE_PREFIX + columnName)
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
     * Two rows are equal when compared against another {@link List} of the same
     * size with equal corresponding elements, using Groovy value semantics. Numeric
     * values are compared by exact mathematical value regardless of runtime type or
     * {@link BigDecimal} scale.
     *
     * @param o the object to compare against
     * @return true if {@code o} is a List with equal elements in the same order
     */
    @Override
    boolean equals(Object o) {
        if (this.is(o)) {
            return true
        }
        if (!(o instanceof List)) {
            return false
        }
        List<?> other = o as List<?>
        if (content.size() != other.size()) {
            return false
        }
        for (int i = 0; i < content.size(); i++) {
            if (ValueComparison.valuesAreDifferent(content[i], other[i], BigDecimal.ZERO)) {
                return false
            }
        }
        true
    }

    /**
     * Hash code consistent with {@link #equals(Object)}. Numerically equal values
     * have the same hash code regardless of runtime type or {@link BigDecimal} scale.
     * A Row is a mutable view, so changing its values after using it in a hash-based
     * collection changes this hash code.
     *
     * @return the hash code derived from this row's current element values
     */
    @Override
    int hashCode() {
        int result = 1
        for (Object value : content) {
            result = 31 * result + ValueComparison.normalizedValueHash(value)
        }
        result
    }

    List<Object> minusColumn(String columnName) {
        int idx = columnNames.indexOf(columnName)
        if (idx == COLUMN_NOT_FOUND) {
            throw new IllegalArgumentException(UNKNOWN_COLUMN_MESSAGE_PREFIX + columnName)
        }
        minusColumn(idx)
    }

    List<Object> minusColumn(int index) {
        def result = new ArrayList(this)
        result.remove(index)
        result
    }

    /**
     * Sorting a row is not supported because it would move values across columns
     * and can violate their declared types.
     */
    @Override
    void sort(Comparator<? super Object> c) {
        throw new UnsupportedOperationException(UNSUPPORTED_SORT_MESSAGE)
    }

    /**
     * Disconnect the row from the underlying Matrix so that any data manipulation will not affect
     * the parent matrix.
     *
     * @return this detached
     */
    Row detach() {
        parent = null
        this
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

    @SuppressWarnings('UnnecessaryCollectCall')
    def asType(Class type) {
        if (type == Row) {
            return this
        }
        if (type == Map) {
            return toMap()
        }
        if (type == List) {
            return content
        }
        if (type == Set) {
            return content as Set
        }
        if (type == String) {
            return content.collect { it.toString() }.join(', ')
        }
        DefaultGroovyMethods.asType(this, type as Class<Object>)
    }

  Row move(int fromIndex, int toIndex) {
    if (fromIndex < 0 || fromIndex >= size() || toIndex < 0 || toIndex >= size()) {
      throw new IndexOutOfBoundsException('Invalid indices for move operation')
    }

    List values = content.collect { it }
    // 1. Remove the element from the source index
    def elementToMove = values.remove(fromIndex)

    // 2. Add the element at the destination index
    values.add(toIndex, elementToMove)
    values.eachWithIndex { Object entry, int i ->
      set(i, entry)
    }
    this
  }

  /**
   * A list iterator over a row whose {@code set} writes through to the parent matrix.
   * Structural mutation ({@code add} and {@code remove}) is rejected.
   */
  private static class CheckedRowListIterator implements ListIterator<Object> {

    private final Row row
    private int cursor
    private int lastReturned = -1

    CheckedRowListIterator(Row row, int index) {
      this.row = row
      this.cursor = index
    }

    @Override
    boolean hasNext() {
      cursor < row.size()
    }

    @Override
    Object next() {
      if (!hasNext()) {
        throw new NoSuchElementException()
      }
      lastReturned = cursor
      return row.get(cursor++)
    }

    @Override
    boolean hasPrevious() {
      cursor > 0
    }

    @Override
    Object previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException()
      }
      lastReturned = --cursor
      return row.get(cursor)
    }

    @Override
    int nextIndex() {
      cursor
    }

    @Override
    int previousIndex() {
      cursor - 1
    }

    @Override
    void remove() {
      throw new UnsupportedOperationException(Row.UNSUPPORTED_MUTATION_MESSAGE)
    }

    @Override
    void set(Object e) {
      if (lastReturned < 0) {
        throw new IllegalStateException('set() can only be called after next() or previous()')
      }
      row.set(lastReturned, e)
    }

    @Override
    void add(Object e) {
      throw new UnsupportedOperationException(Row.UNSUPPORTED_MUTATION_MESSAGE)
    }
  }

  /**
   * A live sublist view of a row. Value writes are reflected in the parent matrix;
   * structural operations are rejected.
   */
  private static class CheckedRowSubList extends AbstractList<Object> {

    private final Row row
    private final List<Object> delegate
    private final int offset

    CheckedRowSubList(Row row, List<Object> delegate, int offset) {
      this.row = row
      this.delegate = delegate
      this.offset = offset
    }

    @Override
    Object get(int index) {
      delegate.get(index)
    }

    @Override
    int size() {
      delegate.size()
    }

    @Override
    Object set(int index, Object element) {
      row.set(offset + index, element)
    }

    @Override
    void sort(Comparator<? super Object> comparator) {
      throw new UnsupportedOperationException(Row.UNSUPPORTED_SORT_MESSAGE)
    }

    @Override
    void add(int index, Object element) {
      throw new UnsupportedOperationException(Row.UNSUPPORTED_MUTATION_MESSAGE)
    }

    @Override
    Object remove(int index) {
      throw new UnsupportedOperationException(Row.UNSUPPORTED_MUTATION_MESSAGE)
    }
  }

}
