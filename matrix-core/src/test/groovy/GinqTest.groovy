
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix

import java.time.LocalDate

import static se.alipsa.matrix.core.ListConverter.*

import static org.junit.jupiter.api.Assertions.*

class GinqTest {

  @Test
  void testSimple() {
    Matrix m = Matrix.builder().data(
        name: ['Orange', 'Apple', 'Banana', 'Mango', 'Durian'],
        price: [11,6,4,29,32],
        stock: [2,3,1,10,9])
    .types(String, int, int)
    .build()

    def expected = [['Mango', 29, 10], ['Orange', 11, 2], ['Apple', 6, 3], ['Banana', 4, 1]]

    def result = GQ {
      from f in m
      where f.price < 32
      orderby f.price in desc
      select f.name, f.price, f.stock
    }

    assertIterableEquals(expected, result.toList())
    // Construct a matrix directly from the query result
    Matrix m2 = Matrix.builder()
        .rows(result.toList())
        .columnNames(m)
        .types(m)
        .build()
    assertIterableEquals(m.subset{it.price < 32}.orderBy('price', true), m2)
  }

  @Test
  void testJoin() {
    Matrix warehouse = Matrix.builder().data(
        id: [1,2,3,4],
        name: ['Orange', 'Apple', 'Banana', 'Mango'],
        price: [11,6,4,29],
        stock: [2,3,1,10])
        .types(int, String, Double, int)
        .build()

    Matrix sales = Matrix.builder().data(
      date: toLocalDates('2024-05-01', '2024-05-02', '2024-05-03'),
      item: [1, 1, 3]
    ).types(LocalDate, int)
    .build()

    def q2 = GQ {
      from n in (
        from s in sales.rows()
        join w in warehouse.rows() on w.id == s.item
        select w.name, w.price
      )
      groupby n.name
      orderby n.name
      select n.name, sum(n.price)
    }
    def m = Matrix.builder()
        .rows(q2.toList())
        .columnNames('name', 'amount')
        .build()
    assertEquals(4.0, m[0, 'amount'])
    assertEquals(22.0, m[1, 'amount'])
  }

  class Warehouse {
    int id
    String name
    Double price
    int stock

    Warehouse(int id, String name, Double price, int stock) {
      this.id = id
      this.name = name
      this.price = price
      this.stock = stock
    }
  }

  class Sales {
    LocalDate date
    int item

    Sales(LocalDate date, int item) {
      this.date = date
      this.item = item
    }
  }

  @Test
  void testJoinFromPojos() {
     Matrix warehouse = Matrix.builder().data([
        new Warehouse(1, 'Orange', 11, 2),
        new Warehouse(2, 'Apple', 6, 3),
        new Warehouse(3, 'Banana', 4, 1),
        new Warehouse(4, 'Mango', 29, 10)
    ]).build()
    Matrix sales = Matrix.builder().data([
        new Sales(LocalDate.of(2024, 5, 1), 1),
        new Sales(LocalDate.of(2024, 5, 2), 1),
        new Sales(LocalDate.of(2024, 5, 3), 3)
    ]).build()

    // a matrix is iterable so can be used directly (no need to call matrix.rows())
    // even though intellij is not happy about it
    def q = GQ {
      from s in sales
      join w in warehouse on w.id == s.item
      select w.name, w.price
    }
    assert  [['Orange', 11.0], ['Orange', 11.0], ['Banana', 4.0]] == q.toList()

    def q2 = GQ {
      from w in q
      groupby w.name
      orderby w.name in desc
      select w.name, sum(w.price)
    }
    assertIterableEquals([['Orange', 22.0], ['Banana', 4.0]], q2.toList())

    /*
    // however, doing it all in one go does not work:
    def qSum = GQ {
      from s in sales
      join w in warehouse on w.id == s.item
      groupby w.name
      orderby w.name in desc
      select w.name, sum(w.price)
    }
    // Fails with No such property: price for class: GinqTest$Sales
     */
    // subselect however, works:
    def qSum = GQ {
      from n in (
        from s in sales
        join w in warehouse on w.id == s.item
        select w.name, w.price)
      groupby n.name
      orderby n.name in desc
      select n.name, sum(n.price) as price
    }
    assertIterableEquals([['Orange', 22.0], ['Banana', 4.0]], qSum.toList())
  }
}
