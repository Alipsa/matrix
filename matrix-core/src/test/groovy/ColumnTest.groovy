import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Stat

class ColumnTest {

  @Test
  void testPlusList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 + [1, 2, 3, 4] == [2, 4, 6, 8]
    // smaller
    assert c1 + [1, 2] == [2, 4, null, null]
    // larger
    assert c1 + [1, 2, 3, 4, 5, 6] == [2, 4, 6, 8]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 + [1, 2, 3, 4] == [2, null, 6, 8]
    // smaller
    assert c2 + [1, 2] == [2, null, null, null]
    // larger
    assert c2 + [1, 2, 3, 4, 5, 6] == [2, null, 6, 8]
  }

  @Test
  void testPlusObject() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1 + 1 == [2, 3, 4, 5]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 + 2 == [3, null, 5, 6]

    Column s1 = new Column(['1', '2', '3', '4'])
    assert s1 + 'px' == ['1px', '2px', '3px', '4px']

    Column s2 = new Column(['1', '2', null, '4'])
    assert s2 + 'px' == ['1px', '2px', null, '4px']
  }

  @Test
  void testMinusList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 - [1, 2, 3, 4] == [0, 0, 0, 0]
    // smaller
    assert c1 - [0, 1] == [1, 1, null, null]
    // larger
    assert c1 - [-1, 2, 2.1, 4.2, 5, 6] == [2, 0, 0.9, -0.2]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 - [1, 0, -1, -1.1] == [0, null, 4, 5.1]
    // smaller
    assert c2 - [2, 1] == [-1, null, null, null]
    // larger
    assert c2 - [1, 2, 3, 4, 5, 6] == [0, null, 0, 0]
  }

  @Test
  void testMinusObject() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1 - 1 == [0, 1, 2, 3]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 - 2.5 == [-1.5, null, 0.5, 1.5]

    Column s1 = new Column(['1', '2', '3', '4'])
    assert s1 - '2' == ['1', '', '3', '4']

    Column s2 = new Column(['1', '2', null, '4'])
    assert s2 - '1' == ['', '2', null, '4']
  }

  @Test
  void testMultiplyList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 * [1, 2, 3, 4] == [1, 4, 9, 16]
    // smaller
    assert c1 * [1, 2] == [1, 4, null, null]
    // larger
    assert c1 * [1, 2, 3, 4, 5, 6] == [1, 4, 9, 16]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 * [1, 2, 3, 4] == [1, null, 9, 16]
    // smaller
    assert c2 * [1, 2] == [1, null, null, null]
    // larger
    assert c2 * [1, 2, 3, 4, 5, 6] == [1, null, 9, 16]
  }

  @Test
  void testMultiplyNumber() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1.multiply(2) == [2, 4, 6, 8]
    assert c1 * 2 == [2, 4, 6, 8]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 * 2 == [2, null, 6, 8]
  }

  @Test
  void testDivList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 / [1, 1, 2, 0.5] == [1, 2, 1.5, 8]
    // smaller
    assert c1 / [2, 1] == [0.5, 2, null, null]
    // larger
    assert c1 / [1, 2, 0.5, 10, 11, 12] == [1, 1, 6, 0.4]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 / [2, 2, 1, 2] == [0.5, null, 3, 2]
    // smaller
    assert c2 / [1, 2] == [1, null, null, null]
    // larger
    assert c2 / [0.5, 2, 2, 2, 1, 60] == [2, null, 1.5, 2]
  }

  @Test
  void testDivNumber() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1.div(2) == [0.5, 1, 1.5, 2]
    assert c1 / 2 ==  [0.5, 1, 1.5, 2]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 / 2 == [0.5, null, 1.5, 2]
  }

  @Test
  void testPowerList() {
    Column c1 = new Column([1, 2, 3, 4])
    // equal size
    assert c1 ** [1, 2, 3, 4] == [1, 4, 27, 256]
    // smaller
    assert c1 ** [1, 2] == [1, 4, null, null]
    // larger
    assert c1 ** [1, 2, 3, 4, 5, 6] == [1, 4, 27, 256]

    Column c2 = new Column([1, null, 3, 4])
    // equal size
    assert c2 ** [1, 2, 3, 4] == [1, null, 27, 256]
    // smaller
    assert c2 ** [1, 2] == [1, null, null, null]
    // larger
    assert c2 ** [1, 2, 3, 4, 5, 6] == [1, null, 27, 256]
  }

  @Test
  void testPowerNumber() {
    Column c1 = new Column([1, 2, 3, 4])
    assert c1.power(2) == [1, 4, 9, 16]
    assert c1 ** 2 == [1, 4, 9, 16]

    Column c2 = new Column([1, null, 3, 4])
    assert c2 ** 2 == [1, null, 9, 16]
  }

  @Test
  void testLeftShift() {
    Column c1 = new Column([1, 2, 3, 4])
    c1 << 5
    assert [1,2,3,4,5] == c1
    c1 << [6,7]
    assert [1,2,3,4,5,6,7] == c1
  }

  @Test
  void testAsType() {
    Column c = [1,2,3] as Column
    assert [1,2,3] == c

    LinkedList<Number> l = new LinkedList<>()
    l.add(1)
    l.add(2)
    l.add(1)
    Column col = l as Column
    assert [2,4,4] == col + c
  }

  @Test
  void testSubList() {
    Column c = [1,2,3,4] as Column
    assert [1,2,3] == c.subList(0..2)
  }

  @Test
  void testUnique() {
    Column c = new Column([1, 2, 3, 4, 1, 2, 3])
    assert [1, 2, 3, 4] == c.unique() : "unique() should return a new Column with unique values"
    assert new Column([1, 2, 3, 4, 1, 2, 3]) == c : "unique() should not mutate the original Column"
  }

  @Test
  void testCollectionMaths() {
    Column c = new Column([1, 2, 3, 4, 1, 2, 3])
    assert 4 == c.max()
    assert 1 == c.min()
    assert 2 == c.median()
    assert 2.285714286 == c.mean()
    assert 1.1126972805283737 == c.sd()
    assert Stat.variance(c) == c.variance()
  }

  @Test
  void testRemoveNulls() {
    Column c = new Column([1, null, 3, null, 5])
    Column noNulls = c.removeNulls()
    assert [1,3,5] == noNulls
    assert [1, null, 3, null, 5] == c // original should be unchanged
  }
}
