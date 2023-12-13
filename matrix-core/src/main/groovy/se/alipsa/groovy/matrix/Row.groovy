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

    @Override
    List subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException()
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

    Object getAt(int index) {
        return get(index)
    }

    Object getAt(Number index) {
        return get(index.intValue())
    }

    Object getAt(String columnName) {
        return get(parent.columnIndex(columnName))
    }

    int getRowNumber() {
        return rowNumber
    }

    List<String> columnNames() {
        return parent.columnNames()
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

    String getString(int index) {
        return String.valueOf(get(index))
    }

    Integer getInt(int index) {
        return get(index) as Integer
    }

    Object getObject(int index){
        return get(index)
    }
}
