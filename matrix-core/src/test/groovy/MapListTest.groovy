import static se.alipsa.matrix.core.ValueConverter.*

import java.time.LocalDate
import java.time.YearMonth

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.MapList

class MapListTest {

  @Test
  void testAddAndGet() {
    MapList<String, BigDecimal> lm = new MapList<>()

    lm.add("one", 34.4)
    lm.add("two", 23.1)
    lm.add("two", 1.4)
    lm.add("three", 34 as BigDecimal)
    lm.add("three", 0.009)
    lm.add("three", 3.14)

    assertEquals(23.1, lm["two", 0])
    assertEquals(3.14, lm["three", 2])
    assertEquals(34.4, lm["one", 0])
  }

  @Test
  void testAddAndGetDates() {
    MapList<YearMonth, LocalDate> lm = new MapList<>()

    lm.add(YearMonth.of(2014, 1), asLocalDate("2014-01-11"))
    lm.add(YearMonth.of(2014, 2), asLocalDate("2014-02-01"))
    lm.add(YearMonth.of(2014, 2), asLocalDate("2014-02-22"))
    lm.add(YearMonth.of(2014, 3), asLocalDate("2014-03-20"))
    lm.add(YearMonth.of(2014, 3), asLocalDate("2014-03-20"))
    lm.add(YearMonth.of(2014, 3), asLocalDate("2014-03-28"))

    //println lm.get(YearMonth.of(2014, 2))
    assertEquals(asLocalDate("2014-02-01"), lm.getAt([YearMonth.of(2014, 2), 0]), lm.toString())
    assertEquals(asLocalDate("2014-02-01"), lm[YearMonth.of(2014, 2), 0], lm.toString())
    assertEquals(asLocalDate("2014-03-28"), lm[YearMonth.of(2014, 3), 2])
    assertEquals(asLocalDate("2014-01-11"), lm[YearMonth.of(2014, 1), 0])
  }

  @Test
  void testPutAt() {
    MapList<Integer, String> ml = new MapList<>()
    ml[1] = 'Foo'
    ml[1] = 'Bar'
    ml[2] = 'Baz'
    assertIterableEquals(['Baz'], ml.get(2))
    assertIterableEquals(['Foo', 'Bar'], ml.get(1))
  }

  @Test
  void testRemove() {
    MapList<Integer, String> ml = new MapList<>()
    ml[1] = 'Foo'
    ml[1] = 'Bar'
    ml.remove(1, 0)
    assertIterableEquals(['Bar'], ml.get(1))
  }
}
